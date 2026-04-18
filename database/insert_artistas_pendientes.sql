USE musicreviews;

-- =====================================================
-- Insertar artistas pendientes (solo si no existen ya)
-- =====================================================

INSERT INTO artista (nombre, genero, pais)
SELECT 'Bizarrap', 'trap latino', 'Argentina'
WHERE NOT EXISTS (SELECT 1 FROM artista WHERE nombre = 'Bizarrap');

INSERT INTO artista (nombre, genero, pais)
SELECT 'Izal', 'indie rock', 'España'
WHERE NOT EXISTS (SELECT 1 FROM artista WHERE nombre = 'Izal');

INSERT INTO artista (nombre, genero, pais)
SELECT 'Sidonie', 'indie pop', 'España'
WHERE NOT EXISTS (SELECT 1 FROM artista WHERE nombre = 'Sidonie');

INSERT INTO artista (nombre, genero, pais)
SELECT 'La Habitación Roja', 'indie rock', 'España'
WHERE NOT EXISTS (SELECT 1 FROM artista WHERE nombre = 'La Habitación Roja');

INSERT INTO artista (nombre, genero, pais)
SELECT 'El Columpio Asesino', 'indie rock', 'España'
WHERE NOT EXISTS (SELECT 1 FROM artista WHERE nombre = 'El Columpio Asesino');

INSERT INTO artista (nombre, genero, pais)
SELECT 'Supersubmarina', 'rock', 'España'
WHERE NOT EXISTS (SELECT 1 FROM artista WHERE nombre = 'Supersubmarina');

-- =====================================================
-- Álbumes de Bizarrap
-- =====================================================
INSERT INTO album (titulo, fecha_lanzamiento, genero, artista_id)
SELECT 'BZRP Music Sessions Vol. 52', '2023-01-12', 'trap latino', id FROM artista WHERE nombre = 'Bizarrap'
AND NOT EXISTS (SELECT 1 FROM album a WHERE a.titulo = 'BZRP Music Sessions Vol. 52' AND a.artista_id = (SELECT id FROM artista WHERE nombre = 'Bizarrap'));

INSERT INTO album (titulo, fecha_lanzamiento, genero, artista_id)
SELECT 'BZRP Music Sessions Vol. 53', '2023-02-16', 'trap latino', id FROM artista WHERE nombre = 'Bizarrap'
AND NOT EXISTS (SELECT 1 FROM album a WHERE a.titulo = 'BZRP Music Sessions Vol. 53' AND a.artista_id = (SELECT id FROM artista WHERE nombre = 'Bizarrap'));

INSERT INTO album (titulo, fecha_lanzamiento, genero, artista_id)
SELECT 'BZRP Music Sessions Vol. 57', '2023-07-13', 'trap latino', id FROM artista WHERE nombre = 'Bizarrap'
AND NOT EXISTS (SELECT 1 FROM album a WHERE a.titulo = 'BZRP Music Sessions Vol. 57' AND a.artista_id = (SELECT id FROM artista WHERE nombre = 'Bizarrap'));

-- =====================================================
-- Álbumes de Izal
-- =====================================================
INSERT INTO album (titulo, fecha_lanzamiento, genero, artista_id)
SELECT 'Magia y efectos especiales', '2013-03-19', 'indie rock', id FROM artista WHERE nombre = 'Izal'
AND NOT EXISTS (SELECT 1 FROM album a WHERE a.titulo = 'Magia y efectos especiales' AND a.artista_id = (SELECT id FROM artista WHERE nombre = 'Izal'));

INSERT INTO album (titulo, fecha_lanzamiento, genero, artista_id)
SELECT 'Agujeros de gusano', '2015-09-25', 'indie rock', id FROM artista WHERE nombre = 'Izal'
AND NOT EXISTS (SELECT 1 FROM album a WHERE a.titulo = 'Agujeros de gusano' AND a.artista_id = (SELECT id FROM artista WHERE nombre = 'Izal'));

INSERT INTO album (titulo, fecha_lanzamiento, genero, artista_id)
SELECT 'Copacabana (En el lado salvaje)', '2017-10-06', 'indie rock', id FROM artista WHERE nombre = 'Izal'
AND NOT EXISTS (SELECT 1 FROM album a WHERE a.titulo = 'Copacabana (En el lado salvaje)' AND a.artista_id = (SELECT id FROM artista WHERE nombre = 'Izal'));

INSERT INTO album (titulo, fecha_lanzamiento, genero, artista_id)
SELECT 'Los Ruidos de Fondo', '2020-10-23', 'indie rock', id FROM artista WHERE nombre = 'Izal'
AND NOT EXISTS (SELECT 1 FROM album a WHERE a.titulo = 'Los Ruidos de Fondo' AND a.artista_id = (SELECT id FROM artista WHERE nombre = 'Izal'));

-- =====================================================
-- Álbumes de Sidonie
-- =====================================================
INSERT INTO album (titulo, fecha_lanzamiento, genero, artista_id)
SELECT 'El incendio', '2003-01-01', 'indie pop', id FROM artista WHERE nombre = 'Sidonie'
AND NOT EXISTS (SELECT 1 FROM album a WHERE a.titulo = 'El incendio' AND a.artista_id = (SELECT id FROM artista WHERE nombre = 'Sidonie'));

INSERT INTO album (titulo, fecha_lanzamiento, genero, artista_id)
SELECT 'Los Hermanos', '2005-01-01', 'indie pop', id FROM artista WHERE nombre = 'Sidonie'
AND NOT EXISTS (SELECT 1 FROM album a WHERE a.titulo = 'Los Hermanos' AND a.artista_id = (SELECT id FROM artista WHERE nombre = 'Sidonie'));

INSERT INTO album (titulo, fecha_lanzamiento, genero, artista_id)
SELECT 'Fascinado', '2007-01-01', 'indie pop', id FROM artista WHERE nombre = 'Sidonie'
AND NOT EXISTS (SELECT 1 FROM album a WHERE a.titulo = 'Fascinado' AND a.artista_id = (SELECT id FROM artista WHERE nombre = 'Sidonie'));

INSERT INTO album (titulo, fecha_lanzamiento, genero, artista_id)
SELECT 'Pequeño', '2009-01-01', 'indie pop', id FROM artista WHERE nombre = 'Sidonie'
AND NOT EXISTS (SELECT 1 FROM album a WHERE a.titulo = 'Pequeño' AND a.artista_id = (SELECT id FROM artista WHERE nombre = 'Sidonie'));

INSERT INTO album (titulo, fecha_lanzamiento, genero, artista_id)
SELECT 'La Mala Educación', '2015-01-01', 'indie pop', id FROM artista WHERE nombre = 'Sidonie'
AND NOT EXISTS (SELECT 1 FROM album a WHERE a.titulo = 'La Mala Educación' AND a.artista_id = (SELECT id FROM artista WHERE nombre = 'Sidonie'));

INSERT INTO album (titulo, fecha_lanzamiento, genero, artista_id)
SELECT 'Mira!', '2018-01-01', 'indie pop', id FROM artista WHERE nombre = 'Sidonie'
AND NOT EXISTS (SELECT 1 FROM album a WHERE a.titulo = 'Mira!' AND a.artista_id = (SELECT id FROM artista WHERE nombre = 'Sidonie'));

-- =====================================================
-- Álbumes de La Habitación Roja
-- =====================================================
INSERT INTO album (titulo, fecha_lanzamiento, genero, artista_id)
SELECT 'Amor Dividido', '1997-01-01', 'indie rock', id FROM artista WHERE nombre = 'La Habitación Roja'
AND NOT EXISTS (SELECT 1 FROM album a WHERE a.titulo = 'Amor Dividido' AND a.artista_id = (SELECT id FROM artista WHERE nombre = 'La Habitación Roja'));

INSERT INTO album (titulo, fecha_lanzamiento, genero, artista_id)
SELECT 'Cuatro', '2003-01-01', 'indie rock', id FROM artista WHERE nombre = 'La Habitación Roja'
AND NOT EXISTS (SELECT 1 FROM album a WHERE a.titulo = 'Cuatro' AND a.artista_id = (SELECT id FROM artista WHERE nombre = 'La Habitación Roja'));

INSERT INTO album (titulo, fecha_lanzamiento, genero, artista_id)
SELECT 'El Pop No Muere', '2007-01-01', 'indie rock', id FROM artista WHERE nombre = 'La Habitación Roja'
AND NOT EXISTS (SELECT 1 FROM album a WHERE a.titulo = 'El Pop No Muere' AND a.artista_id = (SELECT id FROM artista WHERE nombre = 'La Habitación Roja'));

INSERT INTO album (titulo, fecha_lanzamiento, genero, artista_id)
SELECT 'Mar de Nubes', '2011-01-01', 'indie rock', id FROM artista WHERE nombre = 'La Habitación Roja'
AND NOT EXISTS (SELECT 1 FROM album a WHERE a.titulo = 'Mar de Nubes' AND a.artista_id = (SELECT id FROM artista WHERE nombre = 'La Habitación Roja'));

INSERT INTO album (titulo, fecha_lanzamiento, genero, artista_id)
SELECT 'Mientras Todo Pasa', '2018-01-01', 'indie rock', id FROM artista WHERE nombre = 'La Habitación Roja'
AND NOT EXISTS (SELECT 1 FROM album a WHERE a.titulo = 'Mientras Todo Pasa' AND a.artista_id = (SELECT id FROM artista WHERE nombre = 'La Habitación Roja'));

-- =====================================================
-- Álbumes de El Columpio Asesino
-- =====================================================
INSERT INTO album (titulo, fecha_lanzamiento, genero, artista_id)
SELECT 'Rodeo', '2008-01-01', 'indie rock', id FROM artista WHERE nombre = 'El Columpio Asesino'
AND NOT EXISTS (SELECT 1 FROM album a WHERE a.titulo = 'Rodeo' AND a.artista_id = (SELECT id FROM artista WHERE nombre = 'El Columpio Asesino'));

INSERT INTO album (titulo, fecha_lanzamiento, genero, artista_id)
SELECT 'Gritan', '2011-01-01', 'indie rock', id FROM artista WHERE nombre = 'El Columpio Asesino'
AND NOT EXISTS (SELECT 1 FROM album a WHERE a.titulo = 'Gritan' AND a.artista_id = (SELECT id FROM artista WHERE nombre = 'El Columpio Asesino'));

INSERT INTO album (titulo, fecha_lanzamiento, genero, artista_id)
SELECT 'Fantasmas', '2014-01-01', 'indie rock', id FROM artista WHERE nombre = 'El Columpio Asesino'
AND NOT EXISTS (SELECT 1 FROM album a WHERE a.titulo = 'Fantasmas' AND a.artista_id = (SELECT id FROM artista WHERE nombre = 'El Columpio Asesino'));

INSERT INTO album (titulo, fecha_lanzamiento, genero, artista_id)
SELECT 'Extrarradio', '2019-01-01', 'indie rock', id FROM artista WHERE nombre = 'El Columpio Asesino'
AND NOT EXISTS (SELECT 1 FROM album a WHERE a.titulo = 'Extrarradio' AND a.artista_id = (SELECT id FROM artista WHERE nombre = 'El Columpio Asesino'));

-- =====================================================
-- Álbumes de Supersubmarina
-- =====================================================
INSERT INTO album (titulo, fecha_lanzamiento, genero, artista_id)
SELECT 'Supersubmarina', '2009-01-01', 'rock', id FROM artista WHERE nombre = 'Supersubmarina'
AND NOT EXISTS (SELECT 1 FROM album a WHERE a.titulo = 'Supersubmarina' AND a.artista_id = (SELECT id FROM artista WHERE nombre = 'Supersubmarina'));

INSERT INTO album (titulo, fecha_lanzamiento, genero, artista_id)
SELECT 'Yellow & Blue', '2012-01-01', 'rock', id FROM artista WHERE nombre = 'Supersubmarina'
AND NOT EXISTS (SELECT 1 FROM album a WHERE a.titulo = 'Yellow & Blue' AND a.artista_id = (SELECT id FROM artista WHERE nombre = 'Supersubmarina'));

INSERT INTO album (titulo, fecha_lanzamiento, genero, artista_id)
SELECT 'Ahora', '2015-01-01', 'rock', id FROM artista WHERE nombre = 'Supersubmarina'
AND NOT EXISTS (SELECT 1 FROM album a WHERE a.titulo = 'Ahora' AND a.artista_id = (SELECT id FROM artista WHERE nombre = 'Supersubmarina'));

INSERT INTO album (titulo, fecha_lanzamiento, genero, artista_id)
SELECT 'Copito de Nieve', '2018-01-01', 'rock', id FROM artista WHERE nombre = 'Supersubmarina'
AND NOT EXISTS (SELECT 1 FROM album a WHERE a.titulo = 'Copito de Nieve' AND a.artista_id = (SELECT id FROM artista WHERE nombre = 'Supersubmarina'));
