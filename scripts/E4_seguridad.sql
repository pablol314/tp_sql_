-- =============================================================
-- TFI - Bases de Datos I (Etapa 4: Seguridad)
-- Motor objetivo: MySQL 8.0+
-- Ejecutar después de schema.sql y sample_data.sql
-- =============================================================

USE producto_barras;

-- Vistas con información filtrada para el usuario de aplicación
CREATE OR REPLACE VIEW vw_producto_publico AS
SELECT
  p.id,
  p.nombre,
  c.nombre AS categoria,
  m.nombre AS marca,
  p.precio,
  p.stock,
  p.fecha_alta,
  cb.gtin13,
  cb.tipo AS tipo_codigo
FROM producto p
JOIN categoria c ON c.id = p.categoria_id
JOIN marca     m ON m.id = p.marca_id
LEFT JOIN codigo_barras cb ON cb.producto_id = p.id
WHERE p.eliminado = 0;

CREATE OR REPLACE VIEW vw_inventario_resumido AS
SELECT
  c.nombre AS categoria,
  m.nombre AS marca,
  COUNT(*) AS cantidad_productos,
  SUM(p.stock) AS stock_total
FROM producto p
JOIN categoria c ON c.id = p.categoria_id
JOIN marca     m ON m.id = p.marca_id
WHERE p.eliminado = 0
GROUP BY c.nombre, m.nombre
ORDER BY c.nombre, m.nombre;

CREATE OR REPLACE VIEW vw_busqueda AS
SELECT
  p.id,
  p.nombre,
  c.nombre AS categoria,
  m.nombre AS marca,
  cb.gtin13
FROM producto p
JOIN categoria c ON c.id = p.categoria_id
JOIN marca     m ON m.id = p.marca_id
LEFT JOIN codigo_barras cb ON cb.producto_id = p.id
WHERE p.eliminado = 0;

-- Usuario de aplicación con permisos mínimos de lectura
CREATE USER IF NOT EXISTS 'app_user'@'localhost' IDENTIFIED BY 'TPIntegrador2025!';
ALTER USER 'app_user'@'localhost' IDENTIFIED BY 'TPIntegrador2025!';

GRANT SELECT ON producto_barras.vw_producto_publico TO 'app_user'@'localhost';
GRANT SELECT ON producto_barras.vw_inventario_resumido TO 'app_user'@'localhost';
GRANT SELECT ON producto_barras.vw_busqueda TO 'app_user'@'localhost';

FLUSH PRIVILEGES;

-- Validaciones rápidas
SHOW GRANTS FOR 'app_user'@'localhost';
SELECT * FROM vw_producto_publico LIMIT 10;
SELECT * FROM vw_inventario_resumido LIMIT 10;
