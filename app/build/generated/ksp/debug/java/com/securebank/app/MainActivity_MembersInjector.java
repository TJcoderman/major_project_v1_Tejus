package com.securebank.app;

import com.securebank.app.sensor.SensorDataCollector;
import com.securebank.app.sensor.TouchDataCollector;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;

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
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<SensorDataCollector> sensorDataCollectorProvider;

  private final Provider<TouchDataCollector> touchDataCollectorProvider;

  public MainActivity_MembersInjector(Provider<SensorDataCollector> sensorDataCollectorProvider,
      Provider<TouchDataCollector> touchDataCollectorProvider) {
    this.sensorDataCollectorProvider = sensorDataCollectorProvider;
    this.touchDataCollectorProvider = touchDataCollectorProvider;
  }

  public static MembersInjector<MainActivity> create(
      Provider<SensorDataCollector> sensorDataCollectorProvider,
      Provider<TouchDataCollector> touchDataCollectorProvider) {
    return new MainActivity_MembersInjector(sensorDataCollectorProvider, touchDataCollectorProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectSensorDataCollector(instance, sensorDataCollectorProvider.get());
    injectTouchDataCollector(instance, touchDataCollectorProvider.get());
  }

  @InjectedFieldSignature("com.securebank.app.MainActivity.sensorDataCollector")
  public static void injectSensorDataCollector(MainActivity instance,
      SensorDataCollector sensorDataCollector) {
    instance.sensorDataCollector = sensorDataCollector;
  }

  @InjectedFieldSignature("com.securebank.app.MainActivity.touchDataCollector")
  public static void injectTouchDataCollector(MainActivity instance,
      TouchDataCollector touchDataCollector) {
    instance.touchDataCollector = touchDataCollector;
  }
}
