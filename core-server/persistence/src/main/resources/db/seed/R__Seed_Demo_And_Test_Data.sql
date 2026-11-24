TRUNCATE TABLE users CASCADE;

-- Seed Initial Super Admin
INSERT INTO platform_admins (id, name, username, phone, email, password_hash, role, status)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'Ali B',
    'kodach',
    '+233598479398',
    'kodach351@gmail.com',
    '$2a$12$XRjslOnt0JktQZUX56Hy7eFTroAlks2k5bvEOECThd4WZFnoTHn1i', --password123
    'PLATFORM_SUPER_ADMIN',
    'ACTIVE'
) ON CONFLICT DO NOTHING;


