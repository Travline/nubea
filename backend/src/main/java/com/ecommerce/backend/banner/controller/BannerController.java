package com.ecommerce.backend.banner.controller;

import com.ecommerce.backend.banner.dto.ActualizarBannerRequest;
import com.ecommerce.backend.banner.dto.BannerResponse;
import com.ecommerce.backend.banner.dto.ReordenarBannersRequest;
import com.ecommerce.backend.banner.service.BannerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tiendas/{idTienda}/banners")
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;

    @GetMapping
    @PreAuthorize("@tiendaAuthorizationService.esPropietario(#idTienda, authentication)")
    public ResponseEntity<List<BannerResponse>> listarBanners(@PathVariable UUID idTienda) {
        return ResponseEntity.ok(bannerService.listarBanners(idTienda));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@tiendaAuthorizationService.esPropietario(#idTienda, authentication)")
    public ResponseEntity<BannerResponse> crearBanner(
            @PathVariable UUID idTienda,
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam(value = "enlaceDestino", required = false) String enlaceDestino,
            @RequestParam("orden") Short orden) {
        BannerResponse response = bannerService.crearBanner(idTienda, archivo, enlaceDestino, orden);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{idBanner}")
    @PreAuthorize("@tiendaAuthorizationService.esPropietario(#idTienda, authentication)")
    public ResponseEntity<BannerResponse> actualizarBanner(
            @PathVariable UUID idTienda,
            @PathVariable UUID idBanner,
            @RequestBody @Valid ActualizarBannerRequest request) {
        return ResponseEntity.ok(bannerService.actualizarBanner(idTienda, idBanner, request));
    }

    @PutMapping(value = "/{idBanner}/archivo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@tiendaAuthorizationService.esPropietario(#idTienda, authentication)")
    public ResponseEntity<BannerResponse> sustituirImagen(
            @PathVariable UUID idTienda,
            @PathVariable UUID idBanner,
            @RequestParam("archivo") MultipartFile archivo) {
        return ResponseEntity.ok(bannerService.sustituirImagen(idTienda, idBanner, archivo));
    }

    @PutMapping("/orden")
    @PreAuthorize("@tiendaAuthorizationService.esPropietario(#idTienda, authentication)")
    public ResponseEntity<List<BannerResponse>> reordenarBanners(
            @PathVariable UUID idTienda,
            @RequestBody @Valid ReordenarBannersRequest request) {
        return ResponseEntity.ok(bannerService.reordenarBanners(idTienda, request));
    }

    @DeleteMapping("/{idBanner}")
    @PreAuthorize("@tiendaAuthorizationService.esPropietario(#idTienda, authentication)")
    public ResponseEntity<Void> eliminarBanner(
            @PathVariable UUID idTienda,
            @PathVariable UUID idBanner) {
        bannerService.eliminarBanner(idTienda, idBanner);
        return ResponseEntity.noContent().build();
    }
}
