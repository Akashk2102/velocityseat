-- ==============================================================================
-- VelocitySeat Distributed Event Booking Platform
-- Seed Initial Demo Data (Venues, Seats, Published Events, Demo Users)
-- ==============================================================================

-- 1. Seed Demo Users (Password: password123)
-- BCrypt Hash: $2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW
INSERT INTO users (id, email, password, full_name, phone_number, role, is_active, created_at, updated_at)
VALUES 
    (1, 'admin@velocityseat.com', '$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW', 'System Administrator', '+14155550100', 'ROLE_ADMIN', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'customer@velocityseat.com', '$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW', 'Alex Mercer', '+14155550101', 'ROLE_CUSTOMER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (email) DO NOTHING;

-- 2. Seed Demo Venue
INSERT INTO venues (id, name, address, city, state, country, postal_code, total_capacity, created_at, updated_at)
VALUES 
    (1, 'Velocity Grand Arena', '100 Innovation Way', 'San Francisco', 'CA', 'USA', '94107', 40, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 3. Seed Physical Seats (4 rows A-D x 10 seats each = 40 seats)
-- Row A & B: Floor VIP (20 seats)
-- Row C & D: Lower Bowl Standard (20 seats)
INSERT INTO seats (venue_id, section, row_number, seat_number, seat_type)
VALUES
    (1, 'Floor VIP', 'A', '1', 'VIP'),
    (1, 'Floor VIP', 'A', '2', 'VIP'),
    (1, 'Floor VIP', 'A', '3', 'VIP'),
    (1, 'Floor VIP', 'A', '4', 'VIP'),
    (1, 'Floor VIP', 'A', '5', 'VIP'),
    (1, 'Floor VIP', 'A', '6', 'VIP'),
    (1, 'Floor VIP', 'A', '7', 'VIP'),
    (1, 'Floor VIP', 'A', '8', 'VIP'),
    (1, 'Floor VIP', 'A', '9', 'VIP'),
    (1, 'Floor VIP', 'A', '10', 'VIP'),
    (1, 'Floor VIP', 'B', '1', 'VIP'),
    (1, 'Floor VIP', 'B', '2', 'VIP'),
    (1, 'Floor VIP', 'B', '3', 'VIP'),
    (1, 'Floor VIP', 'B', '4', 'VIP'),
    (1, 'Floor VIP', 'B', '5', 'VIP'),
    (1, 'Floor VIP', 'B', '6', 'VIP'),
    (1, 'Floor VIP', 'B', '7', 'VIP'),
    (1, 'Floor VIP', 'B', '8', 'VIP'),
    (1, 'Floor VIP', 'B', '9', 'VIP'),
    (1, 'Floor VIP', 'B', '10', 'VIP'),
    (1, 'Lower Bowl', 'C', '1', 'STANDARD'),
    (1, 'Lower Bowl', 'C', '2', 'STANDARD'),
    (1, 'Lower Bowl', 'C', '3', 'STANDARD'),
    (1, 'Lower Bowl', 'C', '4', 'STANDARD'),
    (1, 'Lower Bowl', 'C', '5', 'STANDARD'),
    (1, 'Lower Bowl', 'C', '6', 'STANDARD'),
    (1, 'Lower Bowl', 'C', '7', 'STANDARD'),
    (1, 'Lower Bowl', 'C', '8', 'STANDARD'),
    (1, 'Lower Bowl', 'C', '9', 'STANDARD'),
    (1, 'Lower Bowl', 'C', '10', 'STANDARD'),
    (1, 'Lower Bowl', 'D', '1', 'STANDARD'),
    (1, 'Lower Bowl', 'D', '2', 'STANDARD'),
    (1, 'Lower Bowl', 'D', '3', 'STANDARD'),
    (1, 'Lower Bowl', 'D', '4', 'STANDARD'),
    (1, 'Lower Bowl', 'D', '5', 'STANDARD'),
    (1, 'Lower Bowl', 'D', '6', 'STANDARD'),
    (1, 'Lower Bowl', 'D', '7', 'STANDARD'),
    (1, 'Lower Bowl', 'D', '8', 'STANDARD'),
    (1, 'Lower Bowl', 'D', '9', 'STANDARD'),
    (1, 'Lower Bowl', 'D', '10', 'STANDARD')
ON CONFLICT (venue_id, section, row_number, seat_number) DO NOTHING;

-- 4. Seed Live Published Events
INSERT INTO events (id, venue_id, title, description, category, start_time, end_time, status, created_at, updated_at)
VALUES
    (1, 1, 'Coldplay: Music of the Spheres World Tour', 'Experience Coldplay live in concert with an immersive visual and auditory spectacle, complete with LED wristbands and laser lights.', 'CONCERT', CURRENT_TIMESTAMP + INTERVAL '14 days', CURRENT_TIMESTAMP + INTERVAL '14 days 3 hours', 'PUBLISHED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 1, 'UEFA Champions League Final 2026', 'The biggest club football showdown in Europe broadcast live on giant stadium screens with full arena surround sound.', 'SPORTS', CURRENT_TIMESTAMP + INTERVAL '30 days', CURRENT_TIMESTAMP + INTERVAL '30 days 4 hours', 'PUBLISHED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 5. Seed Event Seat Inventory for Event 1 (Coldplay)
INSERT INTO event_seats (event_id, seat_id, price, status, version, created_at, updated_at)
SELECT 1, s.id, CASE WHEN s.seat_type = 'VIP' THEN 180.00 ELSE 65.00 END, 'AVAILABLE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM seats s WHERE s.venue_id = 1
ON CONFLICT (event_id, seat_id) DO NOTHING;

-- 6. Seed Event Seat Inventory for Event 2 (Champions League Final)
INSERT INTO event_seats (event_id, seat_id, price, status, version, created_at, updated_at)
SELECT 2, s.id, CASE WHEN s.seat_type = 'VIP' THEN 250.00 ELSE 95.00 END, 'AVAILABLE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM seats s WHERE s.venue_id = 1
ON CONFLICT (event_id, seat_id) DO NOTHING;

-- 7. Reset sequences to prevent key collisions on dynamic insertions
SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id), 1) FROM users));
SELECT setval('venues_id_seq', (SELECT COALESCE(MAX(id), 1) FROM venues));
SELECT setval('seats_id_seq', (SELECT COALESCE(MAX(id), 1) FROM seats));
SELECT setval('events_id_seq', (SELECT COALESCE(MAX(id), 1) FROM events));
SELECT setval('event_seats_id_seq', (SELECT COALESCE(MAX(id), 1) FROM event_seats));
