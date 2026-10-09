package com.example.banco.mapper;

import com.example.banco.dto.response.UsuarioResponse;
import com.example.banco.entity.Usuario;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {

    /** Nunca copia la contraseña. */
    public UsuarioResponse aRespuesta(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getClienteId(), usuario.getCorreo(),
                usuario.isActivo(), usuario.getFechaCreacion(), usuario.getFechaActualizacion());
    }
}
