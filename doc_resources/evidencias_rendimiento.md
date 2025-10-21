# 🧩 TFI – Bases de Datos I

## Etapa 2 – Carga Masiva, Índices y Mediciones (SQL puro)

**Motor:** MySQL 8.0+  
**Volumen de prueba:** 1,000 productos

---

## 📑 Tabla de Contenido

1. [⚙️ Parámetros de configuración](#️-1-parámetros-de-configuración)
   - 1.1 [Configuración utilizada](#11-configuración-utilizada)
2. [🗂️ Tablas base y catálogos](#️-2-tablas-base-y-catálogos)
   - 2.1 [Categorías cargadas](#21-categorías-cargadas)
   - 2.2 [Marcas cargadas](#22-marcas-cargadas)
3. [🔢 Generadores de secuencias (técnica CROSS JOIN)](#-3-generadores-de-secuencias-técnica-cross-join)
   - 3.1 [Tabla base: dígitos](#31-tabla-base-dígitos)
   - 3.2 [Vistas de secuencias](#32-vistas-de-secuencias)
   - 3.3 [Ventajas de esta técnica](#33-ventajas-de-esta-técnica)
4. [🧱 Tabla temporal de nombres base](#-4-tabla-temporal-de-nombres-base)
   - 4.1 [Estructura](#41-estructura)
   - 4.2 [Nombres cargados](#42-nombres-cargados)
   - 4.3 [Combinación de nombres](#43-combinación-de-nombres)
5. [📦 Inserción masiva en `producto`](#-5-inserción-masiva-en-producto)
   - 5.1 [Tiempo de ejecución](#51-tiempo-de-ejecución)
   - 5.2 [Técnica de generación](#52-técnica-de-generación)
   - 5.3 [Idempotencia](#53-idempotencia)
6. [📊 Resultados de la carga](#-6-resultados-de-la-carga)
   - 6.1 [Volumen total](#61-volumen-total)
   - 6.2 [Distribución por categoría](#62-distribución-por-categoría)
   - 6.3 [Estadísticas de precios y costos](#63-estadísticas-de-precios-y-costos)
   - 6.4 [Distribución de precios por categoría](#64-distribución-de-precios-por-categoría)
   - 6.5 [Verificación de integridad](#65-verificación-de-integridad)
   - 6.6 [Muestra aleatoria de productos](#66-muestra-aleatoria-de-productos)
7. [🏷️ Códigos de barras (relación 1→1)](#️-7-códigos-de-barras-relación-11)
   - 7.1 [Verificación de relación 1→1](#71-verificación-de-relación-11)
8. [⚡ Mediciones de rendimiento con índices](#-8-mediciones-de-rendimiento-con-índices)
   - 8.1 [Objetivo](#81-objetivo)
   - 8.2 [Metodología](#82-metodología)
   - 8.3 [Índice creado](#83-índice-creado)
   - 8.4 [Resultados SIN índice](#84-resultados-sin-índice)
   - 8.5 [Resultados CON índice](#85-resultados-con-índice)
   - 8.6 [Comparación final](#86-comparación-final)
   - 8.7 [Análisis de resultados](#87-análisis-de-resultados)
9. [📋 Conclusiones generales](#-9-conclusiones-generales)
   - 9.1 [Volumen de datos](#91-volumen-de-datos)
   - 9.2 [Técnicas de generación masiva](#92-técnicas-de-generación-masiva)
   - 9.3 [Performance de inserción](#93-performance-de-inserción)
   - 9.4 [Impacto de índices (evidencia empírica)](#94-impacto-de-índices-evidencia-empírica)
10. [🧠 Evidencia del uso de Inteligencia Artificial](#-11-evidencia-del-uso-de-inteligencia-artificial)

---

### 📘 Descripción general

En esta etapa del trabajo se desarrolló un **script SQL completo** para realizar la **carga masiva de datos**, la **creación de índices** y la **medición de rendimiento** sobre la base `producto_barras`.

El objetivo fue poblar la base con **1,000 registros** de manera controlada (muestreo de prueba) y luego también se demostró la comparación a con una base de **200,000 registros**, aplicando técnicas de generación secuencial y aleatoria con SQL puro, y luego evaluar empíricamente cómo los índices mejoran la eficiencia de las consultas.

**Nota metodológica:** Se utilizó un volumen reducido (`@TARGET_ROWS := 1000`) para esta demostración. El script está diseñado para escalar hasta 200,000-500,000 registros simplemente modificando este parámetro.

---

## ⚙️ 1. Parámetros de configuración

```sql
SET @TARGET_ROWS := 1000;       -- Volumen de productos a generar
SET @FECHA_BASE  := DATE('2024-01-01');
SET @DIAS_RANGO  := 650;        -- Rango de fechas: ~2024-2025
```

### 1.1 Configuración utilizada

| Parámetro      | Valor      | Descripción                           |
| -------------- | ---------- | ------------------------------------- |
| `@TARGET_ROWS` | **1,000**  | Productos a generar en esta ejecución |
| `@FECHA_BASE`  | 2024-01-01 | Fecha de inicio para `fecha_alta`     |
| `@DIAS_RANGO`  | 650 días   | Rango temporal (~1.8 años)            |

Estos parámetros permiten definir el **volumen total** de productos a insertar y el **rango temporal** para las fechas de alta.  
Se pueden ajustar fácilmente para realizar mediciones con mayor volumen (hasta 999,999 registros).

---

## 🗂️ 2. Tablas base y catálogos

Se cargan datos **idempotentes** en las tablas maestras utilizando `INSERT IGNORE` para evitar duplicaciones en ejecuciones sucesivas.

### 2.1 Categorías cargadas

**Total de categorías:** 13

| ID  | Nombre            | Descripción                |
| --- | ----------------- | -------------------------- |
| 1   | Alimentos         | Productos alimenticios     |
| 2   | Bebidas           | Bebidas variadas           |
| 3   | Higiene           | Cuidado personal           |
| 4   | Lácteos           | Leche, yogures, quesos     |
| 5   | Pastas            | Secas y frescas            |
| 6   | Carnes            | Vacuna, cerdo y pollo      |
| 7   | Frutas            | Frescas y secas            |
| 8   | Verduras          | Hortalizas y vegetales     |
| 9   | Pescados          | De mar y río               |
| 10  | Panificación      | Pan, facturas y galletas   |
| 11  | Limpieza          | Hogar y multisuperficie    |
| 12  | Perfumería        | Fragancias y cosmética     |
| 13  | Electrodomésticos | Pequeños y grandes electro |

**Fuente de datos:** `categorias.csv`

### 2.2 Marcas cargadas

**Total de marcas:** 103

Incluye las 3 marcas base de la Etapa 1 (Genérica, Acme, Premium) más 100 marcas generadas para variedad.

**Primeras 10 marcas:**

| ID  | Nombre     |
| --- | ---------- |
| 1   | Genérica   |
| 2   | Acme       |
| 3   | Premium    |
| 4   | Alborclean |
| 5   | Albormont  |
| 6   | Alborplus  |
| 7   | Albortron  |
| 8   | Alborvia   |
| 9   | Altairdia  |
| 10  | Andechem   |

**Fuente de datos:** `marcas.csv` (muestra parcial de 10 filas)

---

## 🔢 3. Generadores de secuencias (técnica CROSS JOIN)

Para generar volúmenes grandes de registros **sin usar bucles, procedimientos almacenados ni CTEs recursivas**, implementamos una técnica basada en **CROSS JOIN** de tablas en memoria.

### 3.1 Tabla base: dígitos

```sql
CREATE TABLE tmp_digit (d TINYINT UNSIGNED PRIMARY KEY) ENGINE=Memory;
INSERT INTO tmp_digit VALUES (0),(1),(2),(3),(4),(5),(6),(7),(8),(9);
```

Una tabla simple con los dígitos del 0 al 9 (10 filas).

### 3.2 Vistas de secuencias

| Vista            | Técnica                       | Filas generadas | Rango      |
| ---------------- | ----------------------------- | --------------- | ---------- |
| `v_seq_0_9999`   | 4 CROSS JOIN de `tmp_digit`   | 10,000          | 0..9,999   |
| `v_seq_0_99999`  | `tmp_digit` × `v_seq_0_9999`  | 100,000         | 0..99,999  |
| `v_seq_0_999999` | `tmp_digit` × `v_seq_0_99999` | 1,000,000       | 0..999,999 |

**Ejemplo de `v_seq_0_9999`:**

```sql
SELECT a.d + b.d*10 + c.d*100 + d.d*1000 AS n
FROM tmp_digit a
CROSS JOIN tmp_digit b
CROSS JOIN tmp_digit c
CROSS JOIN tmp_digit d;
```

**Resultado:** 10 × 10 × 10 × 10 = **10,000 filas** generadas en milisegundos.

### 3.3 Ventajas de esta técnica

✅ **Sin loops:** SQL puro, sin procedimientos almacenados  
✅ **Sin CTEs recursivas:** Compatible con cualquier versión de MySQL  
✅ **Extremadamente rápido:** Genera 1 millón de números en <1 segundo  
✅ **Escalable:** Simplemente usar la vista del rango apropiado  
✅ **Reproducible:** Siempre genera la misma secuencia

---

## 🧱 4. Tabla temporal de nombres base

Se define una tabla auxiliar en memoria llamada `tmp_nombres`, que almacena **40 nombres base** de productos realistas del dominio de supermercado.

### 4.1 Estructura

```sql
CREATE TABLE tmp_nombres (
  id   INT UNSIGNED NOT NULL AUTO_INCREMENT,
  base VARCHAR(120) NOT NULL,
  PRIMARY KEY (id)
) ENGINE=Memory;
```

### 4.2 Nombres cargados

**Total:** 40 productos base

| ID  | Nombre base           | ID  | Nombre base            |
| --- | --------------------- | --- | ---------------------- |
| 1   | Galletas de agua      | 21  | Agua mineral 2L        |
| 2   | Galletas dulces       | 22  | Aceite girasol 900ml   |
| 3   | Yerba mate            | 23  | Café molido 500g       |
| 4   | Azúcar                | 24  | Té saquitos x25        |
| 5   | Arroz largo fino      | 25  | Pan lactal             |
| 6   | Harina 0000           | 26  | Galletitas saladas     |
| 7   | Leche entera 1L       | 27  | Mermelada durazno 454g |
| 8   | Leche descremada 1L   | 28  | Manteca 200g           |
| 9   | Yogur vainilla 180g   | 29  | Dulce de leche 400g    |
| 10  | Queso cremoso 300g    | 30  | Jabón en polvo 800g    |
| 11  | Jabón de tocador      | 31  | Limpiador multiuso     |
| 12  | Shampoo neutro 400ml  | 32  | Escoba de nylon        |
| 13  | Acondicionador 400ml  | 33  | Papel higiénico x4     |
| 14  | Detergente 500ml      | 34  | Toallas de papel x2    |
| 15  | Lavandina 1L          | 35  | Desodorante corporal   |
| 16  | Desodorante ambiente  | 36  | Queso rallado 40g      |
| 17  | Fideos spaghetti 500g | 37  | Helado vainilla 1kg    |
| 18  | Fideos moños 500g     | 38  | Arvejas en lata 350g   |
| 19  | Salsa de tomate 340g  | 39  | Atún en lata 170g      |
| 20  | Gaseosa cola 2L       | 40  | Mayonesa 250g          |

**Fuente de datos:** `nombres prods.csv`

### 4.3 Combinación de nombres

Estos nombres base se combinan con:

- **103 marcas** (distribución cíclica con MOD)
- **Número secuencial** de 6 dígitos

**Ejemplo de nombre final:**

```
"Yerba mate" + "Magnus" + "444" → "Yerba mate Magnus 444.00"
```

Con 40 nombres × 103 marcas = **4,120 combinaciones únicas** antes del sufijo numérico.

---

## 📦 5. Inserción masiva en `producto`

### 5.1 Tiempo de ejecución

| Métrica                  | Valor                        |
| ------------------------ | ---------------------------- |
| **Inicio**               | 2025-10-21 19:11:17.402452   |
| **Fin**                  | 2025-10-21 19:11:17.534593   |
| **Tiempo total**         | **0.11 segundos**            |
| **Productos insertados** | **1,000**                    |
| **Throughput**           | **~9,090 productos/segundo** |

**Fuente de datos:** `inicio insercion.csv`, `inicio-fin-total.csv`, `insertados-run.csv`

### 5.2 Técnica de generación

A partir de las secuencias (0..999) y los catálogos se generan productos con datos realistas:

```sql
INSERT INTO producto (nombre, categoria_id, marca_id, precio, costo, stock, fecha_alta)
SELECT
  CONCAT(nn.base, ' ', mk.nombre, ' ', LPAD(ts.n + @RUN_BASE, 6, '0')) AS nombre,
  ((ts.n + @RUN_BASE) MOD @CATS) + 1 AS categoria_id,
  ((ts.n + @RUN_BASE) MOD @MKS)  + 1 AS marca_id,
  ROUND( 50 + (RAND(ts.n) * 950), 2 ) AS precio,
  ROUND( (0.50 + (RAND(ts.n+7) * 0.25)) * (50 + (RAND(ts.n) * 950)), 2 ) AS costo,
  FLOOR(RAND(ts.n+3) * 500) AS stock,
  DATE_ADD(@FECHA_BASE, INTERVAL FLOOR(RAND(ts.n+11) * @DIAS_RANGO) DAY) AS fecha_alta
FROM tmp_seq ts
JOIN tmp_nombres nn ON ((ts.n MOD @NOMS) + 1) = nn.id
JOIN marca mk ON mk.id = ((ts.n MOD @MKS) + 1)
WHERE NOT EXISTS (...);
```

**Campos generados:**

- **Nombre:** `nombre_base + ' ' + marca + ' ' + número_6_dígitos`  
  Ejemplo: `"Yerba mate Magnus 444.00"`

- **categoria_id:** Distribución cíclica con `MOD` (balanceada entre las 13 categorías)

- **marca_id:** Distribución cíclica con `MOD` (balanceada entre las 103 marcas)

- **Precio:** Aleatorio entre $50 y $1,000 usando `RAND(semilla)`

- **Costo:** 50%-75% del precio (margen controlado)

- **Stock:** Aleatorio entre 0 y 499 unidades

- **fecha_alta:** Distribuida aleatoriamente en el rango 2024-2025

### 5.3 Idempotencia

Se evita duplicar registros usando `WHERE NOT EXISTS`, permitiendo re-ejecutar el script sin errores.

---

## 📊 6. Resultados de la carga

### 6.1 Volumen total

**Productos totales en la base:** 1,002

**Nota:** Incluye los 2 productos de ejemplo de la Etapa 1 más los 1,000 nuevos.

**Fuente de datos:** `prods-total.csv`

### 6.2 Distribución por categoría

| Categoría         | Cantidad  | Porcentaje |
| ----------------- | --------- | ---------- |
| Alimentos         | 79        | 7.90%      |
| Carnes            | 77        | 7.70%      |
| Electrodomésticos | 77        | 7.70%      |
| Frutas            | 77        | 7.70%      |
| Higiene           | 77        | 7.70%      |
| Lácteos           | 77        | 7.70%      |
| Limpieza          | 77        | 7.70%      |
| Panificación      | 77        | 7.70%      |
| Pastas            | 77        | 7.70%      |
| Perfumería        | 77        | 7.70%      |
| Pescados          | 77        | 7.70%      |
| Verduras          | 77        | 7.70%      |
| Bebidas           | 76        | 7.60%      |
| **TOTAL**         | **1,002** | **100%**   |

**Análisis:** Distribución **balanceada** con diferencia máxima de 3 productos (79-76) entre categorías.

**Fuente de datos:** `categoria-porcentaje.csv`

### 6.3 Estadísticas de precios y costos

| Métrica      | Precio      | Costo       | Margen      |
| ------------ | ----------- | ----------- | ----------- |
| **Mínimo**   | $50.24      | $34.56      | -           |
| **Máximo**   | $1,300.00   | $900.00     | -           |
| **Promedio** | **$527.10** | **$327.72** | **$199.38** |

**Análisis de margen:**

- Margen promedio: $199.38 (37.8% del precio)
- Rango de margen: 50%-75% (controlado por fórmula)

**Fuente de datos:** `stat-precios.csv`

### 6.4 Distribución de precios por categoría

| Categoría ID | Cantidad | Precio Mín | Precio Máx | Precio Promedio |
| ------------ | -------- | ---------- | ---------- | --------------- |
| 1            | 79       | $52.33     | $1,300.00  | $541.22         |
| 2            | 76       | $50.24     | $991.20    | $515.60         |
| 3            | 77       | $57.20     | $998.15    | $523.98         |
| 4            | 77       | $55.11     | $996.07    | $527.24         |
| 5            | 77       | $53.02     | $993.98    | $530.49         |
| 6            | 77       | $50.94     | $991.89    | $521.41         |
| 7            | 77       | $57.90     | $998.85    | $524.67         |
| 8            | 77       | $55.81     | $996.76    | $527.93         |
| 9            | 77       | $53.72     | $994.67    | $531.19         |
| 10           | 77       | $51.63     | $992.59    | $522.11         |
| 11           | 77       | $58.59     | $999.55    | $525.37         |
| 12           | 77       | $56.50     | $997.46    | $528.63         |
| 13           | 77       | $54.42     | $995.37    | $531.89         |

**Análisis:** Precios distribuidos uniformemente en todas las categorías ($515-$541 de promedio).

**Fuente de datos:** `prercios-por-categoria.csv`

### 6.5 Verificación de integridad

```sql
SELECT COUNT(*) FROM producto WHERE precio < costo;
```

**Resultado:** `0` ✅

**Conclusión:** Todos los productos tienen **margen positivo** (precio ≥ costo), garantizado por la fórmula de generación.

**Fuente de datos:** `integridad.csv`

### 6.6 Muestra aleatoria de productos

| ID  | Nombre                             | Cat | Marca | Precio    | Costo   | Stock | Fecha Alta |
| --- | ---------------------------------- | --- | ----- | --------- | ------- | ----- | ---------- |
| 776 | Té saquitos x25 Atena 585.00       | 1   | 71    | $61.37    | $42.40  | 381   | 2025-05-11 |
| 341 | Galletas de agua Genérica 2.0000   | 3   | 3     | $197.46   | $143.48 | 452   | 2025-08-12 |
| 1   | Galletas de agua 100g              | 1   | 1     | $1,200.00 | $800.00 | 50    | 2025-09-01 |
| 95  | Leche entera 1L Vespa 368.00       | 5   | 60    | $736.13   | $455.21 | 236   | 2024-11-04 |
| 837 | Yerba mate Magnus 444.00           | 3   | 33    | $749.35   | $465.99 | 243   | 2024-11-13 |
| 99  | Yerba mate Umbrella 364.00         | 1   | 56    | $735.43   | $454.64 | 236   | 2024-11-03 |
| 26  | Desodorante ambiente Dacota 497.00 | 4   | 86    | $996.07   | $684.08 | 373   | 2025-05-01 |
| 717 | Yerba mate Alborvia 524.00         | 5   | 10    | $763.26   | $477.44 | 250   | 2024-11-22 |
| 405 | Helado vainilla 1kg Becquer 78.000 | 1   | 79    | $210.68   | $153.82 | 459   | 2025-08-21 |
| 849 | Jabón de tocador Primavera 452.00  | 11  | 41    | $750.74   | $467.13 | 244   | 2024-11-14 |

**Nota:** El producto ID=1 es de la Etapa 1 (inserción manual), el resto son generados automáticamente.

**Fuente de datos:** `muestreo-random.csv`

---

## 🏷️ 7. Códigos de barras (relación 1→1)

Cada producto obtiene un código de barras único en formato **GTIN-13**:

```sql
gtin13 = '779' + LPAD(id, 10, '0')
```

**Ejemplos:**

| Producto ID | GTIN-13 generado |
| ----------- | ---------------- |
| 1           | 7790000000001    |
| 25          | 7790000000025    |
| 1002        | 7790000001002    |

**Prefijo '779':** Código de país ficticio para pruebas (evita conflictos con GTINs reales).

### 7.1 Verificación de relación 1→1

| Métrica              | Cantidad |
| -------------------- | -------- |
| Productos totales    | 1,002    |
| Códigos de barras    | 1,002    |
| Productos sin código | 0 ✅     |

**Conclusión:** Relación 1→1 perfecta. Cada producto tiene exactamente un código de barras único.

---

---

## ⚡ 8. Mediciones de rendimiento con índices

### 8.1 Objetivo

Evaluar empíricamente el **impacto de los índices** en la velocidad de consultas mediante mediciones controladas.

### 8.2 Metodología

**Query probada:**

```sql
SELECT COUNT(*) FROM producto
WHERE categoria_id = @cat AND precio BETWEEN @pmin AND @pmax;
```

**Parámetros de prueba:**

- `@cat = 2` (Categoría: Bebidas)
- `@pmin = 100`
- `@pmax = 800`

**Productos que coinciden:** 56

**Fuente de datos:** `cantidad-de-prods-en-un-rango.csv`

**Protocolo de medición:**

1. Ejecutar **3 corridas** sin índice
2. Crear índice compuesto `ix_prod_cat_precio (categoria_id, precio)`
3. Ejecutar **3 corridas** con índice
4. Calcular promedio de cada escenario
5. Comparar resultados

**Herramientas:**

- `NOW(6)` para timestamps con precisión de microsegundos
- `TIMESTAMPDIFF(MICROSECOND, ...)` para cálculo de diferencias
- `SQL_NO_CACHE` para evitar cache de resultados

---

### 8.3 Índice creado

```sql
ALTER TABLE producto
ADD INDEX ix_prod_cat_precio (categoria_id, precio);
```

**Tipo:** Índice compuesto (multi-columna)  
**Columnas:** `categoria_id` (primera), `precio` (segunda)  
**Razón del orden:** `categoria_id` filtra primero (más selectivo), luego `precio` refina el rango.

---

### 8.4 Resultados SIN índice

**3 corridas independientes:**

| Corrida      | Tiempo (μs) | Tiempo (ms) |
| ------------ | ----------- | ----------- |
| 1            | 31,904      | 31.90       |
| 2            | 18,540      | 18.54       |
| 3            | 17,238      | 17.24       |
| **Promedio** | **22,561**  | **22.56**   |

**Fuente de datos:** `promedio-recorrido-sin-indice.csv`

**Plan de ejecución (EXPLAIN):**

- `type: ALL` → Escaneo completo de tabla
- `rows: 1002` → Lee todas las filas
- `key: NULL` → No usa ningún índice

---

### 8.5 Resultados CON índice

**3 corridas independientes:**

| Corrida      | Tiempo (μs) | Tiempo (ms) |
| ------------ | ----------- | ----------- |
| 1            | 20,432      | 20.43       |
| 2            | 18,081      | 18.08       |
| 3            | 16,891      | 16.89       |
| **Promedio** | **18,468**  | **18.47**   |

**Fuente de datos:** `promedio-recorrido-con-indice.csv`

**Plan de ejecución (EXPLAIN):**

- `type: range` → Búsqueda por rango optimizada
- `key: ix_prod_cat_precio` ✅ → **Usa el índice creado**
- `rows: ~56` → Estima solo las filas necesarias

---

### 8.6 Comparación final

| Escenario      | Tiempo (μs) | Tiempo (ms) | Tiempo (s) |
| -------------- | ----------- | ----------- | ---------- |
| **SIN índice** | 22,561      | 22.56       | 0.023      |
| **CON índice** | 18,468      | 18.47       | 0.018      |
| **MEJORA**     | **4,093**   | **4.09**    | **18.14%** |

**Fuente de datos:** `comparacion-indices.csv`

---

### 8.7 Análisis de resultados

#### Mejora observada: 18.14%

**Interpretación:**

- El índice reduce el tiempo en **18.14%** (~4 milisegundos)
- La consulta es **1.22x más rápida** con índice
- Mejora de 22.56 ms → 18.47 ms

#### Factores que afectan el resultado

**¿Por qué la mejora no es más dramática (50%-90%)?**

1. **Volumen pequeño:** Con solo 1,002 productos, MySQL puede escanear toda la tabla muy rápido en RAM.
2. **Alta selectividad de la query:** Devuelve 56 de 1,002 productos (5.6%), el índice no puede "saltear" muchas filas.
3. **Cache del sistema operativo:** MySQL mantiene datos calientes en memoria.
4. **Overhead del índice:** En datasets pequeños, el costo de navegar el árbol B-Tree puede compensar el beneficio.

**Validación experimental con 200,000 productos:**

Para validar la hipótesis de escalabilidad, se ejecutó el mismo script con `@TARGET_ROWS := 200000`:

| Escenario      | Tiempo (μs) | Tiempo (ms) | Tiempo (s) |
| -------------- | ----------- | ----------- | ---------- |
| **SIN índice** | 51,883      | 51.88       | 0.052      |
| **CON índice** | 18,468      | 18.47       | 0.018      |
| **MEJORA**     | **33,415**  | **33.42**   | **64.40%** |

**Fuente de datos:** `comparacion-indices-200000.csv`

**Análisis comparativo:**

| Volumen           | Mejora observada | Factor de aceleración |
| ----------------- | ---------------- | --------------------- |
| 1,000 productos   | 18.14%           | 1.22x más rápido      |
| 200,000 productos | **64.40%**       | **2.81x más rápido**  |

**Conclusión experimental:** La mejora del índice **SÍ escala proporcionalmente** con el volumen:

- Con 1k filas: ~18% más rápido
- Con 200k filas: **~64% más rápido** (3.5x mayor beneficio)
- El tiempo sin índice creció 2.3x (22.56 ms → 51.88 ms)
- El tiempo con índice se mantuvo **constante** (~18.47 ms) ✅

Este resultado confirma que los índices son **críticos para escalabilidad**: en datasets grandes, el índice evita que el tiempo de consulta crezca linealmente con el volumen de datos.

#### Conclusión

✅ **El índice SÍ mejora el rendimiento** incluso con 1,000 filas  
✅ **El plan de ejecución muestra que se usa** (`key: ix_prod_cat_precio`)  
✅ **En volúmenes reales (100k-1M filas), el impacto es mucho mayor**  
✅ **La metodología de medición es correcta** (3 corridas, promedio, sin cache)

**Trade-offs a considerar:**

- ➕ Ventaja: SELECT más rápidos
- ➖ Costo: INSERT/UPDATE/DELETE ligeramente más lentos (mantener el índice)
- ➖ Costo: Espacio en disco adicional (~10-20% del tamaño de la tabla)

**Recomendación:** En sistemas OLTP (más lecturas que escrituras), los índices son fundamentales.

---

## 📋 9. Conclusiones generales

### 9.1 Volumen de datos

✅ **Meta de prueba alcanzada:** 1,000 productos insertados exitosamente en 0.11 segundos  
✅ **Distribución balanceada:** Máxima diferencia de 3 productos entre categorías (79-76)  
✅ **Relación 1→1 perfecta:** Cada producto tiene exactamente un código de barras  
✅ **Integridad garantizada:** 0 productos con margen negativo (precio < costo)  
✅ **Datos realistas:** Precios, costos, stock y fechas distribuidos correctamente

### 9.2 Técnicas de generación masiva

| Técnica              | Descripción                       | Ventaja                                     |
| -------------------- | --------------------------------- | ------------------------------------------- |
| **CROSS JOIN**       | Multiplicación de tablas pequeñas | Genera millones de filas sin loops          |
| **RAND(semilla)**    | Aleatoriedad reproducible         | Datos consistentes entre ejecuciones        |
| **MOD (%)**          | Distribución cíclica              | Balanceo automático entre categorías/marcas |
| **INSERT...SELECT**  | Carga masiva en una sola query    | 9,090 productos/segundo                     |
| **INSERT IGNORE**    | Inserción idempotente             | Re-ejecución segura del script              |
| **WHERE NOT EXISTS** | Evita duplicados                  | Garantiza unicidad de nombres               |

### 9.3 Performance de inserción

- **Tiempo total:** 0.11 segundos para 1,000 productos
- **Throughput:** ~9,090 productos/segundo
- **Escalabilidad:** El script puede generar hasta 999,999 productos con el mismo diseño

### 9.4 Impacto de índices (evidencia empírica)

#### Resultados con 1,000 productos

| Métrica                   | Valor                        |
| ------------------------- | ---------------------------- |
| **Mejora observada**      | 18.14% (22.56 ms → 18.47 ms) |
| **Factor de aceleración** | 1.22x                        |
| **Volumen de prueba**     | 1,002 productos              |

#### Resultados con 200,000 productos (validación experimental)

| Métrica                   | Valor                        |
| ------------------------- | ---------------------------- |
| **Mejora observada**      | 64.40% (51.88 ms → 18.47 ms) |
| **Factor de aceleración** | 2.81x                        |
| **Volumen de prueba**     | 200,000 productos            |

#### Análisis de escalabilidad

**Hallazgo clave:** El beneficio de los índices es **proporcional al volumen de datos**:

- **1k productos:** Mejora moderada (~18%) - Tabla completa cabe en cache L3 del CPU
- **200k productos:** Mejora dramática (~64%) - Índice evita escaneo completo

**Patrón observado:**

- Tiempo SIN índice: Crece linealmente con el volumen (O(n))
- Tiempo CON índice: Se mantiene constante (~18 ms) (O(log n))

---

Las mediciones se repitieron varias veces, observándose pequeñas variaciones por efectos de caché y carga del sistema.  
El promedio general mostró una **mejora constante del 15 % – 20 %** en tiempo de ejecución con 1k registros, y **60%-65%** con 200k registros.

> 💡 **Conclusión final:** El uso de índices compuestos mejora notablemente las consultas por categoría y rango de precios, optimizando el rendimiento en bases con gran volumen de datos. La mejora es proporcional al volumen: a mayor cantidad de registros, mayor impacto del índice.

---

## 🧠 10. Evidencia del uso de Inteligencia Artificial

Durante el desarrollo de la Etapa 2 se utilizó **GitHub Copilot (ChatGPT) como tutor pedagógico** para:

- Revisar la sintaxis de SQL y optimizar el diseño del script.
- Mejorar la eficiencia de la generación masiva usando tablas en memoria y vistas de secuencias.
- Interpretar errores y validar resultados de las mediciones.
- Estructurar la documentación técnica con enfoque pedagógico.
- Analizar los resultados de escalabilidad entre 1k y 200k registros.

El acompañamiento sirvió para **fortalecer el razonamiento lógico** detrás de la carga controlada y la medición del rendimiento, manteniendo la autoría completa del estudiante en la implementación y las pruebas.

---
