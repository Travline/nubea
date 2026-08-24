# Base de datos
Vendedores que crean y modifican su tienda con relación 1:M con la tabla stores
- sellers
  - seller_id UUID PK
  - full_name TEXT NOT NULL
  - email TEXT NOT NULL UNIQUE 
  - password TEXT NOT NULL
  - tax_id TEXT (Esto para cosas como RUC o asi)
  - phone TEXT NOT NULL
  - address TEXT NOT NULL
  - is_active BOOLEAN NOT NULL DEFAULT TRUE (Esto para el superadmin y no perder datos de un vendedor al borrar)
  - created_at TIMESTAMP NOT NULL DEFAULT NOW()

Plantillas para elegir con relacion 1:1 con la tabla stores
- templates
  - template_id SERIAl PK
  - name TEXT NOT NULL
  - description TEXT NOT NULL
  - base_info JSONB NOT NUL (Esto es la estructura de un template con la info default)
  - is_active BOOLEAN NOT NULL DEFAULT TRUE (Manejar visibilidad de los templates)

Provedores de pasarela de pagos con relacion 1:M con la tabla store_payment_configs
- payment_methods
  - method_id SERIAL PK
  - name TEXT NOT NULL
  - description TEXT NOT NULL
  - is_active BOOLEAN NOT NULL DEFAULT TRUE (Visibilidad del metodo para los vendedores)

Tiendas de los vendedores
- stores
  - store_id UUID PK
  - seller_id UUID FK NOT NULL
  - template_id INT FK NOT NULL (Se le asignará por defecto el primer template is_active = true)
  - name TEXT NOT NULL
  - slug TEXT NOT NULL (La ruta para la tienda /mi-tienda)
  - template_info JSONB NOT NULL (Esto queda por separarlo en campos normales para una estructura fija)
  - is_active BOOLEAN NOT NULL DEFAULT TRUE
  - created_at TIMESTAMP NOT NULL DEFAULT NOW()

Tabla intermedia de los metodos de pago habilitados por el vendedor (de base todos solo 1)
- store_payment_configs
  - store_id UUID PK FK
  - method_id INTEGER PK FK

Tabla para las categorias de las tiendas
- categories
  - category_id UUID PK
  - store_id UUID FK NOT NULL
  - name TEXT NOT NULL
  - slug TEXT NOT NULL
  - is_active BOOLEAN NOT NULL DEFAULT TRUE

Productos de cada tienda
- products
  - product_id UUID PK
  - store_id UUID FK NOT NULL
  - category_id UUID FK NULL (ON DELETE SET NULL)
  - title TEXT NOT NULL
  - slug TEXT NOT NULL
  - description TEXT NOT NULL
  - price NUMERIC(10,2) NOT NULL (CHECK price >= 0)
  - promo_price NUMERIC(10,2) NULL (CHECK promo_price >= 0)
  - sku TEXT NULL
  - is_visible BOOLEAN DEFAULT TRUE
  - is_unlimited_stock BOOLEAN DEFAULT FALSE
  - stock INT DEFAULT 0 (CHECK stock >= 0)
  - min_stock INT DEFAULT 0
  - max_stock INT NULL
  - is_physical BOOLEAN DEFAULT TRUE
  - weight_kg NUMERIC(6,3) NULL (CHECK weight_kg >= 0)
  - dimensions TEXT NULL (Solo haremos un formateo de las 3 medidas y pasarlas a un 5x10x45 por ejemplo ya que no haremos ninguna logica basado en esto)
  - variant_group_id UUID NOT NULL (Esto para manejar la agrupación de variantes)
  - created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP

Lista de imagenes de un producto
- product_images
  - image_id UUID PK (el nombre del recurso)
  - format TEXT NOT NULL (el .jpg, .webp, etc)
  - product_id FK NOT NULL
  - is_poster BOOLEAN NOT NULL DEFAULT FALSE (esto especificará la imagen principal a mostrar por cada producto)
  - is_active BOOLEAN NOT NULL DEFAULT TRUE

Colecciones personalizadas del vendedor
- collections
  - collection_id UUID PK
  - store_id UUID FK NOT NULL
  - title TEXT NOT NULL
  - slug TEXT NOT NULL

Guarda los productos que contiene una colección
- collection_products
  - collection_id UUID PK FK
  - product_id UUID PK FK

Usuarios registrados por cada tienda
- users
  - user_id UUID PK
  - store_id UUID FK NOT NULL
  - email NOT NULL (se manejara restricción de email unico por tienda UNIQUE(store_id, email))
  - name TEXT NOT NULL
  - last_name TEXT NOT NULL
  - phone TEXT NOT NULL (libphonenumber o alguna similar para el manejo de numeros de telefono) 
  - created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP

Carritos de usuarios por tienda
- carts
  - cart_id UUID PK
  - store_id UUID FK NOT NULL
  - user_id UUID FK NOT NULL (los usuarios no autenticados tendran su carrito en local)
  - updated_at TIMESTAMP NOT NULL

Items del carrito
- cart_items
  - item_id UUID PK
  - cart_id UUID FK NOT NULL
  - quantity INTEGER CHECK (quantity > 0)
  - unit_price NUMERIC(10,2)

Orden creada al darle comprar (carrito -> pasarela de pago)
- orders
  - order_id UUID PK
  - store_id UUID FK NOT NULL
  - user_id UUID FK NOT NULL
  - total NUMERIC(10,2)
  - status TEXT NOT NULL (maneja el estado del pago como PENDING, REJECTED, APROVED, EXPIRED)
  - created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP

Items de la orden
- order_items
  - item_id UUID PK
  - order_id UUID FK NOT NULL
  - quantity INTEGER CHECK (quantity > 0)
  - unit_price NUMERIC(10,2)

# Tecnologias
Esto agrupa tanto toecnoligas a nivel stack como las usadas para manejar el desarrollo
- Spring
- React (React Router, Tailwind, Zustand, Zod)
- Postgre
- Minio (Storage en desarrollo)
- Pgweb / CloudBeaver (Visualizacion web de la bd en desarrollo)
- Docker (Contenedores para api, bd, storage)

# Servicios
Infraestructura para las apps y servicios necesarios
- Vercel
- Supabase
- Render