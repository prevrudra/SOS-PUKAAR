-- Verified police stations (Delhi NCR sample data for mock drill / SOS)
INSERT INTO police_stations (id, name, address, phone_e164, phone_verified, source, latitude, longitude, active)
VALUES
    ('a1000000-0000-4000-8000-000000000001', 'Connaught Place Police Station',
     'Connaught Place, New Delhi', '+911123456789', TRUE, 'Delhi Police Directory', 28.6315, 77.2167, TRUE),
    ('a1000000-0000-4000-8000-000000000002', 'Parliament Street Police Station',
     'Parliament Street, New Delhi', '+911123457890', TRUE, 'Delhi Police Directory', 28.6139, 77.2090, TRUE),
    ('a1000000-0000-4000-8000-000000000003', 'Mandir Marg Police Station',
     'Mandir Marg, New Delhi', '+911123458901', TRUE, 'Delhi Police Directory', 28.6350, 77.1950, TRUE);

INSERT INTO hospitals (id, name, address, phone_e164, latitude, longitude, active)
VALUES
    ('b2000000-0000-4000-8000-000000000001', 'AIIMS Hospital',
     'Ansari Nagar, New Delhi', '+911126588500', 28.5672, 77.2100, TRUE),
    ('b2000000-0000-4000-8000-000000000002', 'Safdarjung Hospital',
     'Ansari Nagar East, New Delhi', '+911126799999', 28.5689, 77.2065, TRUE),
    ('b2000000-0000-4000-8000-000000000003', 'Ram Manohar Lohia Hospital',
     'Baba Kharak Singh Marg, New Delhi', '+91112343040', 28.6250, 77.2105, TRUE);
