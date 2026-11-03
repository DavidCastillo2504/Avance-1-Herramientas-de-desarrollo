CREATE TABLE reservations (
    id UUID PRIMARY KEY,
    customer_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    reservation_date DATE NOT NULL,
    reservation_time TIME NOT NULL,
    party_size INTEGER NOT NULL CHECK (party_size BETWEEN 1 AND 20),
    notes VARCHAR(500),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_reservations_schedule ON reservations(reservation_date, reservation_time);
CREATE INDEX idx_reservations_created_at ON reservations(created_at DESC);
