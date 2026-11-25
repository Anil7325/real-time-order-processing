CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY,
    sku VARCHAR(80) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    status VARCHAR(12) NOT NULL CHECK (status IN ('PENDING', 'PROCESSED')),
    published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS orders_to_publish ON orders (created_at) WHERE NOT published;
