package com.ecommerce.backend.shared.utils;

import java.util.EnumSet;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;

import com.github.f4b6a3.uuid.UuidCreator;

public class UuidV7Generator implements BeforeExecutionGenerator {
  @Override
  public Object generate(SharedSessionContractImplementor session, Object owner, Object currentValue, EventType eventType) {
    return UuidCreator.getTimeOrderedEpoch();
  }

  @Override
  public EnumSet<EventType> getEventTypes() {
    return EnumSet.of(EventType.INSERT);
  }
}