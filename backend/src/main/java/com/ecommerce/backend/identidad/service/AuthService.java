package com.ecommerce.backend.identidad.service;
import java.util.stream.Collectors;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.Authentication;
import com.ecommerce.backend.identidad.dto.LoginRequest;
import com.ecommerce.backend.identidad.model.EstadoUsuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.backend.identidad.dto.RegistroUsuarioRequest;
import com.ecommerce.backend.identidad.dto.UsuarioResponse;
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
import com.ecommerce.backend.identidad.google.DatosGoogle;
import com.ecommerce.backend.shared.exception.MensajesUsuario;

@Service 
@RequiredArgsConstructor 
public class AuthService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthService.class);
    private final UsuarioPlataformaRepository usuarioRepository;
    private final UsuarioIdentidadRepository identidadRepository;
    private final RolRepository rolRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final PasswordEncoder passwordEncoder;  // el bean esta con Argon2 de SecurityConfig 

    @Transactional(readOnly = true)
    public UsuarioResponse usuarioActual(UUID id) {
        return toResponse(usuarioRepository.findById(id).orElseThrow());
    }

    @Transactional(readOnly = true)
    public Authentication autenticar(LoginRequest req) {
        var identidad = identidadRepository.findByUsuario_EmailAndProveedor(
                req.email().trim().toLowerCase(java.util.Locale.ROOT), ProveedorIdentidad.LOCAL)
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));
        var usuario = identidad.getUsuario();
        if (usuario.getEstado() != EstadoUsuario.ACTIVO || !passwordEncoder.matches(req.password(), identidad.getPasswordHash())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }
        return autenticacion(usuario);
    }

    private Authentication autenticacion(UsuarioPlataforma usuario) {
        var roles = usuario.getRoles().stream()
                .map(rol -> new SimpleGrantedAuthority("ROLE_" + rol.getNombre().name())).toList();
        return UsernamePasswordAuthenticationToken.authenticated(usuario.getIdUsuario(), null, roles);
    }

    /** Resultado del inicio de sesión con Google: {@code creado} solo si se creó una cuenta nueva. */
    public record ResultadoGoogle(Authentication autenticacion, UsuarioResponse creado) {}

    /**
     * Inicio de sesión con Google (RN-M1-05). La identidad se busca por (issuer, subject), nunca por correo.
     * Si el correo ya tiene cuenta y la identidad no existe, NO se vincula: 409 y el usuario debe vincular desde su cuenta.
     */
    @Transactional
    public ResultadoGoogle autenticarConGoogle(DatosGoogle google) {
        var existente = identidadRepository.findByIssuerAndSubject(google.issuer(), google.subject());
        if (existente.isPresent()) {
            var usuario = existente.get().getUsuario();
            if (usuario.getEstado() != EstadoUsuario.ACTIVO) throw new BadCredentialsException("Credenciales inválidas");
            return new ResultadoGoogle(autenticacion(usuario), null);
        }
        if (usuarioRepository.existsByEmail(google.email())) {
            throw new RecursosDuplicadosException("correoElectronico", MensajesUsuario.GOOGLE_CORREO_YA_REGISTRADO);
        }
        UsuarioPlataforma usuario = UsuarioPlataforma.builder()
                .nombres(recortar(nombres(google), 100))
                .apellidoPaterno(recortar(limpiar(google.apellido()), 80))
                .email(google.email())
                .build();
        Rol cliente = rolRepository.findByNombre(NombreRol.CLIENTE)
                .orElseThrow(() -> new IllegalStateException("Falta el rol CLIENTE en la base de datos"));
        usuario.getRoles().add(cliente);
        usuarioRepository.save(usuario);
        identidadRepository.saveAndFlush(UsuarioIdentidad.builder()
                .usuario(usuario)
                .proveedor(ProveedorIdentidad.GOOGLE)
                .issuer(google.issuer())
                .subject(google.subject())
                .build());
        log.info("Cuenta creada con Google (sub={})", google.subject());
        return new ResultadoGoogle(autenticacion(usuario), toResponse(usuario));
    }

    /** Vincula Google a la cuenta con sesión iniciada, si ese subject no pertenece a otro usuario. */
    @Transactional
    public void vincularGoogle(UUID idUsuario, DatosGoogle google) {
        var existente = identidadRepository.findByIssuerAndSubject(google.issuer(), google.subject());
        if (existente.isPresent()) {
            if (existente.get().getUsuario().getIdUsuario().equals(idUsuario)) return; // ya vinculada: sin cambios
            throw new RecursosDuplicadosException("idToken", MensajesUsuario.GOOGLE_DE_OTRO_USUARIO);
        }
        if (identidadRepository.existsByUsuario_IdUsuarioAndProveedor(idUsuario, ProveedorIdentidad.GOOGLE)) {
            throw new RecursosDuplicadosException("idToken", MensajesUsuario.GOOGLE_YA_VINCULADO_OTRA);
        }
        var usuario = usuarioRepository.findById(idUsuario).orElseThrow();
        identidadRepository.saveAndFlush(UsuarioIdentidad.builder()
                .usuario(usuario)
                .proveedor(ProveedorIdentidad.GOOGLE)
                .issuer(google.issuer())
                .subject(google.subject())
                .build());
        log.info("Google vinculado a una cuenta existente (sub={})", google.subject());
    }

    private String nombres(DatosGoogle google) {
        String nombres = limpiar(google.nombres());
        if (nombres != null) return nombres;
        return google.email().substring(0, Math.max(1, google.email().indexOf('@')));
    }

    private static String recortar(String texto, int maximo) {
        return texto == null || texto.length() <= maximo ? texto : texto.substring(0, maximo).trim();
    }

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

        return toResponse(usuario);
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
  
    private UsuarioResponse toResponse(UsuarioPlataforma u){
        return new UsuarioResponse(
            u.getIdUsuario(), u.getNombres(),
            u.getApellidoPaterno(), u.getApellidoMaterno(),
            u.getEmail(), u.getEstado().name(),
            u.getRoles().stream().map(r -> r.getNombre().name()).collect(Collectors.toSet()),
            u.getFechaCreacion());
    }

    private String limpiar(String s){
       return (s == null || s.isBlank()) ? null : s.trim(); 
    }


    
}
