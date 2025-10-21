-- =============================================================
-- TFI - Bases de Datos I (Etapa 5: Concurrencia y Transacciones)
-- Simulación de DEADLOCK y Niveles de Aislamiento
-- =============================================================

USE producto_barras;

-- =============================================================
-- PARTE 1: DEMOSTRACIÓN DE DEADLOCK (Ejecutar en DOS SESIONES)
-- =============================================================

-- Necesitas dos IDs de producto para probar (asumimos 1 y 2 por la Etapa 1)
SET @producto_A = 1;
SET @producto_B = 2;


-- -------------------------------------------------------------
-- SESIÓN 1: Ejecutar este bloque de código.
-- El script se detendrá en el 2do SELECT FOR UPDATE.
-- -------------------------------------------------------------
START TRANSACTION;

-- 1. Sesión 1 bloquea Producto A
SELECT * FROM producto WHERE id = @producto_A FOR UPDATE;
SELECT 'Sesión 1: Producto A BLOQUEADO. Esperando 5 segundos...' AS Mensaje;
DO SLEEP(5);

-- 2. Sesión 1 intenta bloquear Producto B (Aquí se produce el deadlock con Sesión 2)
SELECT * FROM producto WHERE id = @producto_B FOR UPDATE; 

COMMIT;
SELECT 'Sesión 1 FINALIZADA (COMMIT/ROLLBACK)' AS Resultado;


-- -------------------------------------------------------------
-- SESIÓN 2: Ejecutar este bloque de código.
-- INICIA después de que la Sesión 1 haya ejecutado el paso 1.
-- -------------------------------------------------------------
START TRANSACTION;

-- 1. Sesión 2 bloquea Producto B
SELECT * FROM producto WHERE id = @producto_B FOR UPDATE;
SELECT 'Sesión 2: Producto B BLOQUEADO. Esperando 5 segundos...' AS Mensaje;
DO SLEEP(5);

-- 2. Sesión 2 intenta bloquear Producto A
SELECT * FROM producto WHERE id = @producto_A FOR UPDATE;

COMMIT;
SELECT 'Sesión 2 FINALIZADA (COMMIT/ROLLBACK)' AS Resultado;


-- ==============================
-- PARTE 2: NIVELES DE AISLAMIENTO 
-- ===============================

-- 1. READ COMMITTED (Evita Lecturas Sucias, pero permite Lecturas No Repetibles)
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;
SELECT 'Nivel de Aislamiento: READ COMMITTED' AS Configuración;
SELECT @@transaction_isolation;

-- 2. REPEATABLE READ (Evita Lecturas Sucias y No Repetibles, pero puede tener Phantom Reads)
SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ;
SELECT 'Nivel de Aislamiento: REPEATABLE READ' AS Configuración;
SELECT @@transaction_isolation;

-- 3. SERIALIZABLE (Máximo Aislamiento, menor concurrencia)
SET SESSION TRANSACTION ISOLATION LEVEL SERIALIZABLE;
SELECT 'Nivel de Aislamiento: SERIALIZABLE' AS Configuración;
SELECT @@transaction_isolation;