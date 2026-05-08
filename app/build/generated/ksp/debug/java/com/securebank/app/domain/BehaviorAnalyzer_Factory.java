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

  private final Provider<MLModelInference> mlModelInferenceProvider;

  private final Provider<MLFeatureExtractor> mlFeatureExtractorProvider;

  public BehaviorAnalyzer_Factory(Provider<BehavioralRepository> behavioralRepositoryProvider,
      Provider<KeystrokeCollector> keystrokeCollectorProvider,
      Provider<MLModelInference> mlModelInferenceProvider,
      Provider<MLFeatureExtractor> mlFeatureExtractorProvider) {
    this.behavioralRepositoryProvider = behavioralRepositoryProvider;
    this.keystrokeCollectorProvider = keystrokeCollectorProvider;
    this.mlModelInferenceProvider = mlModelInferenceProvider;
    this.mlFeatureExtractorProvider = mlFeatureExtractorProvider;
  }

  @Override
  public BehaviorAnalyzer get() {
    return newInstance(behavioralRepositoryProvider.get(), keystrokeCollectorProvider.get(), mlModelInferenceProvider.get(), mlFeatureExtractorProvider.get());
  }

  public static BehaviorAnalyzer_Factory create(
      Provider<BehavioralRepository> behavioralRepositoryProvider,
      Provider<KeystrokeCollector> keystrokeCollectorProvider,
      Provider<MLModelInference> mlModelInferenceProvider,
      Provider<MLFeatureExtractor> mlFeatureExtractorProvider) {
    return new BehaviorAnalyzer_Factory(behavioralRepositoryProvider, keystrokeCollectorProvider, mlModelInferenceProvider, mlFeatureExtractorProvider);
  }

  public static BehaviorAnalyzer newInstance(BehavioralRepository behavioralRepository,
      KeystrokeCollector keystrokeCollector, MLModelInference mlModelInference,
      MLFeatureExtractor mlFeatureExtractor) {
    return new BehaviorAnalyzer(behavioralRepository, keystrokeCollector, mlModelInference, mlFeatureExtractor);
  }
}
