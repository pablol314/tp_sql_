# Entregables Etapa 4: Seguridad e Integridad

**Materia:** Base de Datos I - Tecnicatura Superior en Programación
**Fecha:** Octubre 2025

---

## 📚 Índice

1. [Resumen ejecutivo](#resumen-ejecutivo)
2. [Conceptos clave para entender esta etapa](#conceptos-clave)
3. [Usuario con privilegios mínimos](#1-usuario-con-privilegios-mínimos)
4. [Vistas que ocultan información sensible](#2-vistas-que-ocultan-información-sensible)
5. [Pruebas de integridad referencial](#3-pruebas-de-integridad-referencial)
6. [Código Java seguro (PreparedStatement)](#4-código-java-seguro)
7. [Consejos y mejoras sugeridas](#5-consejos-y-mejoras)
8. [Checklist de entrega](#checklist-de-entrega)
9. [Interacción con IA (este documento)](#interacción-con-ia)

---

## Resumen ejecutivo

Esta etapa implementa **medidas de seguridad y validaciones de integridad** en nuestra base de datos `producto_barras`. Los objetivos principales son:

- ✅ Crear un usuario con **mínimos privilegios** (solo lectura en vistas específicas)
- ✅ Diseñar **vistas seguras** que ocultan datos sensibles (costo, flags internos)
- ✅ Validar que las **restricciones de integridad funcionen** (PK, FK, UNIQUE, CHECK)
- ✅ Implementar **código Java seguro** con `PreparedStatement` para prevenir SQL Injection
- ✅ Documentar todo de forma clara para el equipo

**Archivos entregados:**

- `SQL_etapa4_seguridad.sql` - Script SQL con usuario, vistas y pruebas
- `ProductoDAO.java` - Capa de acceso a datos segura
- `ENTREGABLES_ETAPA4.md` - Este documento (evidencia y guía)

---

## Conceptos clave

Antes de revisar el código, es importante entender estos conceptos:

### Principio de mínimos privilegios

**Qué es:** Dar a cada usuario solo los permisos estrictamente necesarios para su trabajo.

**Ejemplo del mundo real:** Un cajero de supermercado puede cobrar productos, pero no puede modificar precios ni ver el costo de compra.

**En nuestra BD:** El usuario `app_user` solo puede **leer** ciertas vistas, no puede modificar, borrar ni crear tablas.

### Vistas como capa de seguridad

**Qué son:** "Ventanas" que muestran solo parte de los datos de una tabla.

**Ejemplo del mundo real:** Una vitrina de una joyería muestra los productos, pero no los precios de compra ni la alarma de seguridad.

**En nuestra BD:** `vw_producto_publico` muestra el precio de venta pero oculta el costo de compra.

### SQL Injection

**Qué es:** Un ataque donde el usuario malicioso inserta código SQL en un campo de entrada.

**Ejemplo de ataque:**

```java
// Usuario ingresa: ' OR '1'='1
// Query vulnerable: SELECT * FROM producto WHERE nombre LIKE '%' OR '1'='1%'
// Resultado: devuelve TODOS los productos (la condición '1'='1' siempre es verdadera)
```

**Cómo prevenirlo:** Usar `PreparedStatement` que escapa automáticamente caracteres peligrosos.

### Integridad referencial

**Qué es:** Garantizar que las relaciones entre tablas sean consistentes.

**Ejemplo:** No puedes crear un producto con `categoria_id=99999` si esa categoría no existe.

**Cómo se implementa:** Con constraints de `FOREIGN KEY`, `PRIMARY KEY`, `UNIQUE` y `CHECK`.

---

## 1. Usuario con privilegios mínimos

### Código (SQL_etapa4_seguridad.sql)

```sql
-- Eliminar usuario si ya existe (para poder re-ejecutar el script)
DROP USER IF EXISTS 'app_user'@'localhost';

-- Crear usuario con contraseña
CREATE USER 'app_user'@'localhost' IDENTIFIED BY 'TPIntegrador2025!';

-- Otorgar SOLO permisos de lectura (SELECT) en 3 vistas específicas
GRANT SELECT ON producto_barras.vw_producto_publico TO 'app_user'@'localhost';
GRANT SELECT ON producto_barras.vw_inventario_resumido TO 'app_user'@'localhost';
GRANT SELECT ON producto_barras.vw_busqueda TO 'app_user'@'localhost';

-- Aplicar cambios
FLUSH PRIVILEGES;

-- Verificar permisos otorgados
SHOW GRANTS FOR 'app_user'@'localhost';
```

### 🎓 Explicación para el equipo

**¿Qué hace este código?**

1. Crea un usuario llamado `app_user` que solo puede conectarse desde `localhost` (tu computadora).
2. Le da permisos de **solo lectura** (`SELECT`) en 3 vistas.
3. **NO le da** permisos para:
   - Ver tablas base directamente (producto, categoria, marca, codigo_barras)
   - Insertar, modificar o eliminar datos (`INSERT`, `UPDATE`, `DELETE`)
   - Crear o eliminar tablas (`CREATE`, `DROP`, `ALTER`)
   - Gestionar otros usuarios

**¿Por qué es importante?**

- Si una aplicación web se conecta con este usuario y es hackeada, el atacante **solo puede leer** ciertos datos.
- No puede borrar la base de datos completa.
- No puede modificar precios ni costos.
- Es una **capa extra de seguridad**.

**¿Cómo probarlo?**

```bash
# En terminal, conectar como app_user
mysql -u app_user -p producto_barras
# Contraseña: TPIntegrador2025!

# Dentro de MySQL, probar:
SELECT * FROM vw_producto_publico LIMIT 5;  -- ✅ FUNCIONA

SELECT * FROM producto;  -- ❌ ERROR 1142: SELECT command denied
```

### 📊 Tabla de permisos

| Operación             | app_user | root/admin |
| --------------------- | -------- | ---------- |
| SELECT en vistas      | ✅ SÍ    | ✅ SÍ      |
| SELECT en tablas base | ❌ NO    | ✅ SÍ      |
| INSERT/UPDATE/DELETE  | ❌ NO    | ✅ SÍ      |
| CREATE/DROP/ALTER     | ❌ NO    | ✅ SÍ      |
| GRANT (dar permisos)  | ❌ NO    | ✅ SÍ      |

---

## 2. Vistas que ocultan información sensible

### Vista 1: `vw_producto_publico` - Catálogo público

#### 📄 Código

```sql
CREATE VIEW vw_producto_publico AS
SELECT
  p.id,
  p.nombre,
  c.nombre AS categoria,
  m.nombre AS marca,
  p.precio,          -- ✅ Precio de VENTA (público)
  p.stock,
  p.fecha_alta,
  cb.gtin13,
  cb.tipo AS tipo_codigo
FROM producto p
JOIN categoria c ON c.id = p.categoria_id
JOIN marca     m ON m.id = p.marca_id
LEFT JOIN codigo_barras cb ON cb.producto_id = p.id
WHERE p.eliminado = 0;  -- Solo productos activos
```

#### 🎓 Explicación

**¿Qué muestra?**

- Información útil para clientes: nombre, precio, stock, categoría, marca
- Código de barras (para escanear en caja)

**¿Qué OCULTA?**

- ❌ `costo` - Precio de compra (confidencial, no queremos que la competencia lo vea)
- ❌ `eliminado` - Flag interno (0=activo, 1=eliminado lógicamente)
- ❌ `categoria_id`, `marca_id` - IDs técnicos (innecesarios para usuarios)

**¿Cuándo usarla?**

- Página web de catálogo
- App móvil de consulta de precios
- API pública para terceros
- Sistema de punto de venta (caja registradora)

**Ejemplo de consulta:**

```sql
-- Buscar productos de una categoría
SELECT nombre, precio, stock, marca
FROM vw_producto_publico
WHERE categoria = 'Bebidas'
  AND stock > 0
ORDER BY precio ASC
LIMIT 20;
```

---

### Vista 2: `vw_inventario_resumido` - Reportes gerenciales

#### 📄 Código

```sql
CREATE VIEW vw_inventario_resumido AS
SELECT
  c.nombre AS categoria,
  m.nombre AS marca,
  COUNT(*) AS cantidad_productos,
  SUM(p.stock) AS stock_total
FROM producto p
JOIN categoria c ON c.id = p.categoria_id
JOIN marca     m ON m.id = p.marca_id
WHERE p.eliminado = 0
GROUP BY c.nombre, m.nombre
ORDER BY c.nombre, m.nombre;
```

#### 🎓 Explicación

**¿Qué muestra?**

- Totales **agregados** por categoría y marca
- Cuántos productos hay de cada tipo
- Stock total acumulado

**¿Qué OCULTA?**

- ❌ Precios individuales de productos
- ❌ Costos
- ❌ Nombres específicos de productos

**¿Por qué agregar datos?**

- Un empleado de logística necesita saber "cuántos productos de Bebidas marca Acme hay" pero no necesita ver el precio de cada uno.
- Reduce riesgo de fuga de información comercial sensible.

**Ejemplo de consulta:**

```sql
-- Ver categorías con más productos
SELECT categoria, SUM(cantidad_productos) AS total
FROM vw_inventario_resumido
GROUP BY categoria
ORDER BY total DESC;
```

---

### 🔄 Comparación: Tabla base vs Vista segura

| Campo         | Tabla `producto` | Vista `vw_producto_publico`    |
| ------------- | ---------------- | ------------------------------ |
| id            | ✅ Visible       | ✅ Visible                     |
| nombre        | ✅ Visible       | ✅ Visible                     |
| precio        | ✅ Visible       | ✅ Visible                     |
| **costo**     | ✅ Visible       | ❌ **OCULTO**                  |
| stock         | ✅ Visible       | ✅ Visible                     |
| **eliminado** | ✅ Visible       | ❌ **OCULTO** (filtrado WHERE) |
| categoria_id  | ✅ Visible       | ❌ Reemplazado por nombre      |
| marca_id      | ✅ Visible       | ❌ Reemplazado por nombre      |

---

## 3. Pruebas de integridad referencial

Estas pruebas **deben FALLAR** para demostrar que los constraints están funcionando correctamente.

### Prueba 1: Violación de PRIMARY KEY

#### 📄 Código

```sql
INSERT INTO producto (id, nombre, categoria_id, marca_id, precio, costo, stock)
VALUES (1, 'Producto con PK duplicada', 1, 1, 500.00, 300.00, 10);
```

#### 🎓 Explicación

**¿Qué intenta hacer?**

- Insertar un producto con `id = 1`
- Pero el `id = 1` **ya existe** en la tabla

**¿Qué error debe dar?**

```
ERROR 1062 (23000): Duplicate entry '1' for key 'producto.PRIMARY'
```

**¿Por qué es importante?**

- La PRIMARY KEY garantiza que cada producto tenga un identificador único.
- Sin esto, podrías tener dos productos con el mismo ID y no sabrías cuál es cuál.
- Es como tener dos personas con el mismo DNI.

**¿Qué pasa si NO falla?**

- ⚠️ **Problema grave:** El constraint no está activo o no existe.
- Hay que revisar la definición de la tabla en `SQL_etapa1_producto_barras.sql`.

---

### Prueba 2: Violación de FOREIGN KEY

#### 📄 Código

```sql
INSERT INTO producto (nombre, categoria_id, marca_id, precio, costo, stock)
VALUES ('Producto con FK inválida', 99999, 1, 500.00, 300.00, 10);
```

#### 🎓 Explicación

**¿Qué intenta hacer?**

- Insertar un producto con `categoria_id = 99999`
- Pero esa categoría **no existe** en la tabla `categoria`

**¿Qué error debe dar?**

```
ERROR 1452 (23000): Cannot add or update a child row: a foreign key constraint fails
(`producto_barras`.`producto`, CONSTRAINT `fk_prod_categoria` FOREIGN KEY (`categoria_id`) REFERENCES `categoria` (`id`))
```

**¿Por qué es importante?**

- Garantiza **integridad referencial**: no puedes tener productos "huérfanos" sin categoría válida.
- Es como inscribir a un alumno en una carrera que no existe.

**Analogía del mundo real:**
Imagina que estás haciendo una lista de compras:

- Producto: "Leche Serenísima 1L"
- Categoría: "Lácteos"

Si borras la categoría "Lácteos" pero dejas productos que apuntan a ella, ¿a qué categoría pertenecen esos productos? 🤔

La FOREIGN KEY previene esto diciendo: "No puedes crear un producto con una categoría que no existe".

---

### Prueba 3: Violación de CHECK (margen negativo)

#### 📄 Código

```sql
INSERT INTO producto (nombre, categoria_id, marca_id, precio, costo, stock)
VALUES ('Producto con margen negativo', 1, 1, 300.00, 500.00, 10);
```

#### 🎓 Explicación

**¿Qué intenta hacer?**

- Insertar un producto donde `precio = 300` pero `costo = 500`
- Estarías **vendiendo a pérdida** (precio < costo)

**¿Qué error debe dar?**

```
ERROR 3819 (HY000): Check constraint 'chk_prod_margen' is violated.
```

**¿Por qué es importante?**

- Previene errores de negocio (vender productos a pérdida por error de carga).
- Valida **reglas de dominio**: el precio debe ser mayor o igual al costo.

**¿Cuándo SÍ sería válido vender a pérdida?**

- Liquidación de productos vencidos
- Promociones especiales

En esos casos, habría que:

1. Desactivar temporalmente el CHECK, O
2. Agregar un campo `es_promocion` que permita excepciones, O
3. Modificar el CHECK para que sea `precio >= costo OR es_promocion = 1`

---

### Prueba 4: Violación de CHECK (nombre vacío)

#### 📄 Código

```sql
INSERT INTO producto (nombre, categoria_id, marca_id, precio, costo, stock)
VALUES ('   ', 1, 1, 500.00, 300.00, 10);
```

#### 🎓 Explicación

**¿Qué intenta hacer?**

- Insertar un producto con nombre que son solo espacios en blanco

**¿Qué error debe dar?**

```
ERROR 3819 (HY000): Check constraint 'chk_prod_nombre_notblank' is violated.
```

**¿Por qué es importante?**

- Garantiza **calidad de datos**: no puedes tener productos sin nombre.
- El CHECK `TRIM(nombre) <> ''` verifica que después de quitar espacios, quede algo.

**Comparación de constraints:**

| Input              | CHECK `nombre NOT NULL` | CHECK `TRIM(nombre) <> ''`  |
| ------------------ | ----------------------- | --------------------------- |
| `'Galletas'`       | ✅ Pasa                 | ✅ Pasa                     |
| `NULL`             | ❌ Falla                | ❌ Falla                    |
| `'   '` (espacios) | ✅ Pasa                 | ❌ **Falla** ← Más estricto |
| `''` (vacío)       | ✅ Pasa                 | ❌ **Falla** ← Más estricto |

---

### 📊 Resumen de constraints probados

| Constraint     | Qué valida       | Cuándo falla                               | Código de error |
| -------------- | ---------------- | ------------------------------------------ | --------------- |
| PRIMARY KEY    | ID único         | Insertar ID duplicado                      | 1062            |
| FOREIGN KEY    | Relación válida  | Insertar con ID inexistente en tabla padre | 1452            |
| UNIQUE         | Valores únicos   | Insertar GTIN duplicado                    | 1062            |
| CHECK (margen) | Regla de negocio | precio < costo                             | 3819            |
| CHECK (nombre) | Calidad de datos | nombre vacío o solo espacios               | 3819            |

---

## 4. Código Java seguro

### 🏗️ Arquitectura: Patrón DAO

**DAO = Data Access Object** (Objeto de Acceso a Datos)

**¿Qué es?**

- Una clase que **separa** la lógica de negocio de las consultas SQL.
- Toda interacción con la base de datos pasa por el DAO.

**¿Por qué usarlo?**

- ✅ **Mantenibilidad:** Si cambias de MySQL a PostgreSQL, solo modificas el DAO.
- ✅ **Seguridad:** Centralizas las medidas de protección (PreparedStatement).
- ✅ **Testeo:** Puedes crear un DAO "falso" para tests sin tocar la BD real.

**Estructura:**

```
Aplicación (Main.java)
    ↓ usa
ProductoDAO (capa de acceso a datos)
    ↓ conecta con
Base de Datos MySQL
```

---

### 🔒 PreparedStatement: Prevención de SQL Injection

#### ❌ Código INSEGURO (NO usar)

```java
@Deprecated
public List<Producto> buscarPorNombreInseguro(String nombre) throws SQLException {
    // VULNERABLE: concatenación directa
    String sql = "SELECT * FROM producto WHERE nombre LIKE '%" + nombre + "%'";

    Statement stmt = connection.createStatement();
    ResultSet rs = stmt.executeQuery(sql);
    // ...
}
```

**¿Por qué es peligroso?**

Si el usuario ingresa: `' OR '1'='1`

La query resultante sería:

```sql
SELECT * FROM producto WHERE nombre LIKE '%' OR '1'='1%'
```

La condición `'1'='1'` es **siempre verdadera**, entonces devuelve **TODOS** los productos.

**Peor aún, si el usuario ingresa:** `'; DROP TABLE producto; --`

La query resultante:

```sql
SELECT * FROM producto WHERE nombre LIKE '%'; DROP TABLE producto; --%'
```

¡Acabas de **borrar la tabla completa**! 💀

---

#### ✅ Código SEGURO (siempre usar)

```java
public List<Producto> buscarPorNombre(String nombre) throws SQLException {
    // Query con placeholder (?)
    String sql = "SELECT id, nombre, precio, stock " +
                 "FROM vw_producto_publico " +
                 "WHERE nombre LIKE ? " +
                 "LIMIT 100";

    // PreparedStatement escapa automáticamente
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
        stmt.setString(1, "%" + nombre + "%");  // Reemplaza el ? de forma segura

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
    return productos;
}
```

**¿Qué hace diferente?**

1. **Usa `?` como placeholder** en lugar de concatenar.
2. **`setString(1, valor)`** reemplaza el primer `?` con el valor, **escapando automáticamente** comillas y caracteres especiales.
3. Si el usuario ingresa `' OR '1'='1`, se convierte en:
   ```sql
   WHERE nombre LIKE '%'' OR ''1''=''1%'
   ```
   Las comillas se escapan como `''` (doble comilla), tratándose como **texto literal**, no código SQL.

**Resultado:** El ataque NO funciona. ✅

---

### 🎓 Desglose paso a paso del método `buscarPorNombre()`

```java
public List<Producto> buscarPorNombre(String nombre) throws SQLException {
    // 1. Crear lista vacía para guardar resultados
    List<Producto> productos = new ArrayList<>();

    // 2. Definir query con placeholder (?)
    String sql = "SELECT id, nombre, precio, stock " +
                 "FROM vw_producto_publico " +
                 "WHERE nombre LIKE ? " +  // ← El ? será reemplazado
                 "LIMIT 100";

    // 3. Crear PreparedStatement (try-with-resources cierra automáticamente)
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {

        // 4. Reemplazar el ? con el valor de forma SEGURA
        stmt.setString(1, "%" + nombre + "%");
        //              ↑         ↑
        //              |         Valor a buscar (con % para LIKE)
        //              Número del placeholder (primer ?)

        // 5. Ejecutar la query
        try (ResultSet rs = stmt.executeQuery()) {

            // 6. Recorrer los resultados fila por fila
            while (rs.next()) {
                // 7. Crear objeto Producto y llenar campos
                Producto p = new Producto();
                p.setId(rs.getLong("id"));           // Columna 'id'
                p.setNombre(rs.getString("nombre")); // Columna 'nombre'
                p.setPrecio(rs.getDouble("precio")); // Columna 'precio'
                p.setStock(rs.getInt("stock"));      // Columna 'stock'

                // 8. Agregar a la lista
                productos.add(p);
            }
        }
    }

    // 9. Devolver la lista (puede estar vacía si no hay resultados)
    return productos;
}
```

**Conceptos clave:**

- **`try-with-resources`:** Cierra automáticamente `PreparedStatement` y `ResultSet` al terminar, incluso si hay error.
- **`rs.next()`:** Avanza a la siguiente fila. Devuelve `false` cuando no hay más filas.
- **`rs.getLong("id")`:** Obtiene el valor de la columna `id` como `long`.
- **`setString(1, valor)`:** El `1` indica el **primer** `?` en la query. Si hubiera más `?`, usarías `setString(2, ...)`, etc.

---

### 🛡️ Método `insertar()`: Validaciones en capas

```java
public long insertar(ProductoInsert p) throws SQLException {
    // CAPA 1: Validaciones en Java (antes de tocar la BD)
    if (p.getNombre() == null || p.getNombre().trim().isEmpty()) {
        throw new IllegalArgumentException("El nombre no puede estar vacío");
    }
    if (p.getPrecio() < 0 || p.getCosto() < 0) {
        throw new IllegalArgumentException("Precio y costo deben ser positivos");
    }
    if (p.getPrecio() < p.getCosto()) {
        throw new IllegalArgumentException("El precio debe ser mayor o igual al costo");
    }

    // CAPA 2: PreparedStatement (previene SQL Injection)
    String sql = "INSERT INTO producto (nombre, categoria_id, marca_id, precio, costo, stock, fecha_alta) " +
                 "VALUES (?, ?, ?, ?, ?, ?, CURRENT_DATE)";

    try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
        stmt.setString(1, p.getNombre().trim());
        stmt.setLong(2, p.getCategoriaId());
        stmt.setLong(3, p.getMarcaId());
        stmt.setDouble(4, p.getPrecio());
        stmt.setDouble(5, p.getCosto());
        stmt.setInt(6, p.getStock());

        stmt.executeUpdate();  // Ejecuta el INSERT

        // Obtener el ID autogenerado (AUTO_INCREMENT)
        try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                return generatedKeys.getLong(1);  // Devuelve el nuevo ID
            }
        }
    }

    // CAPA 3: Constraints de MySQL (PK, FK, CHECK)
    // Si algo falla aquí, lanzará SQLException

    throw new SQLException("No se pudo insertar el producto");
}
```

**¿Por qué validar en Java si MySQL también valida?**

| Capa      | Validación                | Ventaja                                                                                                             |
| --------- | ------------------------- | ------------------------------------------------------------------------------------------------------------------- |
| **Java**  | `if (precio < costo)`     | ✅ Error inmediato, sin viaje a la BD<br>✅ Mensaje de error personalizado<br>✅ Menor carga en el servidor         |
| **MySQL** | `CHECK (precio >= costo)` | ✅ Garantía absoluta (no se puede saltear)<br>✅ Protege si accedes por otro programa<br>✅ Última línea de defensa |

**Conclusión:** Ambas son importantes. Java = primera defensa (rápida), MySQL = defensa definitiva (infalible).

---

### 📚 Clases auxiliares

#### Clase `Producto` (para lectura)

```java
class Producto {
    private Long id;          // ← Tiene ID (ya existe en BD)
    private String nombre;
    private String categoria;
    private String marca;
    private Double precio;
    private Integer stock;
    private Date fechaAlta;
    private String gtin13;

    // Getters y setters...

    @Override
    public String toString() {
        return String.format("Producto[id=%d, nombre='%s', precio=%.2f, stock=%d]",
                             id, nombre, precio, stock);
    }
}
```

**Cuándo usarla:** Cuando **lees** datos de la BD (SELECT).

---

#### Clase `ProductoInsert` (para escritura)

```java
class ProductoInsert {
    private String nombre;
    private Long categoriaId;
    private Long marcaId;
    private Double precio;
    private Double costo;
    private Integer stock;
    // NO tiene 'id' porque será autogenerado

    public ProductoInsert(String nombre, Long categoriaId, Long marcaId,
                         Double precio, Double costo, Integer stock) {
        this.nombre = nombre;
        // ...
    }

    // Solo getters (sin setters, objeto inmutable)
}
```

**Cuándo usarla:** Cuando **insertas** un producto nuevo (INSERT).

**¿Por qué dos clases separadas?**

- `Producto` representa un producto **que ya existe** (con ID).
- `ProductoInsert` representa un producto **nuevo** (sin ID, será autogenerado).
- Evita confusión: no puedes insertar un producto con ID manualmente.

---

## 5. Consejos y mejoras sugeridas

### 🎯 Para el equipo: Buenas prácticas aplicadas

| Práctica                   | ¿Qué hicimos?                                | ¿Por qué es importante?                   |
| -------------------------- | -------------------------------------------- | ----------------------------------------- |
| **Mínimos privilegios**    | Usuario `app_user` solo con SELECT en vistas | Si hackean la app, no pueden borrar datos |
| **Separación de capas**    | DAO maneja toda la interacción con BD        | Código más mantenible y testeable         |
| **PreparedStatement**      | Siempre usamos `?` y `setXxx()`              | Previene SQL Injection                    |
| **Validaciones tempranas** | Checks en Java antes de INSERT               | Respuesta más rápida al usuario           |
| **Vistas seguras**         | Ocultamos costo y flags internos             | Protege información comercial             |
| **Documentación**          | Este documento para el equipo                | Todos entienden qué hace el código        |

---

### 💡 Mejoras opcionales (para aprender más)

#### 1. Logging de operaciones

**¿Qué es?** Registrar qué usuario hizo qué operación y cuándo.

**Cómo implementarlo:**

```java
import java.util.logging.Logger;

public class ProductoDAO {
    private static final Logger logger = Logger.getLogger(ProductoDAO.class.getName());

    public List<Producto> buscarPorNombre(String nombre) throws SQLException {
        logger.info("Buscando productos con nombre: " + nombre);
        // ... resto del código
        logger.info("Encontrados " + productos.size() + " productos");
        return productos;
    }
}
```

**Beneficio:** Si hay un problema, puedes revisar los logs y ver qué pasó.

---

#### 2. Manejo de excepciones más específico

**Actualmente:**

```java
public Producto buscarPorId(long id) throws SQLException {
    // Si hay error, lanza SQLException genérica
}
```

**Mejorado:**

```java
public Producto buscarPorId(long id) throws SQLException {
    try {
        // ... código del método
    } catch (SQLException e) {
        // Log del error con más contexto
        logger.severe("Error al buscar producto ID=" + id + ": " + e.getMessage());
        throw new SQLException("No se pudo buscar el producto con ID " + id, e);
    }
}
```

**Beneficio:** Mensajes de error más claros para debuggear.

---

#### 3. Conexión con pool (para aplicaciones reales)

**Actualmente:**

```java
// Se pasa una conexión abierta al DAO
ProductoDAO dao = new ProductoDAO(connection);
```

**Problema:** Si abres muchas conexiones, el servidor MySQL se queda sin recursos.

**Solución: Connection Pool**

```java
// Usar HikariCP (biblioteca popular)
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DatabaseManager {
    private static HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/producto_barras");
        config.setUsername("app_user");
        config.setPassword("TPIntegrador2025!");
        config.setMaximumPoolSize(10);  // Máximo 10 conexiones simultáneas

        dataSource = new HikariDataSource(config);
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}

// Uso:
try (Connection conn = DatabaseManager.getConnection()) {
    ProductoDAO dao = new ProductoDAO(conn);
    List<Producto> productos = dao.buscarPorNombre("Pradera");
}
```

**Beneficio:** Reutiliza conexiones en lugar de abrir/cerrar constantemente (más eficiente).

---

#### 4. Método para actualizar stock (transacciones)

**Caso de uso:** Cuando vendes un producto, debes restar del stock.

```java
public void actualizarStock(long productoId, int cantidadVendida) throws SQLException {
    connection.setAutoCommit(false);  // Iniciar transacción

    try {
        // 1. Verificar que haya suficiente stock
        String sqlCheck = "SELECT stock FROM producto WHERE id = ? FOR UPDATE";
        try (PreparedStatement stmt = connection.prepareStatement(sqlCheck)) {
            stmt.setLong(1, productoId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int stockActual = rs.getInt("stock");
                if (stockActual < cantidadVendida) {
                    throw new IllegalStateException("Stock insuficiente");
                }
            } else {
                throw new IllegalArgumentException("Producto no encontrado");
            }
        }

        // 2. Actualizar stock
        String sqlUpdate = "UPDATE producto SET stock = stock - ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sqlUpdate)) {
            stmt.setInt(1, cantidadVendida);
            stmt.setLong(2, productoId);
            stmt.executeUpdate();
        }

        connection.commit();  // Confirmar transacción

    } catch (Exception e) {
        connection.rollback();  // Revertir si hay error
        throw e;
    } finally {
        connection.setAutoCommit(true);
    }
}
```

**Conceptos nuevos:**

- **Transacción:** Varias operaciones que se ejecutan como una unidad (todas o ninguna).
- **FOR UPDATE:** Bloquea la fila para que otro usuario no la modifique al mismo tiempo.
- **COMMIT:** Confirma los cambios.
- **ROLLBACK:** Revierte los cambios si algo falla.

**Por qué es importante:** Evita condiciones de carrera (dos cajeros vendiendo el último producto al mismo tiempo).

---

### 🚨 Errores comunes a evitar

#### ❌ Error 1: Cerrar la conexión en el DAO

```java
// MAL ❌
public List<Producto> buscarPorNombre(String nombre) throws SQLException {
    // ...
    connection.close();  // ¡No hacer esto!
    return productos;
}
```

**Por qué está mal:** El DAO no debería cerrar la conexión que recibió. Eso es responsabilidad del código que la creó.

**Correcto:**

```java
// BIEN ✅
try (Connection conn = DriverManager.getConnection(...)) {
    ProductoDAO dao = new ProductoDAO(conn);
    dao.buscarPorNombre("Pradera");
    // La conexión se cierra automáticamente aquí (try-with-resources)
}
```

---

#### ❌ Error 2: Olvidar cerrar Statement/ResultSet

```java
// MAL ❌
public List<Producto> buscarPorNombre(String nombre) throws SQLException {
    PreparedStatement stmt = connection.prepareStatement(sql);
    ResultSet rs = stmt.executeQuery();
    // ... procesar resultados
    return productos;
    // stmt y rs nunca se cierran → fuga de memoria
}
```

**Correcto:**

```java
// BIEN ✅
try (PreparedStatement stmt = connection.prepareStatement(sql);
     ResultSet rs = stmt.executeQuery()) {
    // ... procesar resultados
}  // Se cierran automáticamente
```

---

#### ❌ Error 3: Hardcodear credenciales

```java
// MAL ❌
Connection conn = DriverManager.getConnection(
    "jdbc:mysql://localhost:3306/producto_barras",
    "root",  // ← Hardcodeado
    "password123"  // ← Hardcodeado y subido a GitHub 😱
);
```

**Correcto:**

```java
// BIEN ✅
// Leer de archivo de configuración o variables de entorno
Properties props = new Properties();
props.load(new FileInputStream("db.properties"));

Connection conn = DriverManager.getConnection(
    props.getProperty("db.url"),
    props.getProperty("db.user"),
    props.getProperty("db.password")
);
```

Archivo `db.properties` (NO subir a Git, agregar a `.gitignore`):

```properties
db.url=jdbc:mysql://localhost:3306/producto_barras
db.user=app_user
db.password=TPIntegrador2025!
```

---

### 📝 Checklist de seguridad para el equipo

Antes de entregar, verificar:

- [ ] ✅ Todos los métodos usan `PreparedStatement` (ninguno concatena strings)
- [ ] ✅ Las contraseñas NO están en el código (usar archivo .properties)
- [ ] ✅ El usuario `app_user` tiene SOLO los permisos necesarios
- [ ] ✅ Las vistas ocultan información sensible (costo, eliminado)
- [ ] ✅ Todos los `Statement`/`ResultSet` se cierran (usar try-with-resources)
- [ ] ✅ Las validaciones en Java coinciden con los CHECKs de MySQL
- [ ] ✅ Los errores se logean (para debugging)
- [ ] ✅ El código está comentado (para que el equipo entienda)

---

## Checklist de entrega

### ✅ Archivos SQL

- [x] `SQL_etapa4_seguridad.sql` - Script completo con:
  - [x] Creación de usuario `app_user`
  - [x] Permisos mínimos (GRANT SELECT en vistas)
  - [x] Vista `vw_producto_publico` (oculta costo y eliminado)
  - [x] Vista `vw_inventario_resumido` (solo agregados)
  - [x] 4 pruebas de integridad (PK, FK, CHECK x2)

### ✅ Código Java

- [x] `ProductoDAO.java` - Capa de acceso a datos con:

  - [x] Método `buscarPorNombre()` con PreparedStatement
  - [x] Método `buscarPorId()` con PreparedStatement
  - [x] Método `insertar()` con validaciones
  - [x] Método `buscarPorNombreInseguro()` (solo demostración)
  - [x] Clases `Producto` y `ProductoInsert`

- [x] `TestSQLInjection.java` - Test automatizado que demuestra:
  - [x] Búsqueda legítima funciona
  - [x] Ataque con PreparedStatement es bloqueado
  - [x] Ataque con concatenación es exitoso (vulnerable)

### ✅ Documentación

- [x] `ENTREGABLES_ETAPA4.md` - Este documento con:
  - [x] Explicaciones pedagógicas de cada componente
  - [x] Ejemplos de uso
  - [x] Consejos y mejoras sugeridas
  - [x] Errores comunes a evitar
  - [x] Analogías del mundo real

### 📸 Evidencias (capturas de pantalla)

- [ ] Salida de `SHOW GRANTS FOR 'app_user'@'localhost';`
- [ ] Consulta exitosa a `vw_producto_publico` con `app_user`
- [ ] Error al intentar `SELECT * FROM producto;` con `app_user`
- [ ] Error de PRIMARY KEY duplicada
- [ ] Error de FOREIGN KEY inválida
- [ ] Error de CHECK (margen negativo)
- [ ] Error de CHECK (nombre vacío)
- [ ] Salida completa de `TestSQLInjection.java`

---

## Interacción con IA

### 📝 Prompt del estudiante

> Estás interactuando con un estudiante de la materia Base de Datos I de una Tecnicatura Superior. El estudiante está resolviendo un Trabajo Práctico sobre modelos entidad–relación y modelo relacional. Ayudame a dejar documentado lo realizado en relacionado a seguridad e integridad para que los compañeros del grupo puedan entenderlo rápidamente y considerando que soy estudiante puedes aconsejarme sobre el código argumentado por qué cada cambio de manera que pueda aprender. Añade todo al markdown ENTREGABLES_ETAPA4 incluido este prompt y tu respuesta.

### 🤖 Respuesta de la IA (este documento)

Este documento fue generado como respuesta al prompt anterior. El objetivo es:

1. **Explicar de forma clara** qué hace cada componente del código
2. **Usar analogías del mundo real** para conceptos técnicos
3. **Mostrar ejemplos prácticos** de uso
4. **Sugerir mejoras** con justificación pedagógica
5. **Advertir sobre errores comunes** antes de que ocurran
6. **Documentar el proceso** para que el equipo pueda revisarlo

### 🎓 Aprendizajes clave de esta etapa

**Conceptos de seguridad:**

- ✅ Principio de mínimos privilegios
- ✅ Vistas como capa de abstracción
- ✅ PreparedStatement vs concatenación
- ✅ SQL Injection y cómo prevenirlo

**Conceptos de integridad:**

- ✅ PRIMARY KEY garantiza unicidad
- ✅ FOREIGN KEY garantiza relaciones válidas
- ✅ UNIQUE previene duplicados
- ✅ CHECK valida reglas de negocio

**Patrones de diseño:**

- ✅ DAO (Data Access Object) separa responsabilidades
- ✅ Try-with-resources maneja recursos automáticamente
- ✅ Validación en capas (Java + MySQL)

**Buenas prácticas:**

- ✅ Código comentado y documentado
- ✅ Nombres descriptivos de variables/métodos
- ✅ Manejo de excepciones apropiado
- ✅ Tests automatizados

---

### 📚 Referencias adicionales para aprender más

**Sobre seguridad:**

- OWASP Top 10: https://owasp.org/www-project-top-ten/
- SQL Injection ejemplos: https://www.w3schools.com/sql/sql_injection.asp

**Sobre JDBC y PreparedStatement:**

- Oracle JDBC Tutorial: https://docs.oracle.com/javase/tutorial/jdbc/
- PreparedStatement JavaDoc: https://docs.oracle.com/javase/8/docs/api/java/sql/PreparedStatement.html

**Sobre patrones de diseño:**

- DAO Pattern: https://www.baeldung.com/java-dao-pattern
- Connection Pooling: https://www.baeldung.com/java-connection-pooling

---

## 🎯 Conclusión

Esta etapa demuestra que implementamos:

1. **Seguridad en el acceso**: Usuario con privilegios limitados
2. **Seguridad en los datos**: Vistas que ocultan información sensible
3. **Seguridad en el código**: PreparedStatement previene SQL Injection
4. **Integridad referencial**: Constraints validados y funcionando
5. **Buenas prácticas**: Código limpio, comentado y testeable

El sistema está preparado para:

- ✅ Proteger información comercial (costos ocultos)
- ✅ Prevenir ataques (SQL Injection bloqueado)
- ✅ Garantizar consistencia de datos (constraints activos)
- ✅ Facilitar mantenimiento (arquitectura en capas)

---

**Preparado por:** [Nombres del equipo]  
**Revisado por:** Todos los miembros del equipo  
**Fecha:** Octubre 2025  
**Materia:** Base de Datos I - TPI Etapa 4

---

_Este documento forma parte de los entregables de la Etapa 4 y sirve como evidencia de comprensión de los conceptos de seguridad e integridad en bases de datos._
