CREATE DATABASE IF NOT EXISTS musicreviews;
USE musicreviews;

CREATE TABLE usuario (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    username       VARCHAR(50)  NOT NULL UNIQUE,
    email          VARCHAR(100) NOT NULL UNIQUE,
    password       VARCHAR(255) NOT NULL,
    foto_perfil    VARCHAR(255),
    bio            TEXT,
    fecha_registro DATETIME DEFAULT CURRENT_TIMESTAMP,
    rol            ENUM('USER', 'ADMIN') DEFAULT 'USER'
);

CREATE TABLE artista (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    nombre     VARCHAR(100) NOT NULL,
    foto       VARCHAR(255),
    biografia  TEXT,
    genero     VARCHAR(50),
    pais       VARCHAR(50)
);

CREATE TABLE album (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    titulo            VARCHAR(150) NOT NULL,
    portada           VARCHAR(255),
    fecha_lanzamiento DATE,
    genero            VARCHAR(50),
    descripcion       TEXT,
    artista_id        BIGINT NOT NULL,
    FOREIGN KEY (artista_id) REFERENCES artista(id)
);

CREATE TABLE resena (
    id                 BIGINT PRIMARY KEY AUTO_INCREMENT,
    puntuacion         TINYINT NOT NULL CHECK (puntuacion BETWEEN 1 AND 5),
    titulo             VARCHAR(150),
    contenido          TEXT NOT NULL,
    fecha_creacion     DATETIME DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    usuario_id         BIGINT NOT NULL,
    album_id           BIGINT NOT NULL,
    UNIQUE KEY unique_resena (usuario_id, album_id),
    FOREIGN KEY (usuario_id) REFERENCES usuario(id),
    FOREIGN KEY (album_id)   REFERENCES album(id)
);

CREATE TABLE favorito (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    usuario_id     BIGINT NOT NULL,
    album_id       BIGINT NOT NULL,
    fecha_agregado DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_favorito (usuario_id, album_id),
    FOREIGN KEY (usuario_id) REFERENCES usuario(id),
    FOREIGN KEY (album_id)   REFERENCES album(id)
);
