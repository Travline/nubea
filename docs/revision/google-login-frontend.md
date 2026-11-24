# Inicio de sesión con Google: guía para el frontend

> **Para:** quien integre "Continuar con Google" y "Vincular Google" en el rediseño de `apps/cms` y `apps/store`.
> **Backend:** Bloque 5 (RN-M1-05). Los frontends actuales todavía no usan estos endpoints.

## 1. Cómo funciona

1. El frontend muestra el botón de **Google Identity Services** (GIS), configurado con `VITE_GOOGLE_CLIENT_ID`.
2. Cuando el usuario elige su cuenta, GIS entrega un **ID token** (JWT) en `response.credential`.
3. El frontend envía ese token al backend. El backend lo verifica (firma, emisor, audiencia, vigencia y correo verificado) e inicia **la misma sesión por cookie** que el login con contraseña.

Reglas para el frontend:
- **No decodificar ni guardar** el token en el frontend: se envía tal cual y se descarta.
- **No usar** el flujo con redirección de OAuth (`ux_mode: 'redirect'`) ni pedir *access tokens*. Solo el ID token.

## 2. Configuración

| Dónde | Variable / ajuste |
| --- | --- |
| `apps/cms/.env` y `apps/store/.env` | `VITE_GOOGLE_CLIENT_ID=<id>.apps.googleusercontent.com`. Es público |
| Backend (Render) | `GOOGLE_CLIENT_ID` con **el mismo valor** |
| Google Cloud → Credenciales → cliente "Aplicación web" | **Orígenes de JavaScript autorizados:** `https://nubea.vercel.app` y los locales (`http://localhost:5173`, `http://localhost:5174`, `http://localhost:3000`). **No hace falta** ninguna URI de redirección |
| Google Cloud → Pantalla de consentimiento | En modo **prueba**, solo entran los "usuarios de prueba" añadidos ahí |

Si `VITE_GOOGLE_CLIENT_ID` está vacío, **no mostrar** el botón. Si el backend responde 503, ocultarlo o mostrar el mensaje de la tabla.

Script de GIS: `<script src="https://accounts.google.com/gsi/client" async></script>`. Para inicializarlo:

```ts
google.accounts.id.initialize({
  client_id: import.meta.env.VITE_GOOGLE_CLIENT_ID,
  callback: (respuesta) => continuarConGoogle(respuesta.credential), // el ID token
});
google.accounts.id.renderButton(elemento, { theme: 'outline', text: 'continue_with', locale: 'es' });
```

## 3. Reglas comunes de las peticiones

- Mismo cliente que el login actual: `credentials: 'include'` (o `withCredentials: true`) y **token CSRF**. Antes de cada `POST`, pedir `GET /api/auth/csrf` y enviar su `token` en la cabecera `headerName`, igual que `solicitar()` en `apps/cms/src/autenticacion/api.ts` y `apps/store/src/api.ts`. **Sin CSRF, el backend responde 403.**
- Formato de error, el mismo de toda la API: `{ status, error, mensaje, campos?, timeStap }`. Mostrar `mensaje` o, si hay `campos`, el texto del campo.

## 4. `POST /api/auth/google`: continuar con Google

**Petición**

```http
POST /api/auth/google
Content-Type: application/json
X-XSRF-TOKEN: <token CSRF>

{ "idToken": "<response.credential>", "recordarme": false }
```

`recordarme` es opcional: igual que en el login local, con `true` la sesión dura 24 h; si no, 2 h de inactividad.

**Respuestas**

| Código | `error` | Cuándo | Qué hacer en la UI |
| --- | --- | --- | --- |
| **204** | — | La cuenta ya existía (Google ya vinculado) | Sesión iniciada: hacer lo mismo que tras el login local (`GET /api/auth/me`, volver a `retorno` o al panel) |
| **201** | — | **Cuenta nueva** creada con Google (rol CLIENTE, sin contraseña). Cuerpo: `UsuarioResponse` (`idUsuario`, `nombres`, `apellidoPaterno`, `correoElectronico`, `estado`, `roles`…) | Sesión iniciada. Se puede mostrar "¡Bienvenido/a a Nubea!" |
| **409** | `DUPLICADO` | Ese correo ya tiene una cuenta con contraseña. `campos.correoElectronico` = "Ya tienes una cuenta con este correo. Inicia sesión con tu contraseña y vincula Google desde tu cuenta." | Mostrar el mensaje con un botón **"Iniciar sesión con contraseña"**, con el correo precargado si se conoce. **No** se vincula automáticamente |
| **422** | `REGLA_NEGOCIO` | El correo de Google no está verificado. `campos.idToken` = "Tu correo de Google no está verificado" | Mostrar el mensaje y ofrecer el registro con correo y contraseña |
| **401** | `GOOGLE_TOKEN_INVALIDO` | Token inválido o vencido. `mensaje` = "No pudimos verificar tu cuenta de Google. Inténtalo de nuevo." | Mostrar el mensaje y permitir reintentar (GIS da un token nuevo) |
| **401** | *(sin cuerpo)* | La cuenta está suspendida o cerrada. Es el mismo 401 sin cuerpo que el login local | Mensaje genérico: "No se pudo iniciar sesión." **No** revelar el motivo |
| **503** | `GOOGLE_NO_DISPONIBLE` | El backend no tiene `GOOGLE_CLIENT_ID` o no pudo obtener las claves de Google. `mensaje` = "El inicio de sesión con Google no está disponible en este momento" | Ocultar el botón o mostrar el mensaje, y ofrecer el login con contraseña |
| **400** | `VALIDACION` | Falta `idToken` | Error de integración (no debería verse) |
| **403** | — | Falta el token CSRF | Error de integración: pedir `/api/auth/csrf` antes |

**Desde la vitrina (store):** el login con Google no crea la relación con la tienda, igual que el login local. Esa relación se crea con el primer "Agregar al carrito" (RN-M1-15). Tras el 201 o el 204, volver a la ruta de `retorno` validada, como hace hoy `autenticacion.tsx`.

## 5. `POST /api/auth/google/vinculacion`: vincular Google a mi cuenta

Requiere **sesión iniciada**. Va en "Mi cuenta" para usuarios que se registraron con contraseña.

**Petición**

```http
POST /api/auth/google/vinculacion
Content-Type: application/json
X-XSRF-TOKEN: <token CSRF>

{ "idToken": "<response.credential>" }
```

**Respuestas**

| Código | `error` | Cuándo | Qué hacer en la UI |
| --- | --- | --- | --- |
| **204** | — | Vinculada. También si ya estaba vinculada a esta misma cuenta | "Listo: ya puedes entrar con Google" |
| **409** | `DUPLICADO` | Esa cuenta de Google es de **otro** usuario. `campos.idToken` = "Esta cuenta de Google ya está vinculada a otro usuario de Nubea." | Mostrar el mensaje |
| **409** | `DUPLICADO` | Esta cuenta ya tiene **otra** cuenta de Google. `campos.idToken` = "Tu cuenta ya tiene vinculada otra cuenta de Google." | Mostrar el mensaje (todavía no se puede desvincular) |
| **422** | `REGLA_NEGOCIO` | Correo de Google sin verificar | Igual que en el login |
| **401** | `GOOGLE_TOKEN_INVALIDO` | Token inválido | Igual que en el login |
| **401** | *(sin cuerpo)* | No hay sesión | Enviar al inicio de sesión |
| **503** | `GOOGLE_NO_DISPONIBLE` | Google no configurado | Ocultar la opción |

El correo de la cuenta de Google **puede ser distinto** del de la cuenta de Nubea: la vinculación la decide el usuario con su sesión iniciada.

## 6. Limitaciones actuales (backlog)

- Una cuenta creada con Google **no tiene contraseña**: no hay "añadir contraseña" ni recuperación por correo.
- **No hay desvinculación** de Google.
- **El registro con contraseña no verifica el correo:** alguien podría registrar el correo de otra persona y bloquearle el alta con Google (recibiría el 409). Por eso la verificación de correo es prioridad alta del backlog (ver `estado-backend.md`).
