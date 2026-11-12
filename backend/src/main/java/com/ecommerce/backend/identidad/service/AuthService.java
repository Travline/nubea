package com.ecommerce.backend.identidad.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.backend.identidad.dto.RegistroUsuarioRequest;
import com.ecommerce.backend.identidad.dto.UsuarioResponse;
import com.ecommerce.backend.identidad.mapper.UsuarioMapper;
import com.ecommerce.backend.identidad.model.NombreRol;
import com.ecommerce.backend.identidad.model.ProveedorIdentidad;
import com.ecommerce.backend.identidad.model.Rol;
import com.ecommerce.backend.identidad.model.TipoDocumento;
import com.ecommerce.backend.identidad.model.UsuarioIdentidad;
import com.ecommerce.backend.identidad.model.UsuarioPlataforma;
import com.ecommerce.backend.identidad.repository.RolRepository;
import com.ecommerce.backend.identidad.repository.TipoDocumentoRepository;
import com.ecommerce.backend.identidad.repository.UsuarioIdentidadRepository;
import com.ecommerce.backend.identidad.repository.UsuarioPlataformaRepository;
import com.ecommerce.backend.shared.exception.RecursosDuplicadosException;
import com.ecommerce.backend.shared.exception.ReglaNegocioException;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class AuthService {
    private final UsuarioPlataformaRepository usuarioRepository;
    private final UsuarioIdentidadRepository identidadRepository;
    private final RolRepository rolRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final PasswordEncoder passwordEncoder;  // el bean esta con Argon2 de SecurityConfig 

    @Transactional
    public UsuarioResponse registrar(RegistroUsuarioRequest req)
    {
        String email = req.correoElectronico().trim().toLowerCase();

        if (usuarioRepository.existsByEmail(email)){
            throw new RecursosDuplicadosException("Ya existe una cuenta asociada a ese Correo Electronico "+ email);
        }

        UsuarioPlataforma usuario = UsuarioPlataforma.builder()
            .nombres(req.nombres().trim())
            .apellidoPaterno(limpiar(req.apellidoPaterno()))
            .apellidoMaterno(limpiar(req.apellidoMaterno()))
            .email(email)
            .telefono(limpiar(req.telefono()))
            .build();
        aplicarDocumento(usuario, req);

        //Todo usuario nuevo recibe el rol CLIENTE
        Rol cliente = rolRepository.findByNombre(NombreRol.CLIENTE)
                .orElseThrow(()-> new IllegalStateException("Falta nel rol CLIENTE en la Base de datos"));
        usuario.getRoles().add(cliente);
       
        usuarioRepository.save(usuario);
        
        //Identidad Local con la contraseña cifrada por Argon2id
        UsuarioIdentidad identidad = UsuarioIdentidad.builder()
            .usuario(usuario)
            .proveedor(ProveedorIdentidad.LOCAL)
            .passwordHash(passwordEncoder.encode(req.password()))
            .build();
        identidadRepository.save(identidad);

        return UsuarioMapper.toResponse(usuario);
    }

    private void aplicarDocumento(UsuarioPlataforma usuario, RegistroUsuarioRequest req){
        String codigo = limpiar(req.tipoDocumento());
        String numero = limpiar(req.numeroDocumento());

        if (codigo == null && numero == null)
            return;
        if (codigo == null || numero ==  null){
            throw new ReglaNegocioException("Tipo y numero de documento van juntos");
        }
        TipoDocumento tipo = tipoDocumentoRepository.findByCodigo(codigo.toUpperCase())
           .orElseThrow(()-> new ReglaNegocioException("Tipo de documento no valido: " + codigo));
        
        String pais;
        switch (tipo.getCodigo()) {
            case "DNI" -> {
              if (!numero.matches("\\d{8}")) throw new ReglaNegocioException("El DNI debe tener 8 digitos");
               pais = "PE";
            }

            case "CE" -> {
              if (!numero.matches("[A-Za-z0-9]{9,12}")) throw new ReglaNegocioException("Carne de extranjeria no valido");
               pais = "PE";
            }
            default -> {
                if(!numero.matches("[A-Za-z0-9]{6,12}")) throw new ReglaNegocioException("Pasaport no valido");
                    pais = req.paisEmisorDocumento();
                    if (pais == null) throw new ReglaNegocioException("Para pasaporte indica el pais emisor (ej. CO)"); 
            }     
        }

        usuario.setTipoDocumento(tipo);
        usuario.setNumeroDocumento(numero.toUpperCase());
        usuario.setPaisEmisorDocumento(pais);
    }
  
    private String limpiar(String s){
       return (s == null || s.isBlank()) ? null : s.trim(); 
    }


    
}
