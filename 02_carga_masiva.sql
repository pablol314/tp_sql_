/* ============================================================
   ETAPA 2 – Carga masiva + índice + mediciones (SQL puro)
   Requiere: esquema y tablas de Etapa 1 ya creadas
   BD: producto_barras  (tablas: categoria, marca, producto, codigo_barras)
   ============================================================ */

USE producto_barras;

/* =========================
   0) PARÁMETROS
   ========================= */
SET @TARGET_ROWS := 10000;  -- Cambiar a 50000 o 100000 si querés más volumen.

/* =========================
   1) SEEDS / CATÁLOGOS (idempotentes)
   ========================= */
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

/* =========================
   2) SECUENCIAS (0..9 → 0..9999/99999) SIN CTEs
   ========================= */
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

/* =========================
   4) PARÁMETROS DINÁMICOS (conteos de catálogos)
   ========================= */
SET @CATS := (SELECT COUNT(*) FROM categoria);
SET @MKS  := (SELECT COUNT(*) FROM marca);
SET @NOMS := (SELECT COUNT(*) FROM tmp_nombres);

/* =========================
   5) SECUENCIA OBJETIVO @TARGET_ROWS
   ========================= */
DROP TEMPORARY TABLE IF EXISTS tmp_seq;
CREATE TEMPORARY TABLE tmp_seq (n INT PRIMARY KEY) ENGINE=Memory;

-- <= 10.000
INSERT INTO tmp_seq (n)
SELECT n FROM v_seq_0_9999
WHERE n < @TARGET_ROWS;

-- > 10.000 (hasta 100.000): descomentá estas 2 líneas y comentá el bloque anterior
-- INSERT INTO tmp_seq (n)
-- SELECT n FROM v_seq_0_99999 WHERE n < @TARGET_ROWS;

SELECT COUNT(*) AS secuencias_generadas FROM tmp_seq;

/* =========================
   6) INSERCIÓN MASIVA EN producto
   =========================
   - nombre único: base + marca + n (con cero padding)
   - categoría distribuida por módulo
   - precio >= costo garantizado
   - fecha aleatoria (2024–2025 aprox.)
*/
INSERT INTO producto (nombre, categoria_id, marca_id, precio, costo, stock, fecha_alta)
SELECT
  CONCAT(nn.base, ' ', mk.nombre, ' ', LPAD(ts.n, 5, '0'))                     AS nombre,
  ((ts.n MOD @CATS) + 1)                                                       AS categoria_id,
  ((ts.n MOD @MKS)  + 1)                                                       AS marca_id,
  ROUND( 50 + (RAND(ts.n) * 950), 2 )                                       AS precio,
  ROUND( (50+ (RAND(ts.n) * 950)) * (0.50 + (RAND(ts.n+7) * 0.25)), 2 )    AS costo,
  FLOOR(RAND(ts.n+3) * 500)                                                    AS stock,
  DATE_ADD(DATE('2024-01-01'), INTERVAL FLOOR(RAND(ts.n+11) * 650) DAY)        AS fecha_alta
FROM tmp_seq ts
JOIN tmp_nombres nn ON ((ts.n MOD @NOMS) + 1) = nn.id
JOIN marca mk ON mk.id = ((ts.n MOD @MKS) + 1);

SELECT COUNT(*) AS productos_insertados FROM producto;

/* =========================
   7) INSERCIÓN 1→1 EN codigo_barras (sin duplicar)
   =========================
   gtin13 = '779' + LPAD(id, 10, '0')  → 13 dígitos (cumple CHECK)
*/
INSERT INTO codigo_barras (producto_id, gtin13, tipo, activo)
SELECT p.id,
       CONCAT('779', LPAD(p.id, 10, '0')) AS gtin13,
       'EAN13' AS tipo,
       1 AS activo
FROM producto p
LEFT JOIN codigo_barras cb ON cb.producto_id = p.id
WHERE cb.producto_id IS NULL;

SELECT COUNT(*) AS codigos_barras_insertados FROM codigo_barras;

/* ============================================================
   8) MEDICIONES: sin índice compuesto vs con índice compuesto
   ============================================================ */
   
-- Parámetros de consulta de prueba
SET @cat  := 5;
SET @pmin := 3000.00;
SET @pmax := 6000.00;

-- Consulta objetivo
SET @q := CONCAT(
  "SELECT /* sin_idx */ COUNT(*) AS cant FROM producto ",
  "WHERE categoria_id = ", @cat,
  " AND precio BETWEEN ", @pmin, " AND ", @pmax
);

-- SIN ÍNDICE COMPUESTO
SET @t0 := NOW(6);
PREPARE s FROM @q; EXECUTE s; DEALLOCATE PREPARE s;
SELECT 'sin_indice' AS escenario, TIMESTAMPDIFF(MICROSECOND, @t0, NOW(6)) AS microseg;

-- Crear índice compuesto nuevo para esta etapa
CREATE INDEX ix_prod_categoria_precio ON producto (categoria_id, precio);

-- CON ÍNDICE COMPUESTO
SET @t1 := NOW(6);
PREPARE s2 FROM @q; EXECUTE s2; DEALLOCATE PREPARE s2;
SELECT 'con_indice' AS escenario, TIMESTAMPDIFF(MICROSECOND, @t1, NOW(6)) AS microseg;

-- (Opcional) Plan de ejecución
EXPLAIN SELECT COUNT(*) FROM producto
WHERE categoria_id = @cat AND precio BETWEEN @pmin AND @pmax;
