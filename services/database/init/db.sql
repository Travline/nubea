CREATE EXTENSION IF NOT EXISTS pgcrypto;


-- ============================================================
-- SELLERS
-- ============================================================

CREATE TABLE sellers (
    seller_id UUID PRIMARY KEY,

    full_name TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,

    tax_id TEXT,
    phone TEXT NOT NULL,
    address TEXT NOT NULL,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- ============================================================
-- TEMPLATES
-- ============================================================

CREATE TABLE templates (
    template_id SERIAL PRIMARY KEY,

    name TEXT NOT NULL,
    description TEXT NOT NULL,

    is_active BOOLEAN NOT NULL DEFAULT TRUE
);


-- ============================================================
-- STORES
-- ============================================================

CREATE TABLE stores (
    store_id UUID PRIMARY KEY,

    seller_id UUID NOT NULL,
    template_id INTEGER NOT NULL,

    name TEXT NOT NULL,
    slug TEXT NOT NULL,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_stores_seller
        FOREIGN KEY (seller_id)
        REFERENCES sellers(seller_id),

    CONSTRAINT fk_stores_template
        FOREIGN KEY (template_id)
        REFERENCES templates(template_id),

    CONSTRAINT uq_stores_slug
        UNIQUE (slug)
);


-- ============================================================
-- TEMPLATE INFO
-- 1:1 con stores
-- UUIDv4 porque no necesita ordenamiento
-- ============================================================

CREATE TABLE template_info (
    info_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    store_id UUID NOT NULL UNIQUE,

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

    updated_at TIMESTAMPTZ,

    CONSTRAINT fk_template_info_store
        FOREIGN KEY (store_id)
        REFERENCES stores(store_id)
        ON DELETE CASCADE
);


-- ============================================================
-- PAYMENT CONFIGS
-- ============================================================

CREATE TABLE payment_configs (
    config_id UUID PRIMARY KEY,

    store_id UUID NOT NULL,

    access_token TEXT NOT NULL,
    refresh_token TEXT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,

    scope TEXT NOT NULL,

    CONSTRAINT fk_payment_configs_store
        FOREIGN KEY (store_id)
        REFERENCES stores(store_id)
        ON DELETE CASCADE
);


-- ============================================================
-- CATEGORIES
-- ============================================================

CREATE TABLE categories (
    category_id UUID PRIMARY KEY,

    store_id UUID NOT NULL,

    name TEXT NOT NULL,
    slug TEXT NOT NULL,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_categories_store
        FOREIGN KEY (store_id)
        REFERENCES stores(store_id)
        ON DELETE CASCADE,

    CONSTRAINT uq_categories_store_slug
        UNIQUE (store_id, slug)
);


-- ============================================================
-- PRODUCTS
-- ============================================================

CREATE TABLE products (
    product_id UUID PRIMARY KEY,

    store_id UUID NOT NULL,
    category_id UUID,

    title TEXT NOT NULL,
    slug TEXT NOT NULL,
    description TEXT NOT NULL,

    price NUMERIC(10,2) NOT NULL,
    promo_price NUMERIC(10,2),

    sku TEXT,

    is_visible BOOLEAN NOT NULL DEFAULT TRUE,

    is_unlimited_stock BOOLEAN NOT NULL DEFAULT FALSE,

    stock INTEGER NOT NULL DEFAULT 0,
    min_stock INTEGER NOT NULL DEFAULT 0,
    max_stock INTEGER,

    is_physical BOOLEAN NOT NULL DEFAULT TRUE,

    weight_kg NUMERIC(6,3),
    dimensions TEXT,

    variant_group_id UUID NOT NULL,
    variant_name TEXT NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_products_store
        FOREIGN KEY (store_id)
        REFERENCES stores(store_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_products_category
        FOREIGN KEY (category_id)
        REFERENCES categories(category_id)
        ON DELETE SET NULL,

    CONSTRAINT chk_products_price
        CHECK (price >= 0),

    CONSTRAINT chk_products_promo_price
        CHECK (promo_price IS NULL OR promo_price >= 0),

    CONSTRAINT chk_products_stock
        CHECK (stock >= 0),

    CONSTRAINT chk_products_min_stock
        CHECK (min_stock >= 0),

    CONSTRAINT chk_products_max_stock
        CHECK (max_stock IS NULL OR max_stock >= 0),

    CONSTRAINT chk_products_weight
        CHECK (weight_kg IS NULL OR weight_kg >= 0),

    CONSTRAINT chk_products_stock_range
        CHECK (max_stock IS NULL OR min_stock <= max_stock),

    CONSTRAINT uq_products_store_slug
        UNIQUE (store_id, slug)
);


-- ============================================================
-- PRODUCT IMAGES
-- ============================================================

CREATE TABLE product_images (
    image_id UUID PRIMARY KEY,

    format TEXT NOT NULL,

    product_id UUID NOT NULL,

    is_poster BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_product_images_product
        FOREIGN KEY (product_id)
        REFERENCES products(product_id)
        ON DELETE CASCADE
);


-- ============================================================
-- COLLECTIONS
-- ============================================================

CREATE TABLE collections (
    collection_id UUID PRIMARY KEY,

    store_id UUID NOT NULL,

    title TEXT NOT NULL,
    slug TEXT NOT NULL,

    CONSTRAINT fk_collections_store
        FOREIGN KEY (store_id)
        REFERENCES stores(store_id)
        ON DELETE CASCADE,

    CONSTRAINT uq_collections_store_slug
        UNIQUE (store_id, slug)
);


-- ============================================================
-- COLLECTION PRODUCTS
-- ============================================================

CREATE TABLE collection_products (
    collection_id UUID NOT NULL,
    product_id UUID NOT NULL,

    PRIMARY KEY (collection_id, product_id),

    CONSTRAINT fk_collection_products_collection
        FOREIGN KEY (collection_id)
        REFERENCES collections(collection_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_collection_products_product
        FOREIGN KEY (product_id)
        REFERENCES products(product_id)
        ON DELETE CASCADE
);


-- ============================================================
-- USERS
-- ============================================================

CREATE TABLE users (
    user_id UUID PRIMARY KEY,

    store_id UUID NOT NULL,

    email TEXT NOT NULL,

    name TEXT NOT NULL,
    last_name TEXT NOT NULL,

    phone TEXT NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_users_store
        FOREIGN KEY (store_id)
        REFERENCES stores(store_id)
        ON DELETE CASCADE,

    CONSTRAINT uq_users_store_email
        UNIQUE (store_id, email)
);


-- ============================================================
-- CARTS
-- ============================================================

CREATE TABLE carts (
    cart_id UUID PRIMARY KEY,

    store_id UUID NOT NULL,
    user_id UUID NOT NULL,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_carts_store
        FOREIGN KEY (store_id)
        REFERENCES stores(store_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_carts_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE,

    CONSTRAINT uq_carts_store_user
        UNIQUE (store_id, user_id)
);


-- ============================================================
-- CART ITEMS
-- ============================================================

CREATE TABLE cart_items (
    item_id UUID PRIMARY KEY,

    product_id UUID NOT NULL,
    cart_id UUID NOT NULL,

    quantity INTEGER NOT NULL,

    unit_price NUMERIC(10,2),

    CONSTRAINT fk_cart_items_product
        FOREIGN KEY (product_id)
        REFERENCES products(product_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_cart_items_cart
        FOREIGN KEY (cart_id)
        REFERENCES carts(cart_id)
        ON DELETE CASCADE,

    CONSTRAINT chk_cart_items_quantity
        CHECK (quantity > 0),

    CONSTRAINT chk_cart_items_unit_price
        CHECK (unit_price IS NULL OR unit_price >= 0),

    CONSTRAINT uq_cart_items_cart_product
        UNIQUE (cart_id, product_id)
);


-- ============================================================
-- ORDERS
-- ============================================================

CREATE TABLE orders (
    order_id UUID PRIMARY KEY,

    store_id UUID NOT NULL,
    user_id UUID NOT NULL,

    total NUMERIC(10,2) NOT NULL,

    status TEXT NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_orders_store
        FOREIGN KEY (store_id)
        REFERENCES stores(store_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_orders_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_orders_total
        CHECK (total >= 0),

    CONSTRAINT chk_orders_status
        CHECK (
            status IN (
                'PENDING',
                'REJECTED',
                'APPROVED',
                'EXPIRED'
            )
        )
);


-- ============================================================
-- ORDER ITEMS
-- ============================================================

CREATE TABLE order_items (
    item_id UUID PRIMARY KEY,

    product_id UUID NOT NULL,
    order_id UUID NOT NULL,

    quantity INTEGER NOT NULL,

    product_name TEXT NOT NULL,
    unit_price NUMERIC(10,2) NOT NULL,

    CONSTRAINT fk_order_items_product
        FOREIGN KEY (product_id)
        REFERENCES products(product_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id)
        REFERENCES orders(order_id)
        ON DELETE CASCADE,

    CONSTRAINT chk_order_items_quantity
        CHECK (quantity > 0),

    CONSTRAINT chk_order_items_unit_price
        CHECK (unit_price >= 0)
);