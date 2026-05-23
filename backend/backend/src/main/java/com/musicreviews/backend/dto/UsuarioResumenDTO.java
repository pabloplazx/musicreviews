package com.musicreviews.backend.dto;

import com.musicreviews.backend.model.Usuario;

public record UsuarioResumenDTO(Long id, String username, String fotoPerfil) {

    public static UsuarioResumenDTO from(Usuario u) {
        return new UsuarioResumenDTO(u.getId(), u.getUsername(), u.getFotoPerfil());
    }
}
