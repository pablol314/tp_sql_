# Trabajo Final Integrador — Catálogo Producto → Código de Barras

## Introducción

La solución final integra **MySQL 8** con una aplicación de consola escrita en **Java (JDK 17)** para administrar el dominio de 
negocio *Producto → Código de Barras (GTIN-13)*. El sistema permite dar de alta productos, asociarles un identificador único de 
código de barras y operar sobre el catálogo mediante un menú CRUD orientado a usuarios de backoffice. El modelo relacional `prod
ucto_barras` aplica reglas de integridad, vistas de apoyo y controles de seguridad, mientras que la aplicación Java utiliza `JDBC
` con consultas parametrizadas para evitar inyección SQL y garantizar operaciones transaccionales consistentes.

## Requisitos previos

- **Java Development Kit (JDK) 17** o superior.
- **MySQL Server 8.0** (o compatible con InnoDB y `CHECK` constraints).
- **MySQL Connector/J** 8.x (`mysql-connector-j-<versión>.jar`) disponible en el classpath al compilar/ejecutar.
- Usuario del sistema con permisos para crear esquemas, tablas y usuarios en MySQL.

> Nota: Las instrucciones asumen un entorno Linux/macOS. En Windows, sustituir `:` por `;` en las rutas del classpath.

## Preparación de la base de datos

1. **Crear el esquema y tablas principales**
   ```bash
   mysql -u root -p < scripts/schema.sql
   ```
   El script define el esquema `producto_barras`, las tablas `categoria`, `marca`, `producto` y `codigo_barras`, así como la vista `
   vw_producto_detalle` utilizada por reportes y consultas administrativas.

2. **Cargar datos de ejemplo**
   ```bash
   mysql -u root -p < scripts/sample_data.sql
   ```
   Este set crea categorías, marcas y cuatro productos iniciales para validar rápidamente el flujo del menú.

3. **Configurar el usuario de aplicación (opcional pero recomendado)**
   El script [`scripts/E4_seguridad.sql`](scripts/E4_seguridad.sql) crea el usuario `app_user` con permisos mínimos sobre las vista
   s y tablas necesarias. Ejecute:
   ```bash
   mysql -u root -p < scripts/E4_seguridad.sql
   ```

## Configuración de credenciales para Java

1. Copiar el archivo de ejemplo e indicar sus credenciales reales:
   ```bash
   cd java
   cp database.properties.example database.properties
   ```
2. Editar `database.properties` con la URL JDBC, usuario y contraseña válidos. El valor por defecto apunta a `jdbc:mysql://local
   host:3306/producto_barras?useSSL=false&serverTimezone=UTC` con el usuario `app_user` definido en el paso anterior.

## Aplicación de consola `AppMenu`

### Compilación

Desde la carpeta `java/`, compile todas las clases agregando el conector JDBC al classpath:
```bash
cd java
javac -cp .:mysql-connector-j-8.3.0.jar AppMenu.java ProductoDao.java Producto.java DatabaseConfig.java
```

### Ejecución

Ejecute el menú interactivo con las mismas dependencias en el classpath:
```bash
java -cp .:mysql-connector-j-8.3.0.jar AppMenu
```

### Opciones del menú

1. **Alta de producto**: solicita nombre, identificadores de categoría y marca existentes, precio, costo, stock y código de barras 
   GTIN-13. Inserta la información en `producto` y `codigo_barras` dentro de una transacción.
2. **Listar productos activos**: muestra los productos no eliminados con categoría, marca, GTIN, precio y stock actual.
3. **Actualizar precio y stock**: permite ajustar ambos campos mediante el `id` del producto. Valida que el producto esté activo.
4. **Baja lógica de producto**: marca el producto como `eliminado = TRUE` manteniendo el historial y el vínculo con el código de bar
   ras.
5. **Buscar por código de barras**: recupera un producto (activo o no) a partir del GTIN-13, indicando su estado y datos comerciales.
6. **Buscar por nombre** *(búsqueda adicional)*: filtra por coincidencias parciales en el nombre utilizando `LIKE` parametrizado.
0. **Salir**: finaliza la aplicación.

## Presentación

- **Video demostrativo (10–15 min)**: [https://youtu.be/Jd2kX0XzZ9E](https://youtu.be/Jd2kX0XzZ9E)
- **Diagrama UML / DER final**: [`doc_resources/DER.png`](doc_resources/DER.png)
- **Informe técnico final**: [`doc_resources/informe_final.md`](doc_resources/informe_final.md) *(se actualizará con la versión apro
bada)*

## Anexos y evidencias

Las etapas intermedias, scripts auxiliares y mediciones históricas se conservan como referencia:

- [`scripts/E1_creacion_modelo.sql`](scripts/E1_creacion_modelo.sql): DER inicial y reglas de integridad.
- [`scripts/E2_carga_masiva_indice_mediciones.sql`](scripts/E2_carga_masiva_indice_mediciones.sql): generación de datos y ensayos d
e índices.
- [`doc_resources/evidencias_rendimiento.md`](doc_resources/evidencias_rendimiento.md): bitácora de mediciones y pruebas de rendim
iento.
- [`scripts/E3_consultas_vistas.sql`](scripts/E3_consultas_vistas.sql): consultas analíticas de soporte.
- [`scripts/E5_concurrencia_transacciones.sql`](scripts/E5_concurrencia_transacciones.sql): escenarios de concurrencia y bloqueo.

Estas evidencias complementan la memoria técnica y sirven como respaldo de decisiones de diseño y pruebas.
