# tp_sql — Roadmap del Trabajo Práctico (Bases de Datos I)

Este repositorio contiene los scripts y datos de apoyo para el Trabajo Integrador de "Bases de Datos I". El objetivo es modelar un dominio simple (productos y
códigos de barras), poblarlo con datos, crear vistas/consultas y validar integridad, seguridad y concurrencia según las etapas.

## Estructura del repositorio

- `SQL_etapa1_producto_barras.sql` : DDL del esquema (tablas, PK, FK, UNIQUE, CHECK, ejemplos de inserción)
- `SQL_etapa2_seed_12cats_100brands_200names.sql` : Seeds y scripts de carga masiva (tablas semilla y generación de productos + códigos de barras)
- `SQL_etapa2_vistas_testing.sql` : Vistas útiles para inspección y consultas de verificación
- `SQL_etapa4_seguridad.sql` : Usuario con mínimos privilegios, vistas seguras y pruebas de integridad
- `ProductoDAO.java` : DAO con PreparedStatement (consultas seguras)
- `TestSQLInjection.java` : Test automatizado de prevención de SQL Injection
- `ENTREGABLES_ETAPA4.md` : Documentación detallada de la Etapa 4
- `README.md` : roadmap y guía de ejecución

## Resumen por etapa (qué se entrega)

- Etapa 1 — Modelado y Constraints

  - Entregable: DER (externo), script DDL (`SQL_etapa1_producto_barras.sql`) con pruebas (2 inserciones válidas + 2 inválidas).
  - Objetivo: garantizar integridad referencial y reglas de dominio (márgenes, fechas, nombres no vacíos, formato GTIN).

- Etapa 2 — Generación y carga de datos

  - Entregable: scripts de semillas y carga masiva (`SQL_etapa2_seed_12cats_100brands_200names.sql`).
  - Objetivo: poblar la base con datos realistas para pruebas y reportes.

- Etapa 3 — Consultas y vistas

  - Entregable: vistas y consultas de ejemplo (`SQL_etapa2_vistas_testing.sql`) y los scripts de consulta para medidas de rendimiento.
  - Objetivo: crear consultas con JOIN, GROUP BY/HAVING y subconsultas; crear vistas útiles.

- Etapa 4 — Seguridad e integridad

  - Entregables: `SQL_etapa4_seguridad.sql`, `ProductoDAO.java`, `TestSQLInjection.java`, `ENTREGABLES_ETAPA4.md`
  - Objetivo: aplicar mínimos privilegios, usar vistas seguras, prevenir SQL Injection con PreparedStatement, validar constraints.

- Etapa 5 — Concurrencia y transacciones (pendiente)
  - Entregables: simulación de deadlocks, comparación de niveles de aislamiento, manejo de transacciones en Java.

## Orden de ejecución (MySQL 8.0+)

### Etapas 1-3: Modelado, carga de datos y vistas

1. Conectar al servidor MySQL con un usuario que tenga permiso para crear bases de datos.
2. Ejecutar `SQL_etapa1_producto_barras.sql` para crear la base `producto_barras` y las tablas.
3. Ejecutar `SQL_etapa2_seed_12cats_100brands_200names.sql` para poblar las tablas con datos de prueba.
4. Ejecutar `SQL_etapa2_vistas_testing.sql` para crear vistas e inspeccionar datos.

### Etapa 4: Seguridad e integridad

5. Ejecutar `SQL_etapa4_seguridad.sql` para crear usuario `app_user`, vistas seguras y probar constraints.

## Comprobaciones y verificaciones rápidas (checks)

Después de cargar los datos, conviene ejecutar estas comprobaciones básicas:

- Verificar que no haya productos con margen negativo:
  - `SELECT COUNT(*) FROM producto WHERE precio < costo;` (debería ser 0 si los CHECK funcionaron)
- Verificar productos sin código de barras:
  - `SELECT COUNT(*) FROM vw_sin_codigo_barras;` (si todos deben tener CB, resultado 0)
- Contar filas por tabla:
  - `SELECT COUNT(*) FROM producto;` y `SELECT COUNT(*) FROM codigo_barras;`
- Verificar unicidad de GTIN:
  - `SELECT gtin13, COUNT(*) c FROM codigo_barras GROUP BY gtin13 HAVING c>1;` (debe devolver 0 filas)

Opcional: ejecutar `EXPLAIN` sobre consultas de JOIN para comparar tiempos con/sin índices.

## Checklist para entrega

### Etapa 1

- [ ] DER en PDF o imagen incluido en la entrega
- [x] `SQL_etapa1_producto_barras.sql` (creación de tablas + ejemplos de validación)
- [ ] Capturas de errores de inserciones inválidas

### Etapa 2

- [x] `SQL_etapa2_seed_12cats_100brands_200names.sql` (datos semilla y carga)
- [ ] Documentación breve (1 página) sobre el mecanismo de carga masiva
- [ ] Cuadro de verificaciones (conteos, FK huérfanas = 0)

### Etapa 3

- [x] `SQL_etapa2_vistas_testing.sql` (vistas y consultas de inspección)
- [ ] Scripts de consultas complejas (JOIN, GROUP BY/HAVING, subconsultas)
- [ ] Tabla de tiempos con/sin índice + EXPLAIN

### Etapa 4

- [x] `SQL_etapa4_seguridad.sql` (usuario + vistas seguras + pruebas integridad)
- [x] `ProductoDAO.java` (código Java con PreparedStatement)
- [x] `TestSQLInjection.java` (prueba anti-inyección documentada)
- [x] `ENTREGABLES_ETAPA4.md` (documentación completa)
- [ ] Capturas de errores de constraints (PK, FK, UNIQUE, CHECK)
- [ ] Capturas de prueba de acceso restringido (app_user)
- [ ] Salida del test de SQL Injection

### Etapa 5 (pendiente)

- [ ] Scripts de simulación de deadlock
- [ ] Código Java con transacciones y manejo de errores
- [ ] Comparación de niveles de aislamiento

---

_Este README está pensado como guía de trabajo para el equipo de Programación (TPI final de la materia)._
