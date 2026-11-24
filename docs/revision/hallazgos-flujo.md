# NUBEA · Hallazgos del flujo manual

Registro de lo encontrado al probar el sistema en local (rama `feature/miguel-local`).
Cada punto indica dónde está el problema y la regla de negocio relacionada.

Etiquetas: **[UI]** visual · **[UX]** flujo o lenguaje · **[Backend]** API o reglas · **[Local]** solo entorno de desarrollo

---

## 1. Portada y registro (plataforma)

- **[UI]** Con la ventana angosta, el header fijo tapa el título de la portada ("Tu tienda online, sin vueltas").

## 2. Tiendas

- **[UX]** Una tienda se puede activar sin productos ni configuración. Es correcto según las reglas (RN-M2-13 exige tienda ACTIVA para publicar productos), pero falta definir qué ve el comprador en una tienda activa sin productos (por ejemplo, "Próximamente").

- **[OK]** Arreglo de tiendas suspendidas probado a mano: al quitar el rol DUENO_TIENDA, la tienda pasa a SUSPENDIDA (RN-M1-10) y el dueño ya no puede guardar cambios de inventario.
- **[UX]** Con la tienda suspendida, el panel muestra el mensaje genérico "No se pudo completar la solicitud." El frontend debería:
  - Mostrar el motivo según el código: 403 → "Ya no tienes permiso para administrar tiendas"; 409 → "Tu tienda está suspendida: puedes consultar, pero no modificar".
  - Mostrar un aviso fijo en la cabecera de la tienda cuando el estado es SUSPENDIDA y dejar los formularios en solo lectura (botones deshabilitados), en vez de dejar editar y fallar al guardar.
- **[Pendiente revisar]** Qué ve un comprador al entrar a la vitrina de una tienda SUSPENDIDA (debería indicar que no está disponible y no permitir comprar).

## 3. Catálogo: slugs

- **[Backend]** Todos los slugs deben generarse automáticamente a partir del nombre, como ya ocurre con la tienda (`SlugUtil`). Hoy `CategoriaService` toma el slug tal cual viene del formulario.
  - Revisar también colecciones y productos.
  - El usuario no debería ver ni escribir el slug; como mucho, editarlo en una opción avanzada.
- **[Backend/UX]** Si el slug tiene tildes, se muestra el error genérico "Los datos no cumplen las reglas de la base de datos". El mensaje debe indicar qué campo falla y por qué. Con la autogeneración este caso desaparece, pero el error genérico sigue afectando a otras restricciones.
- **[Pendiente revisar]** El formulario de categoría parece no reflejar todos los campos de la tabla `categoria`.

## 4. Catálogo: productos y presentaciones

- **[UX]** "Presentación" es el nombre de la tabla (`presentacion_producto`), no un término que entienda un vendedor. En e-commerce se usa "variante" u "opción" (por ejemplo, "55 pulgadas / Negro").
- **[UX]** Para publicar un producto hacen falta cuatro pasos repartidos en dos pantallas: crear el producto, crear una presentación con SKU, ir a Inventario a cargar stock y volver a Publicar. El usuario no sabe ese orden y solo lo descubre por el error al publicar (RN-M2-13).
  - Propuesta: un asistente o checklist en la ficha del producto ("✔ Categoría activa · ✖ Falta una presentación · ✖ Falta stock").
  - Propuesta: permitir cargar el stock inicial al crear la presentación.
- **[UX]** Para un producto sin variantes, el sistema debería crear la presentación única automáticamente (RN-M2-11: presentación con firma vacía), en vez de pedirle al usuario que entienda qué es una presentación.
- **[UX]** El SKU es obligatorio y único por tienda (RN-M2-07), pero el usuario no sabe qué poner. Propuesta: sugerir uno autogenerado (por ejemplo, `MIGUELTECH-TELEVISOR-HD-4K-001`) que se pueda editar.
- **[UX]** "Precio especial opcional" no explica que reemplaza al precio del producto para esa presentación.
- **[UI]** Campos sin etiqueta visible en la edición del producto (por ejemplo, el precio `1200` y el nombre aparecen sin rótulo encima).
- **[UX]** Los botones "Publicar" y "Desactivar" aparecen siempre, aunque falten requisitos. Deberían estar deshabilitados con el motivo, o mostrar el checklist.

## 5. Vitrina (apps/store)

- **[OK]** El flujo completo funciona: crear tienda → activar → categoría → producto → presentación → stock → publicar → aparece en la vitrina con precio y "Agregar al carrito".
- **[UX]** La ficha del producto solo muestra "Disponible", no cuántas unidades quedan. Definir criterio: por ejemplo "Disponible", "¡Últimas N unidades!" cuando el stock es bajo, y "Agotado" (con el botón deshabilitado).
- **[UX]** El comprador ve el término interno "Presentación" y la opción "Única". Si el producto no tiene variantes, el selector no debería mostrarse. Si las tiene, debería decir "Color", "Talla", etc.
- **[Backend/UI]** Solo existe la plantilla `minimal` en `apps/store/src/vitrina.tsx`. Si a una tienda se le asigna cualquier otra plantilla de la tabla `plantilla`, la vitrina muestra "La plantilla de esta tienda no está disponible". El admin puede crear plantillas en el panel, pero no tienen diseño detrás. Hay que decidir si habrá varias plantillas reales o solo una con opciones de apariencia (colores, banners, carrusel).
- **[UI]** El diseño de la vitrina es distinto al de la plataforma (claro/verde frente a oscuro/azul). Está bien que sean distintos, pero hay que definir la identidad visual de cada uno en el rediseño.

## 6. Sesión del comprador y carrito

- **[UX/Decisión]** La sesión es compartida entre la plataforma y la vitrina: si el dueño inició sesión en el panel, entra a su tienda ya logueado como comprador. Pasa porque es una sola cuenta global y la cookie de sesión es del backend; en local, además, las cookies no distinguen puertos (5173 y 5174 comparten `localhost`). Esto es coherente con la "cuenta única tipo Shop", pero falta:
  - Mostrar en la vitrina "Continuar como correo@… / Usar otra cuenta" en vez de entrar en silencio.
  - Decidir si el dueño puede comprar en su propia tienda (Shopify lo permite para pruebas). Si no, bloquearlo en el carrito con un mensaje.
- **[Local]** Para probar como comprador sin la sesión del dueño, usar una ventana privada en la vitrina.
- **[UX]** Formulario de dirección: departamento, provincia y distrito son texto libre (decisión RN-M4-01, opción A). Con Google Places, el autocompletado debería llenarlos solo; sin clave de Maps, el usuario los escribe a mano.
- **[UX]** El formulario de dirección se muestra completo y abierto dentro del carrito, y empuja el resumen y el pago hacia abajo. Mejor un paso de checkout separado, o un modal.

## 7. Funcionalidades nuevas (backlog, no ahora)

- **Reconocimiento entre tiendas (estilo Shop):** el diseño ya lo contempla (cuenta única + `tienda_cliente`, RN-M1-14/15). Falta la experiencia: al entrar a otra tienda NUBEA, reconocer la cuenta y ofrecer "Continuar como…". Si en el futuro hay dominios propios por tienda, la cookie ya no se comparte y haría falta un flujo de inicio de sesión centralizado (SSO).
- **Verificación de correo:** recomendado. Hoy el registro no verifica que el correo sea de quien se registra (riesgo ya señalado en `docs/despliegue/render-backend.md`). Propuesta: código de 6 dígitos al registrarse (una vez, no en cada login). Opcional: inicio de sesión sin contraseña con código por correo, como Shop. Requiere un servicio de envío de correos (SMTP, Resend, SendGrid…).
- **Login con Google (RN-M1-05):** cliente OAuth ya creado en Google Cloud (modo prueba); falta implementarlo en el backend y en las dos apps.

## 8. Local (no requiere cambios para producción)

- **[Local]** "Ver tienda pública" usa una ruta relativa (`/tienda/<slug>`). En local da 404 porque la vitrina corre en otro puerto (5174). En producción funciona, porque las dos apps se sirven bajo el mismo dominio.
- **[Local]** Para usar la vitrina en local hay que agregar `http://localhost:5174` a `APP_CORS_ALLOWED_ORIGINS`.
