-- Datos iniciales para PostgreSQL (usar en TablePlus o perfiles postgres)
INSERT INTO equipos (id, nombre) VALUES (10, 'Proyector EPSON Aula Magna') ON CONFLICT (id) DO NOTHING;
INSERT INTO equipos (id, nombre) VALUES (11, 'Notebook HP Laboratorio 1') ON CONFLICT (id) DO NOTHING;
INSERT INTO equipos (id, nombre) VALUES (12, 'Carro de Tablets #1') ON CONFLICT (id) DO NOTHING;

INSERT INTO docentes (id, nombre) VALUES (1, 'Prof. Juan Pérez') ON CONFLICT (id) DO NOTHING;
INSERT INTO docentes (id, nombre) VALUES (2, 'Prof. María González') ON CONFLICT (id) DO NOTHING;
