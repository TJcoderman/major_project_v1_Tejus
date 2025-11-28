package com.securebank.app.ui.viewmodel;

import com.securebank.app.data.repository.BehavioralRepository;
import com.securebank.app.data.repository.UserRepository;
import com.securebank.app.domain.BehaviorAnalyzer;
import com.securebank.app.sensor.KeystrokeCollector;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
    "KotlinInternalInJava"
})
public final class AuthViewModel_Factory implements Factory<AuthViewModel> {
  private final Provider<UserRepository> userRepositoryProvider;

  private final Provider<BehavioralRepository> behavioralRepositoryProvider;

  private final Provider<KeystrokeCollector> keystrokeCollectorProvider;

  private final Provider<BehaviorAnalyzer> behaviorAnalyzerProvider;

  public AuthViewModel_Factory(Provider<UserRepository> userRepositoryProvider,
      Provider<BehavioralRepository> behavioralRepositoryProvider,
      Provider<KeystrokeCollector> keystrokeCollectorProvider,
      Provider<BehaviorAnalyzer> behaviorAnalyzerProvider) {
    this.userRepositoryProvider = userRepositoryProvider;
    this.behavioralRepositoryProvider = behavioralRepositoryProvider;
    this.keystrokeCollectorProvider = keystrokeCollectorProvider;
    this.behaviorAnalyzerProvider = behaviorAnalyzerProvider;
  }

  @Override
  public AuthViewModel get() {
    return newInstance(userRepositoryProvider.get(), behavioralRepositoryProvider.get(), keystrokeCollectorProvider.get(), behaviorAnalyzerProvider.get());
  }

  public static AuthViewModel_Factory create(Provider<UserRepository> userRepositoryProvider,
      Provider<BehavioralRepository> behavioralRepositoryProvider,
      Provider<KeystrokeCollector> keystrokeCollectorProvider,
      Provider<BehaviorAnalyzer> behaviorAnalyzerProvider) {
    return new AuthViewModel_Factory(userRepositoryProvider, behavioralRepositoryProvider, keystrokeCollectorProvider, behaviorAnalyzerProvider);
  }

  public static AuthViewModel newInstance(UserRepository userRepository,
      BehavioralRepository behavioralRepository, KeystrokeCollector keystrokeCollector,
      BehaviorAnalyzer behaviorAnalyzer) {
    return new AuthViewModel(userRepository, behavioralRepository, keystrokeCollector, behaviorAnalyzer);
  }
}
