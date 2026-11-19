package com.ecommerce.backend.shared.storage;

import com.ecommerce.backend.config.BannerProperties;
import com.ecommerce.backend.exception.ArchivoInvalidoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class ImagenValidator {

    private final BannerProperties bannerProperties;

    public record ImagenValidada(String mimeType, String extension) {}

    public ImagenValidada validarImagen(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ArchivoInvalidoException("El archivo de imagen no puede estar vacío");
        }

        if (archivo.getSize() > bannerProperties.getMaxFileSize()) {
            long maxMb = bannerProperties.getMaxFileSize() / (1024 * 1024);
            throw new ArchivoInvalidoException("El archivo supera el tamaño máximo permitido de " + maxMb + " MB");
        }

        byte[] header = new byte[12];
        int read;
        try (InputStream is = archivo.getInputStream()) {
            read = is.read(header);
        } catch (IOException e) {
            throw new ArchivoInvalidoException("No se pudo leer el contenido del archivo de imagen");
        }

        if (read < 4) {
            throw new ArchivoInvalidoException("El archivo proporcionado no es una imagen válida o está dañado");
        }

        ImagenValidada detectada = detectarTipo(header, read);
        if (detectada == null || !bannerProperties.getAllowedMimeTypes().contains(detectada.mimeType())) {
            throw new ArchivoInvalidoException("Tipo de imagen no permitido. Tipos permitidos: "
                    + String.join(", ", bannerProperties.getAllowedMimeTypes()));
        }

        return detectada;
    }

    private ImagenValidada detectarTipo(byte[] header, int read) {
        // JPEG: FF D8 FF
        if (read >= 3 && (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
            return new ImagenValidada("image/jpeg", ".jpg");
        }

        // PNG: 89 50 4E 47
        if (read >= 4 && (header[0] & 0xFF) == 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47) {
            return new ImagenValidada("image/png", ".png");
        }

        // GIF: 47 49 46 38 ("GIF8")
        if (read >= 4 && header[0] == 0x47 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x38) {
            return new ImagenValidada("image/gif", ".gif");
        }

        // WebP: RIFF .... WEBP
        if (read >= 12 && header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46
                && header[8] == 0x57 && header[9] == 0x45 && header[10] == 0x42 && header[11] == 0x50) {
            return new ImagenValidada("image/webp", ".webp");
        }

        return null;
    }
}
