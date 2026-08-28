-- ============================================================
-- 1. IDENTIDAD CENTRAL Y ROLES
-- ============================================================

CREATE TABLE users (
    user_id UUID PRIMARY KEY,
    email TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    first_name TEXT NOT NULL,
    last_name TEXT NOT NULL,
    phone TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE platform_admins (
    user_id UUID PRIMARY KEY REFERENCES users(user_id) ON DELETE CASCADE,
    is_active INT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- ============================================================
-- 2. TIENDAS Y PLANTILLAS
-- ============================================================

CREATE TABLE templates (
    template_id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE stores (
    store_id UUID PRIMARY KEY,
    template_id INT NOT NULL REFERENCES templates(template_id),
    name TEXT NOT NULL,
    slug TEXT NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE store_staff (
    staff_id SERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    store_id UUID NOT NULL REFERENCES stores(store_id) ON DELETE CASCADE,
    role TEXT NOT NULL CHECK (role IN ('OWNER', 'SUPPORT')),
    tax_id TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (store_id, user_id)
);

CREATE TABLE customers (
    customer_id SERIAL PRIMARY KEY,
    user_id UUID REFERENCES users(user_id) ON DELETE SET NULL,
    store_id UUID NOT NULL REFERENCES stores(store_id) ON DELETE CASCADE,
    total_orders INT NOT NULL DEFAULT 0,
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (store_id, user_id)
);

CREATE TABLE template_info (
    info_id SERIAL PRIMARY KEY,
    store_id UUID NOT NULL UNIQUE REFERENCES stores(store_id) ON DELETE CASCADE,
    name TEXT NOT NULL DEFAULT 'Mi tienda',
    hero_text TEXT NOT NULL DEFAULT 'Bienvenido a Mi tienda',
    logo_img TEXT,
    hero_img TEXT,
    ig_link TEXT,
    twitter_link TEXT,
    fb_link TEXT,
    tiktok_link TEXT,
    contact_email TEXT NOT NULL,
    contact_phone TEXT,
    updated_at TIMESTAMPTZ
);

CREATE TABLE payment_configs (
    config_id SERIAL PRIMARY KEY,
    store_id UUID NOT NULL REFERENCES stores(store_id) ON DELETE CASCADE,
    access_token TEXT NOT NULL,
    refresh_token TEXT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    scope TEXT NOT NULL
);


-- ============================================================
-- 3. CATÁLOGO DE PRODUCTOS
-- ============================================================

CREATE TABLE categories (
    category_id SERIAL PRIMARY KEY,
    store_id UUID NOT NULL REFERENCES stores(store_id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    slug TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (store_id, slug)
);

CREATE TABLE products (
    product_id SERIAL PRIMARY KEY,
    store_id UUID NOT NULL REFERENCES stores(store_id) ON DELETE CASCADE,
    category_id INT REFERENCES categories(category_id) ON DELETE SET NULL,
    title TEXT NOT NULL,
    slug TEXT NOT NULL,
    description TEXT NOT NULL,
    price NUMERIC(10,2) NOT NULL CHECK (price >= 0),
    promo_price NUMERIC(10,2) CHECK (promo_price IS NULL OR promo_price >= 0),
    sku TEXT,
    is_visible BOOLEAN NOT NULL DEFAULT TRUE,
    is_unlimited_stock BOOLEAN NOT NULL DEFAULT FALSE,
    stock INT NOT NULL DEFAULT 0 CHECK (stock >= 0),
    min_stock INT NOT NULL DEFAULT 0 CHECK (min_stock >= 0),
    max_stock INT CHECK (max_stock IS NULL OR max_stock >= 0),
    is_physical BOOLEAN NOT NULL DEFAULT TRUE,
    weight_kg NUMERIC(6,3) CHECK (weight_kg IS NULL OR weight_kg >= 0),
    dimensions TEXT,
    variant_group_id UUID NOT NULL,
    variant_name TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (store_id, slug),
    CHECK (max_stock IS NULL OR min_stock <= max_stock)
);

CREATE TABLE product_images (
    image_id SERIAL PRIMARY KEY,
    product_id INT NOT NULL REFERENCES products(product_id) ON DELETE CASCADE,
    format TEXT NOT NULL,
    is_poster BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE collections (
    collection_id SERIAL PRIMARY KEY,
    store_id UUID NOT NULL REFERENCES stores(store_id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    slug TEXT NOT NULL,
    UNIQUE (store_id, slug)
);

CREATE TABLE collection_products (
    collection_id INT NOT NULL REFERENCES collections(collection_id) ON DELETE CASCADE,
    product_id INT NOT NULL REFERENCES products(product_id) ON DELETE CASCADE,
    PRIMARY KEY (collection_id, product_id)
);


-- ============================================================
-- 4. CARRITO Y PEDIDOS
-- ============================================================

CREATE TABLE carts (
    cart_id SERIAL PRIMARY KEY,
    store_id UUID NOT NULL REFERENCES stores(store_id) ON DELETE CASCADE,
    customer_id INT NOT NULL REFERENCES customers(customer_id) ON DELETE CASCADE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (store_id, customer_id)
);

CREATE TABLE cart_items (
    item_id SERIAL PRIMARY KEY,
    cart_id INT NOT NULL REFERENCES carts(cart_id) ON DELETE CASCADE,
    product_id INT NOT NULL REFERENCES products(product_id) ON DELETE RESTRICT,
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(10,2) CHECK (unit_price IS NULL OR unit_price >= 0),
    UNIQUE (cart_id, product_id)
);

CREATE TABLE orders (
    order_id SERIAL PRIMARY KEY,
    store_id UUID NOT NULL REFERENCES stores(store_id) ON DELETE RESTRICT,
    customer_id INT NOT NULL REFERENCES customers(customer_id) ON DELETE RESTRICT,
    total NUMERIC(10,2) NOT NULL CHECK (total >= 0),
    status TEXT NOT NULL CHECK (status IN ('PENDING', 'REJECTED', 'APPROVED', 'EXPIRED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE order_items (
    item_id SERIAL PRIMARY KEY,
    order_id INT NOT NULL REFERENCES orders(order_id) ON DELETE CASCADE,
    product_id INT NOT NULL REFERENCES products(product_id) ON DELETE RESTRICT,
    quantity INT NOT NULL CHECK (quantity > 0),
    product_name TEXT NOT NULL,
    unit_price NUMERIC(10,2) NOT NULL CHECK (unit_price >= 0)
);