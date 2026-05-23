package com.musicreviews.backend.service;

import com.musicreviews.backend.dto.UsuarioResumenDTO;
import com.musicreviews.backend.exception.RecursoNoEncontradoException;
import com.musicreviews.backend.exception.ReglaNegocioException;
import com.musicreviews.backend.model.Seguimiento;
import com.musicreviews.backend.model.Usuario;
import com.musicreviews.backend.repository.SeguimientoRepository;
import com.musicreviews.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SeguimientoService {

    private final SeguimientoRepository seguimientoRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public void seguir(Long seguidoId, String emailSeguidor) {
        Usuario seguidor = usuarioRepository.findByEmail(emailSeguidor)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        Usuario seguido = usuarioRepository.findById(seguidoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        if (seguidor.getId().equals(seguidoId)) {
            throw new ReglaNegocioException("No puedes seguirte a ti mismo");
        }
        if (seguimientoRepository.existsBySeguidorIdAndSeguidoId(seguidor.getId(), seguidoId)) {
            throw new ReglaNegocioException("Ya sigues a este usuario");
        }

        Seguimiento s = new Seguimiento();
        s.setSeguidor(seguidor);
        s.setSeguido(seguido);
        seguimientoRepository.save(s);
    }

    @Transactional
    public void dejarDeSeguir(Long seguidoId, String emailSeguidor) {
        Usuario seguidor = usuarioRepository.findByEmail(emailSeguidor)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        if (!seguimientoRepository.existsBySeguidorIdAndSeguidoId(seguidor.getId(), seguidoId)) {
            throw new ReglaNegocioException("No sigues a este usuario");
        }
        seguimientoRepository.deleteBySeguidorIdAndSeguidoId(seguidor.getId(), seguidoId);
    }

    public boolean estaSiguiendo(Long seguidoId, String emailSeguidor) {
        return usuarioRepository.findByEmail(emailSeguidor)
                .map(u -> seguimientoRepository.existsBySeguidorIdAndSeguidoId(u.getId(), seguidoId))
                .orElse(false);
    }

    public List<UsuarioResumenDTO> getSeguidores(Long usuarioId) {
        return seguimientoRepository.findBySeguidoId(usuarioId).stream()
                .map(s -> UsuarioResumenDTO.from(s.getSeguidor()))
                .toList();
    }

    public List<UsuarioResumenDTO> getSiguiendo(Long usuarioId) {
        return seguimientoRepository.findBySeguidorId(usuarioId).stream()
                .map(s -> UsuarioResumenDTO.from(s.getSeguido()))
                .toList();
    }

    public long contarSeguidores(Long usuarioId) {
        return seguimientoRepository.countBySeguidoId(usuarioId);
    }

    public long contarSiguiendo(Long usuarioId) {
        return seguimientoRepository.countBySeguidorId(usuarioId);
    }
}
