/* ============================================================
   ETAPA 2 – Carga masiva + índice + mediciones (SQL puro)
   Requiere: esquema Etapa 1 creado (categoria, marca, producto, codigo_barras)
   ============================================================ */
USE producto_barras;

-- =========================
-- 0) PARÁMETROS
-- =========================
SET @TARGET_ROWS := 200000;      -- Sugerido para mediciones (200k–500k)
SET @FECHA_BASE  := DATE('2024-01-01');
SET @DIAS_RANGO  := 650;         -- ~2024–2025

-- =========================
-- 1) SEEDS / CATÁLOGOS (idempotentes)
-- =========================
INSERT IGNORE INTO categoria (nombre, descripcion) VALUES
 ('Bebidas','Líquidos y refrescos'),
 ('Lácteos','Leche, yogures, quesos'),
 ('Higiene','Cuidado personal'),
 ('Pastas','Secas y frescas'),
 ('Carnes','Vacuna, cerdo y pollo'),
 ('Frutas','Frescas y secas'),
 ('Verduras','Hortalizas y vegetales'),
 ('Pescados','De mar y río'),
 ('Panificación','Pan, facturas y galletas'),
 ('Limpieza','Hogar y multisuperficie'),
 ('Perfumería','Fragancias y cosmética'),
 ('Electrodomésticos','Pequeños y grandes electro');

INSERT IGNORE INTO marca (nombre) VALUES
 ('Alborclean'),('Albormont'),('Alborplus'),('Albortron'),('Alborvia'),
 ('Altairdia'),('Andechem'),('Andepro'),('Anderax'),('Anderline'),
 ('Borealis'),('Borealift'),('Capella'),('Cedron'),('Cetrax'),
 ('Deltora'),('Domicare'),('Eclipse'),('Estelar'),('Futura'),
 ('Galatea'),('Helix'),('Iberplast'),('Juno'),('Krypton'),
 ('Lumina'),('Lunaris'),('Magnus'),('Nebula'),('Nix'),
 ('Novaclean'),('Optimus'),('Orion'),('Pangea'),('Polaris'),
 ('Primavera'),('Quantum'),('Quimex'),('Radex'),('Riviera'),
 ('Saturn'),('Scala'),('Solaria'),('Stellar'),('Suprema'),
 ('Terralux'),('Titan'),('Trinium'),('Ultraclean'),('Umbra'),
 ('Umbrella'),('Urbanix'),('Valquiria'),('Vanguard'),('Vespa'),
 ('Vesta'),('Vinum'),('Volta'),('Vórtex'),('Zafira'),
 ('Zenix'),('Zion'),('Zodiac'),('Zuma'),('Zurich'),
 ('Atena'),('Atlas'),('Aurora'),('Avalon'),('Axion'),
 ('Báltico'),('Banzai'),('Bastión'),('Becquer'),('Bélgica'),
 ('Calipso'),('Canopus'),('Cefiro'),('Cerbero'),('Ceres'),
 ('Dacota'),('Dagor'),('Dakota'),('Damasco'),('Darko'),
 ('Egeo'),('Eirene'),('Elea'),('Elegans'),('Elipsis'),
 ('Fenrir'),('Fidelius'),('Fulgor'),('Gaia'),('Gala'),
 ('Hera'),('Hespérides'),('Hidra'),('Hipnos'),('Horus');

-- =========================
-- 2) AUX: dígitos y secuencias (0..9 -> 0..9999/0..99999/0..999999)
-- =========================
CREATE TABLE IF NOT EXISTS tmp_digit (
  d TINYINT UNSIGNED PRIMARY KEY
) ENGINE=Memory;
INSERT IGNORE INTO tmp_digit (d) VALUES (0),(1),(2),(3),(4),(5),(6),(7),(8),(9);

CREATE OR REPLACE VIEW v_seq_0_9999 AS
SELECT a.d + b.d*10 + c.d*100 + d.d*1000 AS n
FROM tmp_digit a
CROSS JOIN tmp_digit b
CROSS JOIN tmp_digit c
CROSS JOIN tmp_digit d;

CREATE OR REPLACE VIEW v_seq_0_99999 AS
SELECT e.d*10000 + s.n AS n
FROM tmp_digit e
CROSS JOIN v_seq_0_9999 s;

-- Para 500k+ filas:
CREATE OR REPLACE VIEW v_seq_0_999999 AS
SELECT f.d*100000 + s.n AS n
FROM tmp_digit f
CROSS JOIN v_seq_0_99999 s;

-- =========================
-- 3) AUX: nombres base para productos
-- =========================
DROP TABLE IF EXISTS tmp_nombres;
CREATE TABLE tmp_nombres (
  id   INT UNSIGNED NOT NULL AUTO_INCREMENT,
  base VARCHAR(120) NOT NULL,
  PRIMARY KEY (id)
) ENGINE=Memory;

INSERT INTO tmp_nombres (base) VALUES
 ('Galletas de agua'), ('Galletas dulces'), ('Yerba mate'), ('Azúcar'),
 ('Arroz largo fino'), ('Harina 0000'), ('Leche entera 1L'), ('Leche descremada 1L'),
 ('Yogur vainilla 180g'), ('Queso cremoso 300g'), ('Jabón de tocador'),
 ('Shampoo neutro 400ml'), ('Acondicionador 400ml'), ('Detergente 500ml'),
 ('Lavandina 1L'), ('Desodorante ambiente'), ('Fideos spaghetti 500g'),
 ('Fideos moños 500g'), ('Salsa de tomate 340g'), ('Gaseosa cola 2L'),
 ('Agua mineral 2L'), ('Aceite girasol 900ml'), ('Café molido 500g'),
 ('Té saquitos x25'), ('Pan lactal'), ('Galletitas saladas'),
 ('Mermelada durazno 454g'), ('Manteca 200g'), ('Dulce de leche 400g'),
 ('Jabón en polvo 800g'), ('Limpiador multiuso'), ('Escoba de nylon'),
 ('Papel higiénico x4'), ('Toallas de papel x2'), ('Desodorante corporal'),
 ('Queso rallado 40g'), ('Helado vainilla 1kg'), ('Arvejas en lata 350g'),
 ('Atún en lata 170g'), ('Mayonesa 250g');

-- =========================
-- 4) PARÁMETROS DINÁMICOS
-- =========================
SET @CATS := (SELECT COUNT(*) FROM categoria);
SET @MKS  := (SELECT COUNT(*) FROM marca);
SET @NOMS := (SELECT COUNT(*) FROM tmp_nombres);

-- Desfase para ejecuciones repetidas (evita choques de nombre):
SET @RUN_BASE := COALESCE((SELECT MAX(id) FROM producto), 0);

-- =========================
-- 5) SECUENCIA OBJETIVO @TARGET_ROWS
-- =========================
DROP TEMPORARY TABLE IF EXISTS tmp_seq;
CREATE TEMPORARY TABLE tmp_seq (n INT PRIMARY KEY) ENGINE=Memory;

-- Usa la vista adecuada según el volumen:
-- 0..9,999   -> v_seq_0_9999
-- 0..99,999  -> v_seq_0_99999
-- 0..999,999 -> v_seq_0_999999
INSERT INTO tmp_seq (n)
SELECT n FROM v_seq_0_999999 WHERE n < @TARGET_ROWS;

SELECT COUNT(*) AS secuencias_generadas FROM tmp_seq;

-- =========================
-- 6) INSERCIÓN MASIVA EN producto (idempotente)
-- =========================
INSERT INTO producto (nombre, categoria_id, marca_id, precio, costo, stock, fecha_alta)
SELECT
  CONCAT(nn.base, ' ', mk.nombre, ' ', LPAD(ts.n + @RUN_BASE, 6, '0')) AS nombre,
  ((ts.n + @RUN_BASE) MOD @CATS) + 1                                  AS categoria_id,
  ((ts.n + @RUN_BASE) MOD @MKS)  + 1                                   AS marca_id,
  ROUND( 50 + (RAND(ts.n) * 950), 2 )                                  AS precio,
  ROUND( (0.50 + (RAND(ts.n+7) * 0.25)) * (50 + (RAND(ts.n) * 950)), 2 ) AS costo,
  FLOOR(RAND(ts.n+3) * 500)                                            AS stock,
  DATE_ADD(@FECHA_BASE, INTERVAL FLOOR(RAND(ts.n+11) * @DIAS_RANGO) DAY) AS fecha_alta
FROM tmp_seq ts
JOIN tmp_nombres nn ON ((ts.n MOD @NOMS) + 1) = nn.id
JOIN marca mk ON mk.id = ((ts.n MOD @MKS) + 1)
WHERE NOT EXISTS (  -- evita duplicar si el nombre ya quedó cargado en corridas previas
  SELECT 1 FROM producto p2
  WHERE p2.nombre = CONCAT(nn.base, ' ', mk.nombre, ' ', LPAD(ts.n + @RUN_BASE, 6, '0'))
);

SELECT COUNT(*) AS productos_insertados_en_esta_corrida FROM producto
WHERE id > @RUN_BASE;

-- =========================
-- 7) INSERCIÓN 1→1 EN codigo_barras (idempotente)
--    gtin13 = '779' + LPAD(id, 10, '0')
-- =========================
INSERT INTO codigo_barras (producto_id, gtin13, tipo, activo)
SELECT p.id,
       CONCAT('779', LPAD(p.id, 10, '0')) AS gtin13,
       'EAN13' AS tipo,
       1 AS activo
FROM producto p
LEFT JOIN codigo_barras cb ON cb.producto_id = p.id
WHERE cb.producto_id IS NULL;

SELECT COUNT(*) AS codigos_barras_totales FROM codigo_barras;

/* =========================
   8) ÍNDICES (simple y visible)
   ========================= */

-- (A) Ver índices actuales
SHOW INDEX FROM producto;

-- (B) Crear índices (ejecutá UNA VEZ; si ya existen, comentá estas dos líneas)
-- ALTER TABLE producto ADD INDEX ix_prod_cat_precio (categoria_id, precio);
-- ALTER TABLE producto ADD INDEX ix_prod_marca (marca_id);
-- SHOW INDEX FROM producto;

/* =========================
   9) MEDICIÓN (simple y visible)
   ========================= */

-- Parámetros de prueba
SELECT categoria_id, 
       MIN(precio) AS precio_min, 
       MAX(precio) AS precio_max, 
       COUNT(*) AS cantidad
FROM producto
GROUP BY categoria_id
ORDER BY categoria_id;

SET @cat  := 2;
SET @pmin := 50;
SET @pmax := 1000; 

-- Asegurar “SIN índice” para la 1ª medición
DROP INDEX ix_prod_cat_precio ON producto;  -- comentar si no existe y da error

-- 1) Medición SIN índice
SET @t0 := NOW(6);
SELECT SQL_NO_CACHE COUNT(*) AS cant_sin_indice
FROM producto
WHERE categoria_id=@cat AND precio BETWEEN @pmin AND @pmax;
SET @t_sin := TIMESTAMPDIFF(MICROSECOND, @t0, NOW(6));

-- 2) Crear índice y medir CON índice
ALTER TABLE producto ADD INDEX ix_prod_cat_precio (categoria_id, precio);

-- (Opcional) Ver que usa el índice
EXPLAIN SELECT COUNT(*) FROM producto
WHERE categoria_id=@cat AND precio BETWEEN @pmin AND @pmax;

SET @t1 := NOW(6);
SELECT SQL_NO_CACHE COUNT(*) AS cant_con_indice
FROM producto
WHERE categoria_id=@cat AND precio BETWEEN @pmin AND @pmax;
SET @t_con := TIMESTAMPDIFF(MICROSECOND, @t1, NOW(6));

-- 3) Resultado final
SELECT 
  @t_sin AS microseg_sin_indice,
  @t_con AS microseg_con_indice,
  ROUND((@t_sin-@t_con)/@t_sin*100,2) AS mejora_pct;
