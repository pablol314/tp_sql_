-- sample_data.sql: carga base para pruebas manuales de AppMenu
USE producto_barras;

INSERT INTO categoria (nombre, descripcion) VALUES
    ('Almacén', 'Alimentos no perecederos'),
    ('Bebidas', 'Bebidas alcohólicas y sin alcohol'),
    ('Limpieza', 'Artículos de higiene del hogar'),
    ('Perfumería', 'Cuidado personal y cosmética');

INSERT INTO marca (nombre, sitio_web) VALUES
    ('Luz del Valle', NULL),
    ('Patagonia Fresh', 'https://patagoniafresh.example.com'),
    ('EcoHome', 'https://ecohome.example.com'),
    ('Belle Femme', NULL);

INSERT INTO producto (nombre, categoria_id, marca_id, precio, costo, stock, fecha_alta)
VALUES
    ('Yerba Mate Selección 1kg', 1, 1, 4980.00, 3200.00, 150, '2024-01-15'),
    ('Agua Mineral 1.5L', 2, 2, 1200.00, 600.00, 480, '2024-02-03'),
    ('Limpiador Multiuso 900ml', 3, 3, 1890.00, 1120.00, 220, '2024-03-12'),
    ('Crema Facial Noche 50ml', 4, 4, 8250.00, 5200.00, 90, '2024-01-28');

INSERT INTO codigo_barras (producto_id, gtin13) VALUES
    (1, '7791234567890'),
    (2, '7790987654321'),
    (3, '7795678901234'),
    (4, '7794321098765');
