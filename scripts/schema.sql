-- =============================================================
-- Esquema base "producto_barras"
-- Motor objetivo: MySQL 8.0+ (ejecutar con cliente que soporte CHECK)
-- Orden recomendado de ejecución:
--   1) scripts/schema.sql
--   2) scripts/sample_data.sql
-- =============================================================

CREATE DATABASE IF NOT EXISTS producto_barras
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
USE producto_barras;

-- =============================================================
-- TABLAS DE CATÁLOGO
-- =============================================================

CREATE TABLE IF NOT EXISTS categoria (
  id            BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  nombre        VARCHAR(80)  NOT NULL,
  descripcion   VARCHAR(255),
  eliminado     BOOLEAN      NOT NULL DEFAULT 0,
  CONSTRAINT uq_categoria_nombre UNIQUE (nombre),
  CONSTRAINT chk_categoria_nombre_len CHECK (CHAR_LENGTH(nombre) BETWEEN 3 AND 80),
  CONSTRAINT chk_categoria_nombre_notblank CHECK (TRIM(nombre) <> '')
);

CREATE TABLE IF NOT EXISTS marca (
  id            BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  nombre        VARCHAR(80)  NOT NULL,
  eliminado     BOOLEAN      NOT NULL DEFAULT 0,
  CONSTRAINT uq_marca_nombre UNIQUE (nombre),
  CONSTRAINT chk_marca_nombre_len CHECK (CHAR_LENGTH(nombre) BETWEEN 2 AND 80),
  CONSTRAINT chk_marca_nombre_notblank CHECK (TRIM(nombre) <> '')
);

-- =============================================================
-- TABLA DE NEGOCIO: PRODUCTO
-- =============================================================

CREATE TABLE IF NOT EXISTS producto (
  id            BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  nombre        VARCHAR(120) NOT NULL,
  descripcion   VARCHAR(255),
  categoria_id  BIGINT UNSIGNED NOT NULL,
  marca_id      BIGINT UNSIGNED NOT NULL,
  precio        DECIMAL(12,2) NOT NULL,
  costo         DECIMAL(12,2) NOT NULL,
  stock         INT UNSIGNED  NOT NULL DEFAULT 0,
  fecha_alta    DATE NOT NULL DEFAULT (CURRENT_DATE),
  eliminado     BOOLEAN NOT NULL DEFAULT 0,

  CONSTRAINT chk_prod_nombre_len      CHECK (CHAR_LENGTH(nombre) BETWEEN 3 AND 120),
  CONSTRAINT chk_prod_nombre_notblank CHECK (TRIM(nombre) <> ''),
  CONSTRAINT chk_prod_descripcion_len CHECK (descripcion IS NULL OR CHAR_LENGTH(descripcion) <= 255),
  CONSTRAINT chk_prod_precio_pos      CHECK (precio >= 0),
  CONSTRAINT chk_prod_costo_pos       CHECK (costo  >= 0),
  CONSTRAINT chk_prod_margen          CHECK (precio >= costo),
  CONSTRAINT chk_prod_fecha_rango     CHECK (fecha_alta BETWEEN DATE('2000-01-01') AND DATE('2100-12-31')),

  CONSTRAINT fk_prod_categoria FOREIGN KEY (categoria_id)
    REFERENCES categoria(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT fk_prod_marca FOREIGN KEY (marca_id)
    REFERENCES marca(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT,

  INDEX ix_prod_nombre (nombre),
  INDEX ix_prod_categoria (categoria_id),
  INDEX ix_prod_marca (marca_id)
);

-- =============================================================
-- TABLA 1→1: CODIGO_BARRAS (PK = FK a producto.id)
-- =============================================================

CREATE TABLE IF NOT EXISTS codigo_barras (
  producto_id   BIGINT UNSIGNED NOT NULL,
  gtin13        CHAR(13)        NOT NULL,
  tipo          ENUM('EAN13','EAN8','UPC','QR') NOT NULL DEFAULT 'EAN13',
  activo        BOOLEAN         NOT NULL DEFAULT 1,

  CONSTRAINT pk_cb PRIMARY KEY (producto_id),
  CONSTRAINT fk_cb_producto FOREIGN KEY (producto_id)
    REFERENCES producto(id)
    ON UPDATE RESTRICT
    ON DELETE CASCADE,

  CONSTRAINT uq_cb_gtin UNIQUE (gtin13),
  CONSTRAINT chk_cb_gtin_digits CHECK (REGEXP_LIKE(gtin13, '^[0-9]{13}$')),
  CONSTRAINT chk_cb_tipo CHECK (tipo IN ('EAN13','EAN8','UPC','QR'))
);

-- =============================================================
-- FIN DEL ESQUEMA BASE
-- =============================================================
