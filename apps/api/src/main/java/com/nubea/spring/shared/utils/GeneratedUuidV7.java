package com.nubea.spring.shared.utils;

import org.hibernate.annotations.IdGeneratorType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@IdGeneratorType(UuidV7Generator.class)
public @interface GeneratedUuidV7 {
}
