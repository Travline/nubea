# Alcance
- El vendedor podrá seleccionar una plantilla y cambiar la información mostrada
  - Logo
  - Textos
  - Imagenes de la pagina principal
  - Color primario
  - Link a redes
- El vendedor podrá gestionar los siguientes aspectos sobre sus productos:
  - Categorias
  - Precios promoción y precio venta normal
  - Imagenes
  - Variantes de producto
  - SKU (identificador externo para referencia del vendedor)
  - Visibilidad
  - Stock (puede seleccionar ilimitado)
  - Descripción
  - Tipo fisico o digital (esto solo definirá si añade dimensiones)
  - Dimensiones y peso
  - Stock min max
- El vendedor podrá definir colecciones personalizadas, seleccioanndo produtctos y se mostrará en la pagina de inicio
- El vendedor podrá definir un método de pago (sin este definido no se podrán realizar compras en la tienda)

# No cubierto por el proyecto
- El vendedor no podrá añadir recursos como videos para los productos pero si para la elementos de la plantilla (esto aún no sé si esq no mejor o si solo con link de yutu para productos noma xd)
- El vendedor no podrá personalizar la plantilla más en cuanto a ubicación de elementos u otros aspectos no mencionados en el alcance.
- El vendedor no podrá hacer carga masiva de productos o ventas via excel, csv u otro formato (para no complicarnos xd)
- No se considera tener un registro de movimientos como Kardex
- El vendedor no podrá alterar el estilo de texto (descripciones, nombres, etc)
- No se manejará sub categorias como Tecnologia/Laptops/Oficina, todo debe ir en una categoria general o hacer una nueva
- Solo se puede hacer tienda de productos no tienda de un unico producto (amenos q se nos de por añadir eso junto a una plantilla ya si)
- No se podrá hacer cambios masivos en productos (como seleccionar todos estos productos y cambiar su categoria o precio)
- No se manejará metodos de envio, ni su coste, ni estado de envío
- No se añadiran distintos servicios para los pagos, solo uno que funcione 

# Flujo vendedor
1. Llega a querer contratar el servicio con la home page para vendedores
  - Para la contratación no es necesario hacer un pago (justificado bajo capa gratuita)
  - Esto lleva a un formulario de registro donde se tomará datos generales
    - Nombre y apellido
    - Correo
    - Contraseña
    - RUC (en cuanto a esto se supone que si es global pues no es exactamente asi en todos lados)
    - Teléfono
    - Dirección (entiendo q esto era como para la facturación aparte ver si llegamos a envios)
2. Verá el panel admin donde gestionará sus tiendas
  - Crear
    - Esto le mostrará un formulario
      - Nombre de tienda
      - Elección de plantilla
      - Boton crear
  - Desactivar
  - Eliminar
3. Selecciona una tienda y entrará al panel de gestion de la tienda 
  - Inicio
    - Editar nombre, logo, plantilla, etc
  - Ventas
    - Filtrar ventas y visualizacion general
  - Productos
    - Gestiona productos y categorias 
  - Colecciones (esto son las lista personalizada por el vendedor)
  - Pagos
