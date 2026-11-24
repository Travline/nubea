package com.ecommerce.backend.identidad.service;

import com.ecommerce.backend.identidad.model.TipoDocumento;
import com.ecommerce.backend.identidad.model.UsuarioPlataforma;
import com.ecommerce.backend.identidad.repository.TipoDocumentoRepository;
import com.ecommerce.backend.shared.exception.MensajesUsuario;
import com.ecommerce.backend.shared.exception.ValidacionCampoException;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Documento de identidad (RN-M1-03/04), compartido por el registro y el perfil.
 * Tipo, número y país van juntos; sin tipo ni número el documento se borra (chk_documento_conjunto).
 * Los errores llegan como 400 con el campo afectado. Un documento ya registrado por otro usuario lo detecta
 * uq_usuario_documento (409 con campos.numeroDocumento).
 */
@Component
@RequiredArgsConstructor
public class DocumentoIdentidad {
    private final TipoDocumentoRepository tipos;

    public void aplicar(UsuarioPlataforma usuario, String tipoDocumento, String numeroDocumento, String paisEmisor) {
        String codigo = limpiar(tipoDocumento);
        String numero = limpiar(numeroDocumento);
        if (codigo == null && numero == null) {
            usuario.setTipoDocumento(null);
            usuario.setNumeroDocumento(null);
            usuario.setPaisEmisorDocumento(null);
            return;
        }
        if (codigo == null) throw new ValidacionCampoException("tipoDocumento", MensajesUsuario.DOCUMENTO_TIPO_Y_NUMERO);
        if (numero == null) throw new ValidacionCampoException("numeroDocumento", MensajesUsuario.DOCUMENTO_TIPO_Y_NUMERO);
        TipoDocumento tipo = tipos.findByCodigo(codigo.toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ValidacionCampoException("tipoDocumento", MensajesUsuario.DOCUMENTO_TIPO_INVALIDO));

        String pais;
        switch (tipo.getCodigo()) {
            case "DNI" -> {
                if (!numero.matches("\\d{8}")) throw new ValidacionCampoException("numeroDocumento", MensajesUsuario.DNI_INVALIDO);
                pais = "PE";
            }
            case "CE" -> {
                if (!numero.matches("[A-Za-z0-9]{9,12}")) throw new ValidacionCampoException("numeroDocumento", MensajesUsuario.CARNE_EXTRANJERIA_INVALIDO);
                pais = "PE";
            }
            default -> {
                if (!numero.matches("[A-Za-z0-9]{6,12}")) throw new ValidacionCampoException("numeroDocumento", MensajesUsuario.PASAPORTE_INVALIDO);
                pais = limpiar(paisEmisor);
                if (pais == null) throw new ValidacionCampoException("paisEmisorDocumento", MensajesUsuario.PAIS_EMISOR_REQUERIDO);
                pais = pais.toUpperCase(Locale.ROOT);
            }
        }
        usuario.setTipoDocumento(tipo);
        usuario.setNumeroDocumento(numero.toUpperCase(Locale.ROOT));
        usuario.setPaisEmisorDocumento(pais);
    }

    private static String limpiar(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
