package com.securebank.app.ui.viewmodel;

import android.content.Context;
import com.securebank.app.data.repository.BehavioralRepository;
import com.securebank.app.data.repository.UserRepository;
import com.securebank.app.domain.BehaviorAnalyzer;
import com.securebank.app.sensor.KeystrokeCollector;
import com.securebank.app.sensor.SensorDataCollector;
import com.securebank.app.sensor.TouchDataCollector;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class BankingViewModel_Factory implements Factory<BankingViewModel> {
  private final Provider<Context> contextProvider;

  private final Provider<UserRepository> userRepositoryProvider;

  private final Provider<BehavioralRepository> behavioralRepositoryProvider;

  private final Provider<SensorDataCollector> sensorDataCollectorProvider;

  private final Provider<TouchDataCollector> touchDataCollectorProvider;

  private final Provider<KeystrokeCollector> keystrokeCollectorProvider;

  private final Provider<BehaviorAnalyzer> behaviorAnalyzerProvider;

  public BankingViewModel_Factory(Provider<Context> contextProvider,
      Provider<UserRepository> userRepositoryProvider,
      Provider<BehavioralRepository> behavioralRepositoryProvider,
      Provider<SensorDataCollector> sensorDataCollectorProvider,
      Provider<TouchDataCollector> touchDataCollectorProvider,
      Provider<KeystrokeCollector> keystrokeCollectorProvider,
      Provider<BehaviorAnalyzer> behaviorAnalyzerProvider) {
    this.contextProvider = contextProvider;
    this.userRepositoryProvider = userRepositoryProvider;
    this.behavioralRepositoryProvider = behavioralRepositoryProvider;
    this.sensorDataCollectorProvider = sensorDataCollectorProvider;
    this.touchDataCollectorProvider = touchDataCollectorProvider;
    this.keystrokeCollectorProvider = keystrokeCollectorProvider;
    this.behaviorAnalyzerProvider = behaviorAnalyzerProvider;
  }

  @Override
  public BankingViewModel get() {
    return newInstance(contextProvider.get(), userRepositoryProvider.get(), behavioralRepositoryProvider.get(), sensorDataCollectorProvider.get(), touchDataCollectorProvider.get(), keystrokeCollectorProvider.get(), behaviorAnalyzerProvider.get());
  }

  public static BankingViewModel_Factory create(Provider<Context> contextProvider,
      Provider<UserRepository> userRepositoryProvider,
      Provider<BehavioralRepository> behavioralRepositoryProvider,
      Provider<SensorDataCollector> sensorDataCollectorProvider,
      Provider<TouchDataCollector> touchDataCollectorProvider,
      Provider<KeystrokeCollector> keystrokeCollectorProvider,
      Provider<BehaviorAnalyzer> behaviorAnalyzerProvider) {
    return new BankingViewModel_Factory(contextProvider, userRepositoryProvider, behavioralRepositoryProvider, sensorDataCollectorProvider, touchDataCollectorProvider, keystrokeCollectorProvider, behaviorAnalyzerProvider);
  }

  public static BankingViewModel newInstance(Context context, UserRepository userRepository,
      BehavioralRepository behavioralRepository, SensorDataCollector sensorDataCollector,
      TouchDataCollector touchDataCollector, KeystrokeCollector keystrokeCollector,
      BehaviorAnalyzer behaviorAnalyzer) {
    return new BankingViewModel(context, userRepository, behavioralRepository, sensorDataCollector, touchDataCollector, keystrokeCollector, behaviorAnalyzer);
  }
}
