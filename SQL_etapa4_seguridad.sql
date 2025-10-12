USE producto_barras;

DROP USER IF EXISTS 'app_user'@'localhost';

CREATE USER 'app_user'@'localhost' IDENTIFIED BY 'TPIntegrador2025!';

GRANT SELECT ON producto_barras.vw_producto_publico TO 'app_user'@'localhost';
GRANT SELECT ON producto_barras.vw_inventario_resumido TO 'app_user'@'localhost';
GRANT SELECT ON producto_barras.vw_busqueda TO 'app_user'@'localhost';

FLUSH PRIVILEGES;

SHOW GRANTS FOR 'app_user'@'localhost';

DROP VIEW IF EXISTS vw_producto_publico;
CREATE VIEW vw_producto_publico AS
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
WHERE p.eliminado = 0;  -- Solo productos activos

DROP VIEW IF EXISTS vw_inventario_resumido;
CREATE VIEW vw_inventario_resumido AS
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

SELECT * FROM vw_producto_publico LIMIT 10;
SELECT * FROM vw_inventario_resumido LIMIT 10;

INSERT INTO producto (id, nombre, categoria_id, marca_id, precio, costo, stock)
VALUES (1, 'Producto con PK duplicada', 1, 1, 500.00, 300.00, 10);

