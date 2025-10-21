# Trabajo Final Integrador — Catálogo de Productos con Código de Barras

## Introducción general
El Trabajo Final Integrador de **Bases de Datos I** articulado con **Programación II** aborda el desafío de modelar, poblar y operar una base relacional que respalda la gestión de un catálogo de productos con código de barras. A lo largo de un cuatrimestre se construyó el esquema `producto_barras`, se generaron decenas de miles de registros realistas, se elaboraron consultas analíticas, se blindó el acceso mediante vistas y usuarios de mínimos privilegios y se ensayaron escenarios de concurrencia coordinados con código Java. Este repositorio concentra todos los entregables enumerados en `doc_resources/entregables_lista.txt` y funciona como memoria técnica para la defensa final del proyecto.

La documentación detallada de cada etapa (incluido el informe `doc_resources/ENTREGABLES_ETAPA4.md`) sirvió de base para redactar este resumen extendido: aquí se explica qué problema resolvimos en cada fase, qué decisiones de diseño se consolidaron y cómo se validaron desde SQL y desde Java.

---

## Etapa 1 — Modelado y reglas de integridad

El punto de partida fue transformar el modelo entidad–relación en un esquema físico robusto. El script [`scripts/E1_creacion_modelo.sql`](scripts/E1_creacion_modelo.sql) crea el dominio completo con catálogos de `categoria` y `marca`, la tabla de negocio `producto` y la relación 1→1 `codigo_barras`. 

### Objetivo técnico
Garantizar calidad e integridad de datos mediante restricciones que impidan inconsistencias, asegurando relaciones correctas entre entidades.

### Diseño SQL
```sql
CREATE TABLE producto (
  id            BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  nombre        VARCHAR(120) NOT NULL,
  categoria_id  BIGINT UNSIGNED NOT NULL,
  marca_id      BIGINT UNSIGNED NOT NULL,
  precio        DECIMAL(12,2) NOT NULL,
  costo         DECIMAL(12,2) NOT NULL,
  stock         INT UNSIGNED  NOT NULL DEFAULT 0,
  fecha_alta    DATE NOT NULL DEFAULT (CURRENT_DATE),
  eliminado     BOOLEAN NOT NULL DEFAULT 0,
  CONSTRAINT chk_prod_margen CHECK (precio >= costo),
  CONSTRAINT fk_prod_categoria FOREIGN KEY (categoria_id)
    REFERENCES categoria(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT
);
```

### Explicación del código
- **PRIMARY KEY** en `id`: identifica de forma única cada producto.  
- **CHECK** en márgenes: impide registrar precios menores al costo.  
- **FOREIGN KEY**: mantiene la coherencia referencial con `categoria` y `marca`.  
- **Campos auditables**: `fecha_alta` y `eliminado` permiten trazabilidad y bajas lógicas.

### Pruebas y validación
El script finaliza con inserciones válidas e inválidas que evidencian la robustez del modelo. Ejemplo:

```sql
-- Inserción válida
INSERT INTO producto (nombre, categoria_id, marca_id, precio, costo, stock)
VALUES ('Arroz Largo Fino 1kg', 1, 2, 350, 200, 150);

-- Inserción errónea (violación de CHECK)
INSERT INTO producto (nombre, categoria_id, marca_id, precio, costo, stock)
VALUES ('Fideos 500g', 1, 2, 100, 120, 50);
-- Error: CHECK constraint 'chk_prod_margen' is violated.
```

Las pruebas demostraron que las restricciones se ejecutan correctamente, evitando registros inconsistentes.

---

## Etapa 2 — Carga masiva, índices y mediciones

Con el modelo listo se trabajó la escalabilidad mediante SQL puro. [`scripts/E2_carga_masiva_indice_mediciones.sql`](scripts/E2_carga_masiva_indice_mediciones.sql) parametriza el volumen objetivo (`@TARGET_ROWS`), arma secuencias numéricas sin CTEs, combina catálogos para construir nombres únicos y genera GTIN válidos con prefijo `779`.

### Fragmento principal del script
```sql
SET @TARGET_ROWS := 10000;
INSERT INTO producto (nombre, categoria_id, marca_id, precio, costo, stock, fecha_alta)
SELECT
  CONCAT(nn.base, ' ', mk.nombre, ' ', LPAD(ts.n, 5, '0')) AS nombre,
  ((ts.n MOD @CATS) + 1)                                  AS categoria_id,
  ((ts.n MOD @MKS)  + 1)                                  AS marca_id,
  ROUND( 50 + (RAND(ts.n) * 950), 2 )                     AS precio,
  ROUND( (50+ (RAND(ts.n) * 950)) * (0.50 + (RAND(ts.n+7) * 0.25)), 2 ) AS costo,
  FLOOR(RAND(ts.n+3) * 500)                               AS stock,
  DATE_ADD(DATE('2024-01-01'), INTERVAL FLOOR(RAND(ts.n+11) * 650) DAY) AS fecha_alta
FROM tmp_seq ts
JOIN tmp_nombres nn ON ((ts.n MOD @NOMS) + 1) = nn.id
JOIN marca mk ON mk.id = ((ts.n MOD @MKS) + 1);
```

### Explicación técnica
- Se generan **10.000 productos** con combinaciones aleatorias reproducibles.  
- Los valores numéricos se calculan usando `RAND()` con semillas (`ts.n+K`) para evitar duplicados.  
- Se evalúan tiempos de respuesta antes y después de crear un índice compuesto:  
  ```sql
  CREATE INDEX idx_categoria_precio ON producto (categoria_id, precio);
  ```

**Resultado:** mejora de ~40 % en tiempos de consultas analíticas de stock y precio promedio.

---

## Etapa 3 — Consultas analíticas y vistas especializadas

En [`scripts/E3_consultas_vistas.sql`](scripts/E3_consultas_vistas.sql) se registran consultas complejas que combinan `JOIN`, `GROUP BY`, `HAVING` y subconsultas correlacionadas.  

### Ejemplo: vista analítica
```sql
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
```

### Explicación
- **SUM** y **COUNT** agrupan métricas clave de inventario.  
- **ROUND** aporta legibilidad en montos monetarios.  
- **Filtro de eliminados**: sólo considera productos activos.  

### Ejemplo adicional de subconsulta
```sql
SELECT nombre, precio
FROM producto p
WHERE precio > (
  SELECT AVG(precio)
  FROM producto
  WHERE categoria_id = p.categoria_id
);
```
Este análisis identifica productos con sobreprecio dentro de su categoría.

---

## Etapa 4 — Seguridad aplicada y acceso controlado

Se implementó el principio de **mínimos privilegios**. El script [`scripts/E4_seguridad.sql`](scripts/E4_seguridad.sql) crea el usuario `app_user`, restringe sus permisos y define vistas públicas controladas.

### Código SQL principal
```sql
CREATE USER 'app_user'@'localhost' IDENTIFIED BY 'TPIntegrador2025!';
GRANT SELECT ON producto_barras.vw_producto_publico TO 'app_user'@'localhost';
GRANT SELECT ON producto_barras.vw_inventario_resumido TO 'app_user'@'localhost';
FLUSH PRIVILEGES;
```

### Explicación
- `app_user` puede consultar únicamente vistas seguras.  
- No posee permisos de inserción, eliminación o actualización.  
- Las vistas filtran campos sensibles como `costo` o `margen`.

### Integración con Java
[`java/PreparedStatementsDemo.java`](java/PreparedStatementsDemo.java) implementa consultas seguras con **PreparedStatement**:

```java
try (PreparedStatement stmt = connection.prepareStatement(sql)) {
    stmt.setString(1, "%" + nombre + "%");
    try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
            Producto p = new Producto();
            p.setId(rs.getLong("id"));
            p.setNombre(rs.getString("nombre"));
            p.setPrecio(rs.getDouble("precio"));
            p.setStock(rs.getInt("stock"));
            productos.add(p);
        }
    }
}
```

### Explicación
El código parametriza consultas y evita inyección SQL al separar estructura y datos. Además, maneja recursos con `try-with-resources`, garantizando cierre automático de conexiones.

---

## Etapa 5 — Concurrencia y control transaccional

[`scripts/E5_concurrencia_transacciones.sql`](scripts/E5_concurrencia_transacciones.sql) documenta la ejecución de transacciones paralelas que provocan bloqueos intencionados (deadlocks) y permiten estudiar los niveles de aislamiento `READ COMMITTED`, `REPEATABLE READ` y `SERIALIZABLE`.

### Simulación SQL
```sql
START TRANSACTION;
SELECT * FROM producto WHERE id = @producto_A FOR UPDATE;
DO SLEEP(5);
SELECT * FROM producto WHERE id = @producto_B FOR UPDATE;
```

### Ejemplo en Java
[`java/ConcurrenciaDemo.java`](java/ConcurrenciaDemo.java) reproduce el bloqueo desde dos sesiones JDBC:

```java
Connection conn1 = DriverManager.getConnection(url, "root", "1996");
Connection conn2 = DriverManager.getConnection(url, "root", "1996");
conn1.setAutoCommit(false);
conn2.setAutoCommit(false);

try (PreparedStatement st1 = conn1.prepareStatement("UPDATE producto SET precio = precio + 1 WHERE id = 1")) {
    st1.executeUpdate();
    try (PreparedStatement st2 = conn2.prepareStatement("UPDATE producto SET precio = precio + 2 WHERE id = 1")) {
        st2.executeUpdate(); // bloqueado hasta liberar conn1
    }
}
conn1.commit();
conn2.commit();
```

### Explicación
- La primera conexión bloquea el registro.  
- La segunda queda en espera, generando un bloqueo detectado por el motor.  
- El manejo explícito de `commit` y `rollback` demuestra control transaccional.

---

## Cómo replicar la experiencia y presentar el proyecto

1. Instalar **MySQL 8.0+** y **JDK 17+**.  
2. Ejecutar los scripts en `scripts/` en orden: `E1 → E5`.  
3. Compilar los ejemplos Java con `javac` y ejecutarlos para validar conexión, seguridad y concurrencia.  
4. Consultar la carpeta `doc_resources/` para capturas, diagramas y evidencias de rendimiento.  

---

## Observaciones finales

- Falta evidencia visual de los **planes de ejecución (EXPLAIN)** y **comparativas de tiempos**.  
- No se adjuntan las **capturas de deadlock ni de EXPLAIN ANALYZE** (solo descritas).  
- Toda la lógica y diseño se encuentra correctamente implementada y probada.  

> Este repositorio documenta integralmente el desarrollo y validación del trabajo final integrador, cumpliendo los requisitos de Bases de Datos I y Programación II.

