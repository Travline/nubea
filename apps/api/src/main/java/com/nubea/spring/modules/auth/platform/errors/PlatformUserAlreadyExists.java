package com.nubea.spring.modules.auth.platform.errors;

public class PlatformUserAlreadyExists extends RuntimeException {
  public PlatformUserAlreadyExists(String message) {
    super(message);
  }
}
