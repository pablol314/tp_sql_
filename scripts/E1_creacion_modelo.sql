-- =============================================================
-- TFI - Bases de Datos I (Etapa 1: Modelado y Constraints)
-- Dominio: Producto ↔ CódigoBarras (1→1), con catálogos Marca/Categoría
-- Motor objetivo: MySQL 8.0+ (probado con CHECK y REGEXP_LIKE)
-- =============================================================

-- Limpieza opcional (entorno de desarrollo)
DROP DATABASE IF EXISTS producto_barras;
CREATE DATABASE producto_barras
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
USE producto_barras;

-- =============================================================
-- TABLAS DE CATÁLOGO
-- =============================================================

CREATE TABLE categoria (
  id            BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  nombre        VARCHAR(80)  NOT NULL,
  descripcion   VARCHAR(255),
  eliminado     BOOLEAN      NOT NULL DEFAULT 0,
  -- Unicidad y dominio
  CONSTRAINT uq_categoria_nombre UNIQUE (nombre),
  CONSTRAINT chk_categoria_nombre_len CHECK (CHAR_LENGTH(nombre) BETWEEN 3 AND 80),
  CONSTRAINT chk_categoria_nombre_notblank CHECK (TRIM(nombre) <> '')
);

CREATE TABLE marca (
  id            BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  nombre        VARCHAR(80)  NOT NULL,
  eliminado     BOOLEAN      NOT NULL DEFAULT 0,
  CONSTRAINT uq_marca_nombre UNIQUE (nombre),
  CONSTRAINT chk_marca_nombre_len CHECK (CHAR_LENGTH(nombre) BETWEEN 2 AND 80),
  CONSTRAINT chk_marca_nombre_notblank CHECK (TRIM(nombre) <> '')
);

-- Semillas mínimas (para probar FKs)
INSERT INTO categoria (nombre, descripcion) VALUES
  ('Alimentos', 'Productos alimenticios'),
  ('Bebidas',   'Bebidas variadas'),
  ('Higiene',   'Cuidado personal');

INSERT INTO marca (nombre) VALUES
  ('Genérica'),
  ('Acme'),
  ('Premium');

-- =============================================================
-- TABLA DE NEGOCIO: PRODUCTO
-- - Cada producto pertenece a una marca y una categoría
-- - Se agrega stock para control de inventario
-- =============================================================
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

  -- Reglas de dominio
  CONSTRAINT chk_prod_nombre_len     CHECK (CHAR_LENGTH(nombre) BETWEEN 3 AND 120),
  CONSTRAINT chk_prod_nombre_notblank CHECK (TRIM(nombre) <> ''),
  CONSTRAINT chk_prod_precio_pos     CHECK (precio >= 0),
  CONSTRAINT chk_prod_costo_pos      CHECK (costo  >= 0),
  CONSTRAINT chk_prod_margen         CHECK (precio >= costo),
  CONSTRAINT chk_prod_fecha_rango    CHECK (fecha_alta BETWEEN DATE('2000-01-01') AND DATE('2100-12-31')),

  -- Claves foráneas
  CONSTRAINT fk_prod_categoria FOREIGN KEY (categoria_id)
    REFERENCES categoria(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT fk_prod_marca FOREIGN KEY (marca_id)
    REFERENCES marca(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT,

  -- Índices de búsqueda
  INDEX ix_prod_nombre (nombre),
  INDEX ix_prod_categoria (categoria_id),
  INDEX ix_prod_marca (marca_id)
);

-- =============================================================
-- TABLA 1→1: CODIGO_BARRAS (PK = FK a producto.id)
-- - Cada producto puede tener a lo sumo un código de barras
-- - Cada GTIN es único en todo el sistema
-- =============================================================
CREATE TABLE codigo_barras (
  producto_id   BIGINT UNSIGNED NOT NULL,
  gtin13        CHAR(13)        NOT NULL,
  tipo          ENUM('EAN13','EAN8','UPC','QR') NOT NULL DEFAULT 'EAN13',
  activo        BOOLEAN         NOT NULL DEFAULT 1,

  -- 1→1 real: PK = FK
  CONSTRAINT pk_cb PRIMARY KEY (producto_id),
  CONSTRAINT fk_cb_producto FOREIGN KEY (producto_id)
    REFERENCES producto(id)
    ON UPDATE RESTRICT
    ON DELETE CASCADE,          -- Si se borra el producto, se borra su código

  -- Unicidad y dominio del GTIN
  CONSTRAINT uq_cb_gtin UNIQUE (gtin13),
  CONSTRAINT chk_cb_gtin_digits CHECK (REGEXP_LIKE(gtin13, '^[0-9]{13}$')),
  CONSTRAINT chk_cb_tipo CHECK (tipo IN ('EAN13','EAN8','UPC','QR'))
);

-- =============================================================
-- VALIDACIÓN PRÁCTICA (2 OK + 2 ERROR con mensajes distintos)
-- Sugerencia: ejecutar cada bloque por separado para capturar mensajes
-- =============================================================

-- OK #1: Producto válido + CB único
INSERT INTO producto (nombre, categoria_id, marca_id, precio, costo, stock, fecha_alta)
VALUES ('Galletas de agua 100g', 1, 1, 1200.00, 800.00, 50, '2025-09-01');
INSERT INTO codigo_barras (producto_id, gtin13, tipo, activo)
VALUES (LAST_INSERT_ID(), '7791234567890', 'EAN13', 1);

-- ERROR #1 (UNIQUE): GTIN duplicado
INSERT INTO producto (nombre, categoria_id, marca_id, precio, costo, stock, fecha_alta)
VALUES ('Galletas de agua DUP', 1, 1, 1300.00, 900.00, 15, '2025-09-02');
-- Reutiliza el mismo GTIN → debe fallar por uq_cb_gtin
INSERT INTO codigo_barras (producto_id, gtin13, tipo, activo)
VALUES (LAST_INSERT_ID(), '7791234567890', 'EAN13', 1);

-- ERROR #2A (FK): categoría inexistente
INSERT INTO producto (nombre, categoria_id, marca_id, precio, costo, stock, fecha_alta)
VALUES ('Refresco inválido FK', 99999, 2, 500.00, 300.00, 10, '2025-09-03');

-- ERROR #2B (CHECK): margen negativo (precio < costo)
INSERT INTO producto (nombre, categoria_id, marca_id, precio, costo, stock, fecha_alta)
VALUES ('Refresco inválido MARGEN', 2, 2, 500.00, 700.00, 10, '2025-09-03');

-- OK #2: Producto válido + CB único
INSERT INTO producto (nombre, categoria_id, marca_id, precio, costo, stock, fecha_alta)
VALUES ('Jabón neutro 90g', 3, 3, 900.00, 500.00, 40, '2025-09-05');
INSERT INTO codigo_barras (producto_id, gtin13, tipo, activo)
VALUES (LAST_INSERT_ID(), '7791234567891', 'EAN13', 1);

-- =============================================================
-- NOTAS
-- - 1→1 real: se fuerza con PK=FK en codigo_barras (no puede haber más de un CB por producto).
-- - gtin13: se controla unicidad + 13 dígitos; (opcional) checksum EAN-13 puede validarse en app o con trigger.
-- - ON DELETE CASCADE en codigo_barras garantiza consistencia si se elimina el producto.
-- - ON DELETE/UPDATE RESTRICT en catálogos evita “huérfanos” por cambios de claves maestras.
-- - Fechas acotadas, strings no vacíos y stock no negativo mediante CHECK/UNSIGNED.
-- =============================================================
