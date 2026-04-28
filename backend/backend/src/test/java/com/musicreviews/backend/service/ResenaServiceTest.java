package com.musicreviews.backend.service;

import com.musicreviews.backend.exception.AccesoDenegadoException;
import com.musicreviews.backend.model.Album;
import com.musicreviews.backend.model.Resena;
import com.musicreviews.backend.model.Usuario;
import com.musicreviews.backend.repository.ResenaRepository;
import com.musicreviews.backend.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Esto prueba la lógica de negocio de ResenaService de forma aislada, sin BD ni Spring.
// Se usan mocks (objetos falsos) para simular el comportamiento del repositorio.
@ExtendWith(MockitoExtension.class)
class ResenaServiceTest {

    private static final String EMAIL = "test@test.com";
    private static final String OTRO_EMAIL = "otro@test.com";

    @Mock
    private ResenaRepository resenaRepository;

    // El servicio consulta UsuarioRepository en crear() para obtener el id del usuario
    // autenticado a partir del email del JWT (verificación de propiedad).
    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private ResenaService resenaService;

    private Resena resena;
    private Usuario usuario;
    private Album album;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail(EMAIL);

        album = new Album();
        album.setId(1L);

        resena = new Resena();
        resena.setId(1L);
        resena.setUsuario(usuario);
        resena.setAlbum(album);
        resena.setPuntuacion(4.0);
        resena.setComentario("Muy buen álbum");
    }

    // --- TESTS DE crear() ---

    @Test
    void crear_conDatosValidos_guardaYDevuelveResena() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(resenaRepository.existsByUsuarioIdAndAlbumId(1L, 1L)).thenReturn(false);
        when(resenaRepository.save(resena)).thenReturn(resena);

        Resena resultado = resenaService.crear(resena, EMAIL, false);

        assertNotNull(resultado);
        assertEquals(4.0, resultado.getPuntuacion());
        verify(resenaRepository).save(resena);
    }

    @Test
    void crear_conPuntuacionCero_lanzaExcepcion() {
        resena.setPuntuacion(0.0);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> resenaService.crear(resena, EMAIL, false));
        assertEquals("La puntuación debe estar entre 0.5 y 5", ex.getMessage());
        verify(resenaRepository, never()).save(any());
    }

    @Test
    void crear_conPuntuacionSeis_lanzaExcepcion() {
        resena.setPuntuacion(6.0);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> resenaService.crear(resena, EMAIL, false));
        assertEquals("La puntuación debe estar entre 0.5 y 5", ex.getMessage());
        verify(resenaRepository, never()).save(any());
    }

    @Test
    void crear_conResenaYaExistente_lanzaExcepcion() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(resenaRepository.existsByUsuarioIdAndAlbumId(1L, 1L)).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> resenaService.crear(resena, EMAIL, false));
        assertEquals("El usuario ya ha reseñado este álbum", ex.getMessage());
        verify(resenaRepository, never()).save(any());
    }

    // Verificación de propiedad: si maría intenta crear una reseña con usuario.id de otro,
    // se lanza AccesoDenegadoException.
    @Test
    void crear_conIdDeOtroUsuario_lanzaAccesoDenegada() {
        Usuario otro = new Usuario();
        otro.setId(2L);
        otro.setEmail(OTRO_EMAIL);
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(otro));
        // El email del token (EMAIL) corresponde a otro usuario (id=2), pero la reseña tiene
        // usuario.id=1 → suplantación detectada.

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class,
                () -> resenaService.crear(resena, EMAIL, false));
        assertEquals("Solo puedes crear reseñas en tu propio nombre", ex.getMessage());
        verify(resenaRepository, never()).save(any());
    }

    // ADMIN puede crear en nombre de otro (excepción explícita para moderación).
    @Test
    void crear_comoAdmin_permitidoEnNombreDeOtro() {
        when(resenaRepository.existsByUsuarioIdAndAlbumId(1L, 1L)).thenReturn(false);
        when(resenaRepository.save(resena)).thenReturn(resena);

        Resena resultado = resenaService.crear(resena, "admin@test.com", true);

        assertNotNull(resultado);
        verify(usuarioRepository, never()).findByEmail(any()); // como esAdmin no se consulta
    }

    // --- TESTS DE actualizar() ---

    @Test
    void actualizar_conDatosValidos_actualizaYDevuelveResena() {
        Resena datosNuevos = new Resena();
        datosNuevos.setPuntuacion(5.0);
        datosNuevos.setComentario("Obra maestra");

        when(resenaRepository.findById(1L)).thenReturn(Optional.of(resena));
        when(resenaRepository.save(resena)).thenReturn(resena);

        Resena resultado = resenaService.actualizar(1L, datosNuevos, EMAIL, false);

        assertEquals(5.0, resultado.getPuntuacion());
        assertEquals("Obra maestra", resultado.getComentario());
    }

    @Test
    void actualizar_conPuntuacionInvalida_lanzaExcepcion() {
        Resena datosNuevos = new Resena();
        datosNuevos.setPuntuacion(7.0);

        when(resenaRepository.findById(1L)).thenReturn(Optional.of(resena));

        assertThrows(RuntimeException.class,
                () -> resenaService.actualizar(1L, datosNuevos, EMAIL, false));
        verify(resenaRepository, never()).save(any());
    }

    @Test
    void actualizar_conIdInexistente_lanzaExcepcion() {
        when(resenaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> resenaService.actualizar(99L, resena, EMAIL, false));
    }

    // Verificación de propiedad: si otro intenta editar la reseña de maría, AccesoDenegada.
    @Test
    void actualizar_deOtroUsuario_lanzaAccesoDenegada() {
        when(resenaRepository.findById(1L)).thenReturn(Optional.of(resena));

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class,
                () -> resenaService.actualizar(1L, resena, OTRO_EMAIL, false));
        assertEquals("Solo puedes editar tus propias reseñas", ex.getMessage());
        verify(resenaRepository, never()).save(any());
    }

    // ADMIN puede editar reseñas ajenas.
    @Test
    void actualizar_comoAdmin_permitidoEnResenaDeOtro() {
        Resena datosNuevos = new Resena();
        datosNuevos.setPuntuacion(3.0);
        datosNuevos.setComentario("Moderado");

        when(resenaRepository.findById(1L)).thenReturn(Optional.of(resena));
        when(resenaRepository.save(resena)).thenReturn(resena);

        assertDoesNotThrow(() -> resenaService.actualizar(1L, datosNuevos, OTRO_EMAIL, true));
    }

    // --- TESTS DE eliminar() ---

    @Test
    void eliminar_conIdExistente_eliminaCorrectamente() {
        when(resenaRepository.findById(1L)).thenReturn(Optional.of(resena));

        resenaService.eliminar(1L, EMAIL, false);

        verify(resenaRepository).deleteById(1L);
    }

    @Test
    void eliminar_conIdInexistente_lanzaExcepcion() {
        when(resenaRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> resenaService.eliminar(99L, EMAIL, false));
        assertEquals("Reseña no encontrada", ex.getMessage());
        verify(resenaRepository, never()).deleteById(any());
    }

    @Test
    void eliminar_deOtroUsuario_lanzaAccesoDenegada() {
        when(resenaRepository.findById(1L)).thenReturn(Optional.of(resena));

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class,
                () -> resenaService.eliminar(1L, OTRO_EMAIL, false));
        assertEquals("Solo puedes borrar tus propias reseñas", ex.getMessage());
        verify(resenaRepository, never()).deleteById(any());
    }

    @Test
    void eliminar_comoAdmin_permitidoEnResenaDeOtro() {
        when(resenaRepository.findById(1L)).thenReturn(Optional.of(resena));

        assertDoesNotThrow(() -> resenaService.eliminar(1L, OTRO_EMAIL, true));
        verify(resenaRepository).deleteById(1L);
    }

    // --- TESTS DE consultas ---

    @Test
    void obtenerPorAlbum_devuelveLista() {
        when(resenaRepository.findByAlbumId(1L)).thenReturn(List.of(resena));

        List<Resena> resultado = resenaService.obtenerPorAlbum(1L);

        assertEquals(1, resultado.size());
        verify(resenaRepository).findByAlbumId(1L);
    }

    @Test
    void obtenerPorUsuario_devuelveLista() {
        when(resenaRepository.findByUsuarioId(1L)).thenReturn(List.of(resena));

        List<Resena> resultado = resenaService.obtenerPorUsuario(1L);

        assertEquals(1, resultado.size());
        verify(resenaRepository).findByUsuarioId(1L);
    }
}
