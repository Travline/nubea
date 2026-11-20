package com.ecommerce.backend.compra;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Verificación de firma; la autenticidad no reemplaza la consulta de la orden al proveedor. */
public final class FirmaWebhookMercadoPago {
    private FirmaWebhookMercadoPago() {}

    public static boolean verificar(String secreto, String firma, String requestId, String dataId) {
        if (secreto == null || secreto.isBlank() || firma == null || firma.length() > 512
                || requestId == null || !requestId.matches("[A-Za-z0-9_-]{1,128}")
                || dataId == null || !dataId.matches("[A-Za-z0-9_-]{1,128}")) return false;
        String ts = null, v1 = null;
        for (String parte : firma.split(",")) {
            String[] par = parte.trim().split("=", 2);
            if (par.length != 2) return false;
            if ("ts".equals(par[0])) {
                if (ts != null) return false;
                ts = par[1];
            } else if ("v1".equals(par[0])) {
                if (v1 != null) return false;
                v1 = par[1];
            }
        }
        if (ts == null || !ts.matches("[0-9]{1,20}") || v1 == null || !v1.matches("[0-9a-fA-F]{64}")) return false;
        String manifest = "id:" + dataId.toLowerCase(Locale.ROOT) + ";request-id:" + requestId + ";ts:" + ts + ";";
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return MessageDigest.isEqual(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)), HexFormat.of().parseHex(v1));
        } catch (java.security.GeneralSecurityException ex) {
            throw new IllegalStateException("No se pudo validar la firma", ex);
        }
    }
}
