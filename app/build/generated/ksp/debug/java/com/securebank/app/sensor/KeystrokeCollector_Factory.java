package com.securebank.app.sensor;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class KeystrokeCollector_Factory implements Factory<KeystrokeCollector> {
  @Override
  public KeystrokeCollector get() {
    return newInstance();
  }

  public static KeystrokeCollector_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static KeystrokeCollector newInstance() {
    return new KeystrokeCollector();
  }

  private static final class InstanceHolder {
    static final KeystrokeCollector_Factory INSTANCE = new KeystrokeCollector_Factory();
  }
}
