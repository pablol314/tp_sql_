-- =============================================================
-- TFI - Bases de Datos I (Etapa 3: Consultas y Vistas)
-- Requisitos: 5 consultas complejas (3 JOIN, 1 GROUP BY/HAVING, 1 Subconsulta) y 1 vista.
-- =============================================================

USE producto_barras;

-- -------------------------------------------------------------
-- 1. CONSULTA COMPLEJA: JOIN (Inventario y Precios) [REQ 1/5 - Tipo: JOIN]
-- -------------------------------------------------------------
-- OBJETIVO: Muestra Producto, Categoría, Marca y Stock.
SELECT
    p.nombre AS Producto,
    c.nombre AS Categoria,
    m.nombre AS Marca,
    p.stock AS Stock
FROM producto p
JOIN categoria c ON c.id = p.categoria_id  -- JOIN 1
JOIN marca m ON m.id = p.marca_id          -- JOIN 2
WHERE p.eliminado = FALSE;


-- -------------------------------------------------------------
-- 2. CONSULTA COMPLEJA: JOIN (Relación 1:1) [REQ 2/5 - Tipo: JOIN]
-- -------------------------------------------------------------
-- OBJETIVO: Productos que tienen un Código de Barras ACTIVO. (Usa INNER JOIN 1:1)
SELECT
    p.nombre AS Producto,
    cb.gtin13 AS GTIN13
FROM producto p
INNER JOIN codigo_barras cb ON cb.producto_id = p.id  -- JOIN 3 (INNER fuerza la existencia del CB)
WHERE cb.activo = TRUE;


-- -------------------------------------------------------------
-- 3. CONSULTA COMPLEJA: GROUP BY y HAVING [REQ 3/5 - Tipo: GROUP BY/HAVING]
-- -------------------------------------------------------------
-- OBJETIVO: Marcas con margen de ganancia promedio superior a 300 y con más de 5 productos.
SELECT
    m.nombre AS Marca,
    COUNT(p.id) AS Total_Productos,
    ROUND(AVG(p.precio - p.costo), 2) AS Margen_Promedio
FROM producto p
JOIN marca m ON m.id = p.marca_id
GROUP BY m.nombre
-- Filtra grupos: Margen > 300 Y (AND) Total de productos > 5
HAVING Margen_Promedio > 30 AND COUNT(p.id) > 5
ORDER BY Margen_Promedio DESC;


-- -------------------------------------------------------------
-- 4. CONSULTA COMPLEJA: SUBCOSULTA [REQ 4/5 - Tipo: Subconsulta]
-- -------------------------------------------------------------
-- OBJETIVO: Productos con un precio mayor al promedio de su propia categoría.
SELECT
    p.nombre AS Producto,
    p.precio,
    c.nombre AS Categoria
FROM producto p
JOIN categoria c ON c.id = p.categoria_id
WHERE p.precio > (
    -- Subconsulta que calcula el AVG para la categoría actual
    SELECT AVG(precio)
    FROM producto
    WHERE categoria_id = p.categoria_id
)
ORDER BY c.nombre, p.precio DESC;


-- -------------------------------------------------------------
-- 5. CONSULTA COMPLEJA: JOIN [REQ 5/5 - Tipo: JOIN]
-- -------------------------------------------------------------
-- OBJETIVO: Reporte de antigüedad del inventario (días desde la fecha de alta).
SELECT
    p.nombre AS Producto,
    p.fecha_alta,
    c.nombre AS Categoria,
    DATEDIFF(CURRENT_DATE(), p.fecha_alta) AS Dias_Antiguedad
FROM producto p
JOIN categoria c ON c.id = p.categoria_id  -- JOIN 4
ORDER BY Dias_Antiguedad DESC;


-- =============================================================
-- VISTA REQUERIDA (Al menos 1 vista útil)
-- =============================================================
-- OBJETIVO: Vista para control de inventario: Stock total y valor de reposición por Categoría.
CREATE OR REPLACE VIEW vw_stock_por_categoria AS
SELECT
    c.nombre AS Categoria,
    SUM(p.stock) AS Stock_Total,
    COUNT(p.id) AS Items_Distintos,
    ROUND(SUM(p.costo * p.stock), 2) AS Valor_Reposicion
FROM categoria c
JOIN producto p ON p.categoria_id = c.id
WHERE p.eliminado = FALSE
GROUP BY c.nombre
ORDER BY Stock_Total DESC;

-- -------------------------------------------------------------
-- Prueba:
-- SELECT * FROM vw_stock_por_categoria;
