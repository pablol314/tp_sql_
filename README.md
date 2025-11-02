# Trabajo Final Integrador — Gestión de Productos con Código de Barras

## 1. Descripción general
Este repositorio contiene la segunda parte del Trabajo Final Integrador para **Programación II** y **Bases de Datos I**. Se desarrolló una aplicación Java (JDK 17+) que gestiona un catálogo de **productos** y sus **códigos de barras**, vinculados mediante una relación **1→1 unidireccional**: la clase `Producto` mantiene una referencia obligatoria a `CodigoBarras`, mientras que `CodigoBarras` desconoce a su propietario. La solución emplea **JDBC sin ORM**, respeta el patrón **DAO + Service** y expone un **menú de consola** con operaciones CRUD envueltas en transacciones que ejecutan `commit` o `rollback` según el resultado.

## 2. Cumplimiento detallado de las consignas
La siguiente sección resume cómo se cubre cada requisito del enunciado, con referencias directas al código fuente y a los recursos incluidos.

### 2.1 Diseño y UML
- Se reservaron los archivos de recursos en `doc_resources/`. El diagrama UML se integrará en `doc_resources/uml_relacion_producto_codigo.png` (placeholder) y se vincula en la [Sección 6](#6-diagrama-uml) para incorporarlo apenas se finalice la imagen.
- Las dependencias entre paquetes se reflejan en la estructura bajo `java/src/main/java`, donde cada capa mantiene responsabilidades claras (ver [Sección 3](#3-arquitectura-y-paquetes)).

### 2.2 Entidades y dominio (A → B)
- `entities/Producto.java` define los atributos de negocio (`nombre`, `descripcion`, `categoriaId`, `marcaId`, `precio`, `costo`, `stock`, `fechaAlta`), el identificador `id`, la bandera de baja lógica `eliminado` y la referencia `private CodigoBarras codigoBarras;`, cumpliendo el requisito 1→1 unidireccional.
- `entities/CodigoBarras.java` utiliza `productoId` como clave primaria/foránea compartida, almacena el `gtin13`, el `tipo` (EAN13, UPC, etc.) y el estado `activo`, sin referenciar de vuelta a `Producto`.
- Ambos modelos ofrecen constructores completos y vacíos, getters/setters y un `toString()` legible para apoyo del menú.

### 2.3 Base de datos y scripts SQL
- `scripts/schema.sql` crea la base `producto_barras`, define tablas (`producto`, `codigo_barras`, catálogos auxiliares) e impone la relación 1→1 mediante una clave foránea única (`codigo_barras.producto_id` con `UNIQUE` y `ON DELETE CASCADE`).
- `scripts/sample_data.sql` carga datos reproducibles para categorías, marcas, productos y códigos, facilitando la puesta en marcha desde cero.
- `config/DatabaseConnection` (ver [Sección 3](#3-arquitectura-y-paquetes)) abre conexiones a MySQL reutilizando `database.properties` o overrides por variables/propiedades JVM.

### 2.4 Capa DAO (JDBC + PreparedStatement)
- `dao/GenericDao.java` declara las operaciones básicas (`crear`, `leer`, `leerTodos`, `actualizar`, `eliminar`) comunes a cada entidad.
- `dao/ProductoDao.java` y `dao/CodigoBarrasDao.java` implementan dichas operaciones con `PreparedStatement`, admiten una `Connection` inyectada externamente para compartir transacciones y reutilizan helpers de mapeo para componer entidades completas.
- Ambas clases incluyen búsquedas adicionales: por nombre (`ProductoDao`) y por GTIN (`CodigoBarrasDao`).

### 2.5 Capa Service y transacciones
- `service/ProductoService.java` y `service/CodigoBarrasService.java` validan entradas (campos obligatorios, reglas de negocio), abren transacciones con `setAutoCommit(false)` y aseguran `commit()`/`rollback()` en bloques `try/catch/finally`.
- La lógica impide asignar más de un código a un producto, evita duplicar GTIN y centraliza la baja lógica tanto para productos como para códigos.

### 2.6 Menú de consola y experiencia de uso
- `main/AppMenu.java` arranca desde `Main` y ofrece opciones CRUD completas para productos y códigos de barras, búsquedas específicas y manejo robusto de errores (parseo numérico, IDs inexistentes, entradas vacías).
- Cada opción delega en la capa `service`, capturando mensajes amigables para el usuario.

### 2.7 Entregables adicionales
- Scripts SQL: `schema.sql` + `sample_data.sql` ya disponibles.
- Video: el enlace público se documenta en la [Sección 7](#7-video-de-demostración) y debe subirse antes de la entrega definitiva.
- Informe PDF: queda pendiente (ver [Sección 8](#8-pendientes-de-la-entrega)).

## 3. Arquitectura y paquetes
La aplicación Java reside en `java/src/main/java` y sigue una arquitectura por capas:

- `config/`: `DatabaseConnection` obtiene los parámetros desde `database.properties`, admite overrides (`DB_PROPERTIES`, propiedades JVM) y expone `getConnection()` reutilizable.
- `entities/`: modelos `Producto` y `CodigoBarras` con atributos de negocio, `id`, banderas de baja lógica y referencia 1→1 desde `Producto`.
- `dao/`: `GenericDao`, `ProductoDao` y `CodigoBarrasDao` con operaciones CRUD, búsquedas específicas y soporte para conexiones externas.
- `service/`: reglas de negocio (`ProductoService`, `CodigoBarrasService`), validaciones de campos, control transaccional con `commit`/`rollback` y administración de la relación 1→1.
- `dto/` y `util/`: componentes auxiliares para encapsular solicitudes/respuestas y validar formatos (por ejemplo, longitud del GTIN).
- `main/`: `AppMenu` y `Main`, responsables de la interacción con el usuario y del ciclo de vida de la aplicación.

## 4. Requisitos previos
- **Java Development Kit (JDK) 17 o superior** (se recomienda 21 para alinearse con la consigna).
- **MySQL 8.0 o compatible**.
- Cliente de línea de comandos para `javac`, `java` y `mysql`.

## 5. Guía paso a paso para reproducir la aplicación
### 5.1 Preparar la base de datos
1. Crear la base y todas las tablas requeridas:
   ```bash
   mysql -u root -p < scripts/schema.sql
   ```
2. Insertar los datos de ejemplo (puede ejecutarse múltiples veces sin duplicados):
   ```bash
   mysql -u root -p < scripts/sample_data.sql
   ```
3. (Opcional) Crear un usuario dedicado ejecutando `scripts/E4_seguridad.sql` y ajustar los permisos necesarios.

### 5.2 Configurar las credenciales de conexión
`DatabaseConnection` utiliza `java/src/main/resources/database.properties`. Actualice el archivo o sobrescriba las claves mediante variables/propiedades antes de ejecutar la app:

```properties
jdbc.url=jdbc:mysql://localhost:3306/producto_barras
jdbc.user=app_user
jdbc.password=TPIntegrador2025!
jdbc.driverClassName=com.mysql.cj.jdbc.Driver
```

Orden de precedencia (de menor a mayor):
1. Archivo definido por `-Ddb.properties=<archivo>` o `DB_PROPERTIES`.
2. Overrides individuales (`-Ddb.jdbc.url=...`, `DB_JDBC_URL=...`, etc.).
3. Valores del archivo `database.properties` incluido en el repositorio.

Si sus credenciales difieren, actualice el archivo o utilice overrides antes de compilar/ejecutar.

### 5.3 Compilar la aplicación
```bash
cd java
find src/main/java -name "*.java" > sources.list
mkdir -p out
javac -d out @sources.list
cp -R src/main/resources/* out/
```

### 5.4 Ejecutar el menú de consola
```bash
java -cp out main.AppMenu
```

Notas:
- El menú imprime las opciones disponibles y continúa hasta que el usuario elige `0` (salir).
- Para recompilar después de cambios, repita `find` + `javac`. Puede eliminar `sources.list` cuando termine.

## 6. Diagrama UML
- El diagrama de clases que refleja la relación 1→1 (paquetes, atributos, métodos y dependencias) se integrará aquí:

  ![Diagrama UML Producto → CodigoBarras](doc_resources/uml_relacion_producto_codigo.png)

  > _Pendiente_: subir la imagen final al repositorio.

## 7. Video de demostración
Enlace al video (10–15 minutos) que presenta al equipo, explica la arquitectura y muestra el flujo CRUD con transacciones:

- **[Agregar URL del video aquí]**

## 8. Pendientes de la entrega
- Subir el diagrama UML definitivo en `doc_resources/` (ver [Sección 6](#6-diagrama-uml)).
- Incorporar el informe final en PDF (6–8 páginas) con la documentación solicitada.
- Actualizar esta sección cuando se completen los ítems anteriores.

## 9. Funcionalidades expuestas por el AppMenu
`AppMenu` ofrece las siguientes acciones, todas respaldadas por la capa `service` y con manejo robusto de entradas inválidas:

1. Crear producto y código de barras en una única transacción.
2. Actualizar producto y código asociado.
3. Dar de baja lógica un producto.
4. Listar todos los productos (incluyendo su código y estado).
5. Buscar productos por coincidencia en el nombre.
6. Buscar producto por GTIN.
7. Crear un código de barras para un producto existente.
8. Consultar código por ID de producto.
9. Listar todos los códigos de barras.
10. Actualizar un código de barras.
11. Baja lógica del código de barras.

Cada opción delega en `ProductoService` o `CodigoBarrasService`, que validan datos, orquestan transacciones (`commit`/`rollback`) y preservan la unicidad de la relación 1→1.

## 10. Estructura del repositorio
```
.
├── README.md
├── java/
│   ├── src/main/java/
│   │   ├── config/
│   │   ├── dao/
│   │   ├── dto/
│   │   ├── entities/
│   │   ├── main/
│   │   ├── service/
│   │   └── util/
│   └── src/main/resources/
├── scripts/
│   ├── schema.sql
│   ├── sample_data.sql
│   └── E1_... E5_...
└── doc_resources/
```

## 11. Próximos pasos sugeridos
- Publicar el enlace definitivo al video en la [Sección 7](#7-video-de-demostración).
- Agregar el diagrama UML y el informe PDF cuando estén terminados.
- (Opcional) Automatizar la compilación con Maven/Gradle y añadir pruebas unitarias para servicios/DAOs.
