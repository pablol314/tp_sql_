-- VISTAS DE PRUEBA / INSPECCIÓN (MySQL 8.0+)
-- Para validar rápidamente que la carga masiva quedó bien
-- Requiere ejecutar previamente schema.sql y sample_data.sql
-- =============================================================

USE producto_barras;

-- 1) Vista “full” con joins clave (producto + marca + categoría + código)
CREATE OR REPLACE VIEW vw_producto_full AS
SELECT
  p.id,
  p.nombre,
  p.descripcion,
  p.stock,
  p.precio,
  p.costo,
  (p.precio - p.costo)                 AS margen_abs,
  ROUND(1000 * (p.precio - p.costo) / NULLIF(p.precio,0), 2) AS margen_pct,
  p.fecha_alta,
  c.nombre AS categoria,
  m.nombre AS marca,
  cb.gtin13,
  cb.tipo  AS tipo_codigo,
  cb.activo AS cb_activo
FROM producto p
JOIN categoria c ON c.id = p.categoria_id
JOIN marca     m ON m.id = p.marca_id
LEFT JOIN codigo_barras cb ON cb.producto_id = p.id;

-- 2) Resumen de distribución por categoría y marca
CREATE OR REPLACE VIEW vw_resumen_cat_marca AS
SELECT
  c.nombre AS categoria,
  m.nombre AS marca,
  COUNT(*) AS cantidad,
  SUM(p.stock) AS stock_total,
  ROUND(AVG(p.precio), 2) AS precio_prom,
  ROUND(AVG(p.costo),  2) AS costo_prom
FROM producto p
JOIN categoria c ON c.id = p.categoria_id
JOIN marca     m ON m.id = p.marca_id
GROUP BY c.nombre, m.nombre
ORDER BY c.nombre, m.nombre;

-- 3) KPIs de inventario
CREATE OR REPLACE VIEW vw_kpis_inventario AS
SELECT
  COUNT(*)                         AS productos,
  SUM(stock)                       AS stock_total,
  ROUND(AVG(precio), 2)            AS precio_promedio,
  ROUND(AVG(costo), 2)             AS costo_promedio,
  ROUND(SUM(precio * stock), 2)    AS valor_reposicion_aprox
FROM producto;

-- 4) Control de integridad: productos sin código de barras (debe ser 0)
CREATE OR REPLACE VIEW vw_sin_codigo_barras AS
SELECT p.id, p.nombre, p.categoria_id, p.marca_id
FROM producto p
LEFT JOIN codigo_barras cb ON cb.producto_id = p.id
WHERE cb.producto_id IS NULL;

-- 5) Top por stock (para inspección rápida)
CREATE OR REPLACE VIEW vw_top_stock AS
SELECT id, nombre, stock, precio, costo
FROM producto
ORDER BY stock DESC
LIMIT 100;

-- 6) Margen negativo (debe quedar vacío por CHECK; útil para test)
CREATE OR REPLACE VIEW vw_margen_negativo AS
SELECT id, nombre, precio, costo, (precio - costo) AS margen
FROM producto
WHERE precio < costo;

-- 7) Bandas de precio (bucketización simple)
CREATE OR REPLACE VIEW vw_precio_bandas AS
SELECT
  CASE
    WHEN precio < 200  THEN '< 200'
    WHEN precio < 500  THEN '200 - 499'
    WHEN precio < 800  THEN '500 - 799'
    WHEN precio < 1200 THEN '800 - 1199'
    ELSE '>= 1200'
  END AS banda_precio,
  COUNT(*) AS cantidad,
  ROUND(AVG(precio),2) AS precio_prom
FROM producto
GROUP BY banda_precio
ORDER BY MIN(precio);

-- 8) Cargados recientemente (últimos 60 días de la fecha_alta)
CREATE OR REPLACE VIEW vw_recientes AS
SELECT id, nombre, fecha_alta, precio, costo, stock
FROM producto
WHERE fecha_alta >= DATE_SUB(CURRENT_DATE, INTERVAL 60 DAY)
ORDER BY fecha_alta DESC, id DESC;

-- 9) Vista auxiliar para búsquedas (usa los campos más consultados)
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
LEFT JOIN codigo_barras cb ON cb.producto_id = p.id;

-- =============================================================
-- CONSULTAS DE PRUEBA RÁPIDAS
-- =============================================================

-- Cantidades básicas
SELECT * FROM vw_producto_full;
SELECT * FROM vw_resumen_cat_marca LIMIT 50;
SELECT * FROM vw_top_stock;
SELECT * FROM vw_precio_bandas;
SELECT COUNT(*) AS productos_sin_cb FROM vw_sin_codigo_barras;

-- Búsqueda de ejemplo
-- SELECT * FROM vw_busqueda WHERE nombre LIKE 'Luna%' AND categoria='Bebidas' LIMIT 20;

-- Muestra extendida
-- SELECT * FROM vw_producto_full ORDER BY id DESC LIMIT 50;
