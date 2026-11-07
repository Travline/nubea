package com.ecommerce.backend.shared.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.annotations.IdGeneratorType;

@Target ({ ElementType.FIELD, ElementType.METHOD })
@Retention (RetentionPolicy.RUNTIME)
@IdGeneratorType (UuidV7Generator.class)
public @interface  GeneratedUuidV7 {
    
}
