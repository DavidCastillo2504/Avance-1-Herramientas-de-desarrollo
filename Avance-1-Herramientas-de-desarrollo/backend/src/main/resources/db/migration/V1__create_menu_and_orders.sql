CREATE TABLE menu_items (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    category VARCHAR(60) NOT NULL,
    price NUMERIC(10, 2) NOT NULL CHECK (price > 0),
    emoji VARCHAR(16) NOT NULL,
    description VARCHAR(300) NOT NULL,
    available BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE customer_orders (
    id UUID PRIMARY KEY,
    customer_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    order_type VARCHAR(20) NOT NULL CHECK (order_type IN ('TAKEAWAY', 'DINE_IN')),
    notes VARCHAR(500),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'READY')),
    total NUMERIC(10, 2) NOT NULL CHECK (total > 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES customer_orders(id) ON DELETE CASCADE,
    menu_item_id BIGINT NOT NULL REFERENCES menu_items(id),
    item_name VARCHAR(120) NOT NULL,
    unit_price NUMERIC(10, 2) NOT NULL CHECK (unit_price > 0),
    quantity INTEGER NOT NULL CHECK (quantity BETWEEN 1 AND 20),
    subtotal NUMERIC(10, 2) NOT NULL CHECK (subtotal > 0)
);

CREATE INDEX idx_orders_created_at ON customer_orders(created_at DESC);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_menu_items_category ON menu_items(category);

INSERT INTO menu_items (name, category, price, emoji, description, available) VALUES
('Seco de cabrito', 'Criollos', 28.00, '🍛', 'Con frejoles y arroz blanco.', TRUE),
('Arroz con pato', 'Criollos', 26.00, '🍚', 'Arroz verde, pato tierno y salsa criolla.', TRUE),
('Ceviche clásico', 'Marinos', 30.00, '🐟', 'Pescado fresco, limón, cebolla, camote y choclo.', TRUE),
('Sudado de pescado', 'Marinos', 27.00, '🍲', 'Pescado en caldo criollo con tomate y yuca.', FALSE),
('Lomo saltado', 'Criollos', 29.00, '🥩', 'Carne salteada, papas fritas y arroz.', TRUE),
('Ají de gallina', 'Criollos', 23.00, '🍗', 'Crema de ají amarillo, pollo, papa y arroz.', TRUE),
('Chicha morada', 'Bebidas', 7.00, '🥤', 'Preparación tradicional de maíz morado.', TRUE),
('Maracuyá', 'Bebidas', 8.00, '🧃', 'Refresco natural de maracuyá.', TRUE),
('Mazamorra morada', 'Postres', 9.00, '🍮', 'Postre tradicional con frutas y especias.', TRUE);
