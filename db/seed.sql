INSERT INTO users (full_name, cpf, email, job_title, username, password, role, is_active)
VALUES ('Administrador', '00000000000', 'admin@sistema.com', 'Administrador', 'admin', 'admin123', 'ADMIN', 1)
ON CONFLICT(username) DO NOTHING;  -- SQLite UPSERT
