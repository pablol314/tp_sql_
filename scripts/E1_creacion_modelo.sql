-- =============================================================
-- TFI - Bases de Datos I (Etapa 1: Modelado y Constraints)
-- Dominio: Producto ↔ Código de Barras (1→1) con catálogos Marca/Categoría
-- Motor objetivo: MySQL 8.0+
-- Orden recomendado de ejecución:
--   1) scripts/schema.sql
--   2) scripts/sample_data.sql
-- =============================================================

-- Entrypoint de la etapa 1: crea el esquema y carga datos base
SOURCE scripts/schema.sql;
SOURCE scripts/sample_data.sql;

-- Validaciones rápidas (opcional)
USE producto_barras;

SELECT 'categorias' AS entidad, COUNT(*) AS cantidad FROM categoria
UNION ALL
SELECT 'marcas', COUNT(*) FROM marca
UNION ALL
SELECT 'productos', COUNT(*) FROM producto
UNION ALL
SELECT 'codigos_barras', COUNT(*) FROM codigo_barras;

-- Verifica relación 1→1 (no deberían existir productos sin código)
SELECT COUNT(*) AS productos_sin_codigo
FROM producto p
LEFT JOIN codigo_barras cb ON cb.producto_id = p.id
WHERE cb.producto_id IS NULL;
