-- TFI - Bases de Datos I (Etapa 5: Concurrencia y Transacciones)
-- Simulación de DEADLOCK y Niveles de Aislamiento
-- Ejecutar después de schema.sql y sample_data.sql
-- =============================================================

USE producto_barras;

-- =============================================================
-- PARTE 1: DEMOSTRACIÓN DE DEADLOCK (Ejecutar en DOS SESIONES)
-- =============================================================

-- Necesitas dos IDs de producto para probar (asumimos 1 y 2 por la Etapa 1)
SET @producto_A = 1;
SET @producto_B = 2;
SET @incremento_stock = 5;  -- Utilizado en las pruebas de aislamiento


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

-- -------------------------------------------------------------
-- PRUEBA READ COMMITTED → Lectura no repetible
-- -------------------------------------------------------------
-- Preparación:
--   • Abrir dos sesiones (Sesión A y Sesión B) y ejecutar en cada una:
--       USE producto_barras;
--       SET @producto_A = 1;
--       SET @incremento_stock = 5;
--   • Aplicar la configuración anterior (SET SESSION ...) en cada sesión.
-- Objetivo: mostrar que la Sesión A ve el cambio confirmado por la Sesión B.

-- SESIÓN A ----------------------------------------------------
START TRANSACTION;
SELECT @stock_inicial := stock
  FROM producto
 WHERE id = @producto_A;
SELECT 'Sesión A - Lectura inicial' AS contexto, @stock_inicial AS stock_leido;
-- Mantener la transacción abierta para repetir la lectura más adelante.

-- SESIÓN B ----------------------------------------------------
START TRANSACTION;
UPDATE producto
   SET stock = stock + @incremento_stock
 WHERE id = @producto_A;
COMMIT;  -- Confirma el cambio para que sea visible por otras sesiones.
SELECT 'Sesión B - Stock incrementado y confirmado' AS contexto;

-- SESIÓN A ----------------------------------------------------
SELECT stock
  FROM producto
 WHERE id = @producto_A;
-- Resultado esperado en READ COMMITTED: se observa @stock_inicial + @incremento_stock
-- (la lectura repetida muestra el nuevo valor, evidenciando lecturas no repetibles).

-- Limpieza (Sesión A): cerrar la transacción y restaurar el stock original.
ROLLBACK;
START TRANSACTION;
UPDATE producto
   SET stock = @stock_inicial
 WHERE id = @producto_A;
COMMIT;
-- Con esto el dato vuelve a su valor previo y no quedan locks activos.

-- 2. REPEATABLE READ (Evita Lecturas Sucias y No Repetibles, pero puede tener Phantom Reads)
SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ;
SELECT 'Nivel de Aislamiento: REPEATABLE READ' AS Configuración;
SELECT @@transaction_isolation;

-- -------------------------------------------------------------
-- PRUEBA REPEATABLE READ → Lectura estable
-- -------------------------------------------------------------
-- Preparación idéntica: dos sesiones paralelas con el mismo nivel de aislamiento.
-- Repetir en cada sesión: USE producto_barras; SET @producto_A = 1; SET @incremento_stock = 5.
-- Objetivo: demostrar que la Sesión A mantiene un snapshot estable aunque la
-- Sesión B confirme cambios.

-- SESIÓN A ----------------------------------------------------
START TRANSACTION;
SELECT @stock_inicial := stock
  FROM producto
 WHERE id = @producto_A;
SELECT 'Sesión A - Lectura inicial (RR)' AS contexto, @stock_inicial AS stock_leido;

-- SESIÓN B ----------------------------------------------------
START TRANSACTION;
UPDATE producto
   SET stock = stock + @incremento_stock
 WHERE id = @producto_A;
COMMIT;
SELECT 'Sesión B - Cambio confirmado (RR)' AS contexto;

-- SESIÓN A ----------------------------------------------------
SELECT stock
  FROM producto
 WHERE id = @producto_A;
-- Resultado esperado en REPEATABLE READ: la segunda lectura mantiene @stock_inicial,
-- aun cuando la tabla ya contiene el nuevo valor. No hay lecturas no repetibles.

-- Limpieza (Sesión A): finalizar la transacción y devolver el dato a su valor inicial.
ROLLBACK;
START TRANSACTION;
UPDATE producto
   SET stock = @stock_inicial
 WHERE id = @producto_A;
COMMIT;

-- 3. SERIALIZABLE (Máximo Aislamiento, menor concurrencia)
SET SESSION TRANSACTION ISOLATION LEVEL SERIALIZABLE;
SELECT 'Nivel de Aislamiento: SERIALIZABLE' AS Configuración;
SELECT @@transaction_isolation;

-- -------------------------------------------------------------
-- PRUEBA SERIALIZABLE → Actualizaciones bloqueadas
-- -------------------------------------------------------------
-- Preparación: repetir la configuración del nivel en ambas sesiones
-- (USE producto_barras; SET @producto_A = 1; SET @incremento_stock = 5).
-- Objetivo: comprobar que una lectura en la Sesión A bloquea la actualización de la
-- Sesión B hasta que la primera transacción finalice (comportamiento serializable).

-- SESIÓN A ----------------------------------------------------
START TRANSACTION;
SELECT @stock_inicial := stock
  FROM producto
 WHERE id = @producto_A;
SELECT 'Sesión A - Lectura inicial (SER)' AS contexto, @stock_inicial AS stock_leido;
-- Mantener abierta la transacción para observar el bloqueo.

-- SESIÓN B ----------------------------------------------------
START TRANSACTION;
UPDATE producto
   SET stock = stock + @incremento_stock
 WHERE id = @producto_A;
-- En SERIALIZABLE esta instrucción quedará BLOQUEADA hasta que la Sesión A haga
-- COMMIT o ROLLBACK, ya que la lectura previa adquirió un lock compartido.

-- SESIÓN A ----------------------------------------------------
ROLLBACK;  -- o COMMIT; liberar la transacción desbloquea a la Sesión B.

-- SESIÓN B ----------------------------------------------------
-- Una vez liberado el bloqueo, decidir si se mantiene el cambio o se descarta.
ROLLBACK;  -- Recomendado para no modificar definitivamente el stock.
SELECT 'Sesión B - Transacción cancelada (SER)' AS contexto;
-- Si se decide confirmar el cambio, ejecutar COMMIT en lugar de ROLLBACK y luego
-- restaurar el valor como en las pruebas anteriores.
