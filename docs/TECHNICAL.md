# Base de datos
Usuarios globales del sistema (cuentas principales)
- users
  - user_id UUID PK
  - email TEXT NOT NULL UNIQUE
  - password_hash TEXT NOT NULL
  - first_name TEXT NOT NULL
  - last_name TEXT NOT NULL
  - phone TEXT NULL
  - is_active BOOLEAN NOT NULL DEFAULT TRUE
  - created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()

Administradores de la plataforma (Superadmin)
- platform_admins
  - user_id UUID PK FK REFERENCES users(user_id) ON DELETE CASCADE
  - is_active INT NOT NULL DEFAULT 1
  - created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()

Plantillas para elegir con relación 1:M con la tabla stores
- templates
  - template_id SERIAL PK
  - name TEXT NOT NULL
  - description TEXT NOT NULL
  - is_active BOOLEAN NOT NULL DEFAULT TRUE

Tiendas creadas en la plataforma
- stores
  - store_id UUID PK
  - template_id INT FK NOT NULL REFERENCES templates(template_id)
  - name TEXT NOT NULL
  - slug TEXT NOT NULL UNIQUE
  - is_active BOOLEAN NOT NULL DEFAULT TRUE
  - created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()

Personal de la tienda (Propietarios y Soporte)
- store_staff
  - staff_id SERIAL PK
  - user_id UUID FK NOT NULL REFERENCES users(user_id) ON DELETE CASCADE
  - store_id UUID FK NOT NULL REFERENCES stores(store_id) ON DELETE CASCADE
  - role TEXT NOT NULL CHECK (role IN ('OWNER', 'SUPPORT'))
  - tax_id TEXT NULL (RUC o documento fiscal)
  - created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
  - UNIQUE (store_id, user_id)

Clientes registrados por cada tienda
- customers
  - customer_id SERIAL PK
  - user_id UUID FK NULL REFERENCES users(user_id) ON DELETE SET NULL
  - store_id UUID FK NOT NULL REFERENCES stores(store_id) ON DELETE CASCADE
  - total_orders INT NOT NULL DEFAULT 0
  - note TEXT NULL
  - created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
  - UNIQUE (store_id, user_id)

Info de las tiendas para mostrar en sus plantillas
- template_info
  - info_id SERIAL PK
  - store_id UUID FK NOT NULL UNIQUE REFERENCES stores(store_id) ON DELETE CASCADE
  - name TEXT NOT NULL DEFAULT 'Mi tienda'
  - hero_text TEXT NOT NULL DEFAULT 'Bienvenido a Mi tienda'
  - logo_img TEXT NULL
  - hero_img TEXT NULL
  - ig_link TEXT NULL
  - twitter_link TEXT NULL
  - fb_link TEXT NULL
  - tiktok_link TEXT NULL
  - contact_email TEXT NOT NULL
  - contact_phone TEXT NULL
  - updated_at TIMESTAMPTZ NULL

Configuraciones y tokens de métodos de pago habilitados por tienda
- payment_configs
  - config_id SERIAL PK
  - store_id UUID FK NOT NULL REFERENCES stores(store_id) ON DELETE CASCADE
  - access_token TEXT NOT NULL
  - refresh_token TEXT NOT NULL
  - expires_at TIMESTAMPTZ NOT NULL
  - scope TEXT NOT NULL

Categorías del catálogo por tienda
- categories
  - category_id SERIAL PK
  - store_id UUID FK NOT NULL REFERENCES stores(store_id) ON DELETE CASCADE
  - name TEXT NOT NULL
  - slug TEXT NOT NULL
  - is_active BOOLEAN NOT NULL DEFAULT TRUE
  - UNIQUE (store_id, slug)

Productos de cada tienda
- products
  - product_id SERIAL PK
  - store_id UUID FK NOT NULL REFERENCES stores(store_id) ON DELETE CASCADE
  - category_id INT FK NULL REFERENCES categories(category_id) ON DELETE SET NULL
  - title TEXT NOT NULL
  - slug TEXT NOT NULL
  - description TEXT NOT NULL
  - price NUMERIC(10,2) NOT NULL CHECK (price >= 0)
  - promo_price NUMERIC(10,2) NULL CHECK (promo_price IS NULL OR promo_price >= 0)
  - sku TEXT NULL
  - is_visible BOOLEAN NOT NULL DEFAULT TRUE
  - is_unlimited_stock BOOLEAN NOT NULL DEFAULT FALSE
  - stock INT NOT NULL DEFAULT 0 CHECK (stock >= 0)
  - min_stock INT NOT NULL DEFAULT 0 CHECK (min_stock >= 0)
  - max_stock INT NULL CHECK (max_stock IS NULL OR max_stock >= 0)
  - is_physical BOOLEAN NOT NULL DEFAULT TRUE
  - weight_kg NUMERIC(6,3) NULL CHECK (weight_kg IS NULL OR weight_kg >= 0)
  - dimensions TEXT NULL
  - variant_group_id UUID NOT NULL
  - variant_name TEXT NOT NULL
  - created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
  - UNIQUE (store_id, slug)
  - CHECK (max_stock IS NULL OR min_stock <= max_stock)

Imágenes de productos
- product_images
  - image_id SERIAL PK
  - product_id INT FK NOT NULL REFERENCES products(product_id) ON DELETE CASCADE
  - format TEXT NOT NULL
  - is_poster BOOLEAN NOT NULL DEFAULT FALSE
  - is_active BOOLEAN NOT NULL DEFAULT TRUE

Colecciones personalizadas por tienda
- collections
  - collection_id SERIAL PK
  - store_id UUID FK NOT NULL REFERENCES stores(store_id) ON DELETE CASCADE
  - title TEXT NOT NULL
  - slug TEXT NOT NULL
  - UNIQUE (store_id, slug)

Guarda los productos que contiene una colección (Tabla intermedia M:N)
- collection_products
  - collection_id INT PK FK REFERENCES collections(collection_id) ON DELETE CASCADE
  - product_id INT PK FK REFERENCES products(product_id) ON DELETE CASCADE
  - PRIMARY KEY (collection_id, product_id)

Carritos de compras de clientes por tienda
- carts
  - cart_id SERIAL PK
  - store_id UUID FK NOT NULL REFERENCES stores(store_id) ON DELETE CASCADE
  - customer_id INT FK NOT NULL REFERENCES customers(customer_id) ON DELETE CASCADE
  - updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
  - UNIQUE (store_id, customer_id)

Ítems agregados dentro del carrito
- cart_items
  - item_id SERIAL PK
  - cart_id INT FK NOT NULL REFERENCES carts(cart_id) ON DELETE CASCADE
  - product_id INT FK NOT NULL REFERENCES products(product_id) ON DELETE RESTRICT
  - quantity INT NOT NULL CHECK (quantity > 0)
  - unit_price NUMERIC(10,2) NULL CHECK (unit_price IS NULL OR unit_price >= 0)
  - UNIQUE (cart_id, product_id)

Órdenes de compra generadas
- orders
  - order_id SERIAL PK
  - store_id UUID FK NOT NULL REFERENCES stores(store_id) ON DELETE RESTRICT
  - customer_id INT FK NOT NULL REFERENCES customers(customer_id) ON DELETE RESTRICT
  - total NUMERIC(10,2) NOT NULL CHECK (total >= 0)
  - status TEXT NOT NULL CHECK (status IN ('PENDING', 'REJECTED', 'APPROVED', 'EXPIRED'))
  - created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()

Ítems de una orden de compra
- order_items
  - item_id SERIAL PK
  - order_id INT FK NOT NULL REFERENCES orders(order_id) ON DELETE CASCADE
  - product_id INT FK NOT NULL REFERENCES products(product_id) ON DELETE RESTRICT
  - quantity INT NOT NULL CHECK (quantity > 0)
  - product_name TEXT NOT NULL
  - unit_price NUMERIC(10,2) NOT NULL CHECK (unit_price >= 0)

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