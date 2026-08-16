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
  - created_at TIMESTAMP NOT NULL DEFAULT NOW()

Plantillas para elegir con relacion 1:1 con la tabla stores
- templates
  - template_id SERIAl PK
  - name TEXT NOT NULL
  - description TEXT NOT NULL
  - base_info JSONB NOT NUL (Esto es la estructura de un template con la info default)
  - is_active BOOLEAN NOT NULL (Manejar visibilidad de los templates)

Provedores de pasarela de pagos con relacion 1:M con la tabla store_payment_configs
- payment_methods
  - method_id SERIAL PK
  - name TEXT NOT NULL
  - description TEXT NOT NULL
  - is_active BOOLEAN NOT NULL (Visibilidad del metodo para los vendedores)

Tiendas de los vendedores 
- stores
  - store_id UUID PK
  - seller_id UUID FK NOT NULL
  - template_id INT FK NOT NULL (Se le asignará por defecto el primer template is_active = true)
  - name TEXT NOT NULL


Productos de cada tienda
- products

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