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
  - method_id UUID PK FK

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
  - dimensions JSONB NULL
  - images TEXT[] DEFAULT '{}'
  - created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP

Usuarios de las tiendas 
- users

Carros de compra de cada tienda
- carts

Items de los carros
- cart_items




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