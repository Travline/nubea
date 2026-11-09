package com.ecommerce.backend.cuentapago.vault;

public interface PaymentCredentialVault {
    String cifrar(String secreto, String finalidad);
    String descifrar(String sobre, String finalidad);
}
