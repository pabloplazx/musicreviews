USE musicreviews;

-- =====================================================
-- Limpiar artistas duplicados
-- =====================================================
-- Radiohead: conservar id=8 (datos reales Spotify), eliminar id=1 e id=32
-- The Beatles: conservar id=9, eliminar id=45
-- =====================================================

-- Reasignar álbumes de Radiohead duplicados al id=8
UPDATE album SET artista_id = 8 WHERE artista_id = 1;
UPDATE album SET artista_id = 8 WHERE artista_id = 32;

-- Reasignar álbumes de The Beatles duplicado al id=9
UPDATE album SET artista_id = 9 WHERE artista_id = 45;

-- Reasignar reseñas y favoritos si apuntan a los duplicados
UPDATE resena SET album_id = (SELECT id FROM album WHERE artista_id = 8 LIMIT 1) WHERE album_id IN (SELECT id FROM album WHERE artista_id IN (1, 32));
UPDATE favorito SET album_id = (SELECT id FROM album WHERE artista_id = 9 LIMIT 1) WHERE album_id IN (SELECT id FROM album WHERE artista_id = 45);

-- Actualizar género de Radiohead (id=8 tenía null)
UPDATE artista SET genero = 'alternative rock' WHERE id = 8;

-- Eliminar artistas duplicados
DELETE FROM artista WHERE id IN (1, 32, 45);

-- Verificar resultado
SELECT id, nombre, genero, foto IS NOT NULL as tiene_foto FROM artista WHERE nombre IN ('Radiohead', 'The Beatles');
