# Evidencias de rendimiento — Etapa 2

Este archivo resume el estado de las mediciones solicitadas en `scripts/E2_carga_masiva_indice_mediciones.sql`.

## 1. Contexto

El entorno de trabajo proporcionado no incluye un servidor ni cliente MySQL instalados. El primer paso fue verificar la disponibilidad del cliente CLI:

```bash
mysql --version
```

La ejecución respondió `bash: command not found: mysql`, confirmando la ausencia del binario requerido.

## 2. Intento de aprovisionamiento

Se intentó instalar los paquetes necesarios mediante `apt-get update`, pero el repositorio está bloqueado por la política de red del entorno. El gestor devolvió errores `403 Forbidden`, impidiendo descargar metadatos o paquetes.

```text
Err:1 http://security.ubuntu.com/ubuntu noble-security InRelease
  403  Forbidden
...
E: Failed to fetch http://archive.ubuntu.com/ubuntu/dists/noble/InRelease  403  Forbidden
```

Sin acceso a los repositorios oficiales resulta imposible desplegar MySQL o MariaDB en la sesión actual.

## 3. Pasos para reproducir las mediciones en un entorno con MySQL

1. **Instalar MySQL 8.0+** (cliente y servidor) en una máquina con acceso a los repositorios oficiales.
2. Ejecutar el script `scripts/E1_creacion_modelo.sql` para recrear el esquema `producto_barras`.
3. Ejecutar `scripts/E2_carga_masiva_indice_mediciones.sql` hasta el bloque de inserción masiva para poblar las tablas.
4. Volver a ejecutar el bloque de mediciones (sección 8 del script):
   ```sql
   -- parámetros
   SET @cat := 5;
   SET @pmin := 3000.00;
   SET @pmax := 6000.00;

   -- medir sin índice
   SET @t0 := NOW(6);
   PREPARE s FROM @q; EXECUTE s; DEALLOCATE PREPARE s;
   SELECT 'sin_indice' AS escenario,
          TIMESTAMPDIFF(MICROSECOND, @t0, NOW(6)) AS microseg;

   -- crear índice y medir nuevamente
   CREATE INDEX ix_prod_categoria_precio ON producto (categoria_id, precio);
   SET @t1 := NOW(6);
   PREPARE s2 FROM @q; EXECUTE s2; DEALLOCATE PREPARE s2;
   SELECT 'con_indice' AS escenario,
          TIMESTAMPDIFF(MICROSECOND, @t1, NOW(6)) AS microseg;
   ```
5. Capturar el `EXPLAIN` de la consulta final:
   ```sql
   EXPLAIN SELECT COUNT(*)
   FROM producto
   WHERE categoria_id = @cat
     AND precio BETWEEN @pmin AND @pmax;
   ```
6. Registrar los resultados (tiempos y plan de ejecución) y adjuntarlos a este archivo o generar un PDF complementario.

## 4. Próximos pasos

- Repetir las mediciones en un entorno controlado y añadir una tabla comparativa (`sin índice` vs `con índice`).
- Incorporar capturas de pantalla o exportes de los planes de ejecución una vez disponibles.

> **Estado actual:** pendiente de ejecución por falta de MySQL en el entorno de trabajo.
