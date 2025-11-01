-- schema.sql: definición definitiva de producto_barras
DROP SCHEMA IF EXISTS producto_barras;
CREATE SCHEMA producto_barras CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE producto_barras;

DROP TABLE IF EXISTS codigo_barras;
DROP TABLE IF EXISTS producto;
DROP TABLE IF EXISTS marca;
DROP TABLE IF EXISTS categoria;

CREATE TABLE categoria (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL UNIQUE,
    descripcion VARCHAR(255) NULL
) ENGINE=InnoDB;

CREATE TABLE marca (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL UNIQUE,
    sitio_web VARCHAR(255) NULL
) ENGINE=InnoDB;

CREATE TABLE producto (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(180) NOT NULL,
    categoria_id BIGINT UNSIGNED NOT NULL,
    marca_id BIGINT UNSIGNED NOT NULL,
    precio DECIMAL(12,2) NOT NULL,
    costo DECIMAL(12,2) NOT NULL,
    stock INT UNSIGNED NOT NULL DEFAULT 0,
    fecha_alta DATE NOT NULL DEFAULT (CURRENT_DATE),
    eliminado BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_prod_margen CHECK (precio >= costo),
    CONSTRAINT fk_prod_categoria FOREIGN KEY (categoria_id)
        REFERENCES categoria(id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_prod_marca FOREIGN KEY (marca_id)
        REFERENCES marca(id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE codigo_barras (
    producto_id BIGINT UNSIGNED PRIMARY KEY,
    gtin13 CHAR(13) NOT NULL UNIQUE,
    fecha_asignacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_codigo_producto FOREIGN KEY (producto_id)
        REFERENCES producto(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT chk_gtin_longitud CHECK (gtin13 REGEXP '^[0-9]{13}$')
) ENGINE=InnoDB;

CREATE INDEX ix_producto_categoria_precio ON producto (categoria_id, precio);
CREATE INDEX ix_producto_nombre ON producto (nombre);

-- vistas de apoyo para la aplicación y reportes
CREATE OR REPLACE VIEW vw_producto_detalle AS
SELECT p.id,
       p.nombre,
       c.nombre AS categoria,
       m.nombre AS marca,
       cb.gtin13 AS codigo_barras,
       p.precio,
       p.costo,
       p.stock,
       p.fecha_alta,
       p.eliminado
FROM producto p
JOIN categoria c ON p.categoria_id = c.id
JOIN marca m ON p.marca_id = m.id
JOIN codigo_barras cb ON cb.producto_id = p.id;
