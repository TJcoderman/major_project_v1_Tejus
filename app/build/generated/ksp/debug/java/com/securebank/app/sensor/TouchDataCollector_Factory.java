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
public final class TouchDataCollector_Factory implements Factory<TouchDataCollector> {
  @Override
  public TouchDataCollector get() {
    return newInstance();
  }

  public static TouchDataCollector_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static TouchDataCollector newInstance() {
    return new TouchDataCollector();
  }

  private static final class InstanceHolder {
    static final TouchDataCollector_Factory INSTANCE = new TouchDataCollector_Factory();
  }
}
