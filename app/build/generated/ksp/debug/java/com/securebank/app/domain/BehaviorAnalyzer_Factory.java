package com.securebank.app.domain;

import com.securebank.app.data.repository.BehavioralRepository;
import com.securebank.app.sensor.KeystrokeCollector;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
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
public final class BehaviorAnalyzer_Factory implements Factory<BehaviorAnalyzer> {
  private final Provider<BehavioralRepository> behavioralRepositoryProvider;

  private final Provider<KeystrokeCollector> keystrokeCollectorProvider;

  public BehaviorAnalyzer_Factory(Provider<BehavioralRepository> behavioralRepositoryProvider,
      Provider<KeystrokeCollector> keystrokeCollectorProvider) {
    this.behavioralRepositoryProvider = behavioralRepositoryProvider;
    this.keystrokeCollectorProvider = keystrokeCollectorProvider;
  }

  @Override
  public BehaviorAnalyzer get() {
    return newInstance(behavioralRepositoryProvider.get(), keystrokeCollectorProvider.get());
  }

  public static BehaviorAnalyzer_Factory create(
      Provider<BehavioralRepository> behavioralRepositoryProvider,
      Provider<KeystrokeCollector> keystrokeCollectorProvider) {
    return new BehaviorAnalyzer_Factory(behavioralRepositoryProvider, keystrokeCollectorProvider);
  }

  public static BehaviorAnalyzer newInstance(BehavioralRepository behavioralRepository,
      KeystrokeCollector keystrokeCollector) {
    return new BehaviorAnalyzer(behavioralRepository, keystrokeCollector);
  }
}
