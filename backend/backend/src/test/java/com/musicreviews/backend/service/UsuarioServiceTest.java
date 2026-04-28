package com.musicreviews.backend.service;

import com.musicreviews.backend.exception.AccesoDenegadoException;
import com.musicreviews.backend.model.Usuario;
import com.musicreviews.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Esto prueba la lógica de negocio de UsuarioService de forma aislada, sin BD ni Spring.
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    // Mock del repositorio — simula la BD sin conectarse a ella.
    @Mock
    private UsuarioRepository usuarioRepository;

    // Servicio real con el mock inyectado en lugar del repositorio real.
    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuario;

    // Esto prepara el objeto de prueba antes de cada test.
    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("pablo@test.com");
        usuario.setUsername("pablo");
        usuario.setBio("Bio de prueba");
    }

    // --- TESTS DE guardar() ---

    // Esto verifica que un usuario con datos únicos se guarda correctamente.
    @Test
    void guardar_conDatosUnicos_guardaYDevuelveUsuario() {
        when(usuarioRepository.existsByEmail("pablo@test.com")).thenReturn(false);
        when(usuarioRepository.existsByUsername("pablo")).thenReturn(false);
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        Usuario resultado = usuarioService.guardar(usuario);

        assertNotNull(resultado);
        assertEquals("pablo@test.com", resultado.getEmail());
        verify(usuarioRepository).save(usuario);
    }

    // Esto verifica que registrar un email ya existente lanza excepción.
    @Test
    void guardar_conEmailDuplicado_lanzaExcepcion() {
        when(usuarioRepository.existsByEmail("pablo@test.com")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> usuarioService.guardar(usuario));
        assertEquals("Ya existe un usuario con ese email", ex.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    // Esto verifica que registrar un username ya existente lanza excepción.
    @Test
    void guardar_conUsernameDuplicado_lanzaExcepcion() {
        when(usuarioRepository.existsByEmail("pablo@test.com")).thenReturn(false);
        when(usuarioRepository.existsByUsername("pablo")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> usuarioService.guardar(usuario));
        assertEquals("Ya existe un usuario con ese username", ex.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    // --- TESTS DE actualizar() ---

    @Test
    void actualizar_conIdExistente_actualizaCampos() {
        Usuario datosNuevos = new Usuario();
        datosNuevos.setUsername("pablo_nuevo");
        datosNuevos.setFotoPerfil("foto.jpg");
        datosNuevos.setBio("Nueva bio");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        Usuario resultado = usuarioService.actualizar(1L, datosNuevos, "pablo@test.com", false);

        assertEquals("pablo_nuevo", resultado.getUsername());
        assertEquals("Nueva bio", resultado.getBio());
    }

    @Test
    void actualizar_conIdInexistente_lanzaExcepcion() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> usuarioService.actualizar(99L, usuario, "pablo@test.com", false));
        assertEquals("Usuario no encontrado", ex.getMessage());
    }

    // Verificación de propiedad: nadie puede editar un perfil que no es el suyo (salvo ADMIN).
    @Test
    void actualizar_perfilDeOtro_lanzaAccesoDenegada() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class,
                () -> usuarioService.actualizar(1L, usuario, "otro@test.com", false));
        assertEquals("Solo puedes editar tu propio perfil", ex.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void actualizar_comoAdmin_permitidoEnPerfilDeOtro() {
        Usuario datosNuevos = new Usuario();
        datosNuevos.setUsername("editado_por_admin");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        assertDoesNotThrow(() -> usuarioService.actualizar(1L, datosNuevos, "admin@test.com", true));
    }

    // --- TESTS DE eliminar() ---

    @Test
    void eliminar_conIdExistente_eliminaCorrectamente() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        usuarioService.eliminar(1L, "pablo@test.com", false);

        verify(usuarioRepository).deleteById(1L);
    }

    @Test
    void eliminar_conIdInexistente_lanzaExcepcion() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> usuarioService.eliminar(99L, "pablo@test.com", false));
        assertEquals("Usuario no encontrado", ex.getMessage());
        verify(usuarioRepository, never()).deleteById(any());
    }

    @Test
    void eliminar_cuentaDeOtro_lanzaAccesoDenegada() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class,
                () -> usuarioService.eliminar(1L, "otro@test.com", false));
        assertEquals("Solo puedes borrar tu propia cuenta", ex.getMessage());
        verify(usuarioRepository, never()).deleteById(any());
    }

    @Test
    void eliminar_comoAdmin_permitidoEnCuentaDeOtro() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        assertDoesNotThrow(() -> usuarioService.eliminar(1L, "admin@test.com", true));
        verify(usuarioRepository).deleteById(1L);
    }
}
