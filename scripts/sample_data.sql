-- =============================================================
-- Datos determinísticos para "producto_barras"
-- Motor objetivo: MySQL 8.0+
-- Orden recomendado de ejecución:
--   1) scripts/schema.sql
--   2) scripts/sample_data.sql
-- =============================================================

USE producto_barras;

-- =============================================================
-- CATÁLOGOS: CATEGORÍA Y MARCA
-- =============================================================

INSERT INTO categoria (id, nombre, descripcion, eliminado)
VALUES
  (1, 'Alimentos', 'Productos alimenticios de consumo masivo', 0),
  (2, 'Bebidas',   'Bebidas frías y calientes', 0),
  (3, 'Higiene',   'Cuidado personal y limpieza', 0)
ON DUPLICATE KEY UPDATE
  nombre = VALUES(nombre),
  descripcion = VALUES(descripcion),
  eliminado = VALUES(eliminado);

INSERT INTO marca (id, nombre, eliminado)
VALUES
  (1, 'Genérica', 0),
  (2, 'Acme',     0),
  (3, 'Premium',  0)
ON DUPLICATE KEY UPDATE
  nombre = VALUES(nombre),
  eliminado = VALUES(eliminado);

-- =============================================================
-- PRODUCTOS (IDs FIJOS PARA ASEGURAR RELACIÓN 1→1)
-- =============================================================

INSERT INTO producto (id, nombre, categoria_id, marca_id, precio, costo, stock, fecha_alta, eliminado)
VALUES
  (1, 'Galletas de Agua 100g',       1, 1, 1200.00, 800.00,  50, '2024-09-01', 0),
  (2, 'Jugo de Naranja 1L',         2, 2,  950.00, 600.00, 120, '2024-08-15', 0),
  (3, 'Jabón Neutro 90g',           3, 3,  900.00, 500.00,  80, '2024-07-20', 0),
  (4, 'Café Molido Premium 250g',   2, 3, 3800.00, 2500.00,  40, '2024-06-10', 0)
ON DUPLICATE KEY UPDATE
  nombre = VALUES(nombre),
  categoria_id = VALUES(categoria_id),
  marca_id = VALUES(marca_id),
  precio = VALUES(precio),
  costo = VALUES(costo),
  stock = VALUES(stock),
  fecha_alta = VALUES(fecha_alta),
  eliminado = VALUES(eliminado);

-- =============================================================
-- CÓDIGOS DE BARRAS (1→1 CON PRODUCTO)
-- =============================================================

INSERT INTO codigo_barras (producto_id, gtin13, tipo, activo)
VALUES
  (1, '7791234567890', 'EAN13', 1),
  (2, '7791234567891', 'EAN13', 1),
  (3, '7791234567892', 'EAN13', 1),
  (4, '7791234567893', 'EAN13', 1)
ON DUPLICATE KEY UPDATE
  gtin13 = VALUES(gtin13),
  tipo   = VALUES(tipo),
  activo = VALUES(activo);

-- =============================================================
-- FIN DE LOS DATOS DE MUESTRA
-- =============================================================
