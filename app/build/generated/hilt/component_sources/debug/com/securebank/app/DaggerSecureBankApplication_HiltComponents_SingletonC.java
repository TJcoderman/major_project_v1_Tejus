package com.securebank.app;

import android.app.Activity;
import android.app.Service;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.securebank.app.data.export.DataExporter;
import com.securebank.app.data.local.SecureBankDatabase;
import com.securebank.app.data.local.dao.BehavioralProfileDao;
import com.securebank.app.data.local.dao.BehavioralSessionDao;
import com.securebank.app.data.local.dao.KeystrokeDao;
import com.securebank.app.data.local.dao.MotionDao;
import com.securebank.app.data.local.dao.TouchDao;
import com.securebank.app.data.local.dao.TransactionDao;
import com.securebank.app.data.local.dao.UserDao;
import com.securebank.app.data.repository.BehavioralRepository;
import com.securebank.app.data.repository.UserRepository;
import com.securebank.app.di.AppModule_ProvideBehavioralProfileDaoFactory;
import com.securebank.app.di.AppModule_ProvideBehavioralSessionDaoFactory;
import com.securebank.app.di.AppModule_ProvideDatabaseFactory;
import com.securebank.app.di.AppModule_ProvideKeystrokeDaoFactory;
import com.securebank.app.di.AppModule_ProvideMotionDaoFactory;
import com.securebank.app.di.AppModule_ProvideTouchDaoFactory;
import com.securebank.app.di.AppModule_ProvideTransactionDaoFactory;
import com.securebank.app.di.AppModule_ProvideUserDaoFactory;
import com.securebank.app.domain.BehaviorAnalyzer;
import com.securebank.app.domain.FeatureExtractor;
import com.securebank.app.domain.MLFeatureExtractor;
import com.securebank.app.domain.MLModelInference;
import com.securebank.app.sensor.KeystrokeCollector;
import com.securebank.app.sensor.SensorDataCollector;
import com.securebank.app.sensor.TouchDataCollector;
import com.securebank.app.ui.viewmodel.AuthViewModel;
import com.securebank.app.ui.viewmodel.AuthViewModel_HiltModules;
import com.securebank.app.ui.viewmodel.AuthViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.securebank.app.ui.viewmodel.AuthViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.securebank.app.ui.viewmodel.BankingViewModel;
import com.securebank.app.ui.viewmodel.BankingViewModel_HiltModules;
import com.securebank.app.ui.viewmodel.BankingViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.securebank.app.ui.viewmodel.BankingViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.securebank.app.ui.viewmodel.ExperimentViewModel;
import com.securebank.app.ui.viewmodel.ExperimentViewModel_HiltModules;
import com.securebank.app.ui.viewmodel.ExperimentViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.securebank.app.ui.viewmodel.ExperimentViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.securebank.app.ui.viewmodel.SignupViewModel;
import com.securebank.app.ui.viewmodel.SignupViewModel_HiltModules;
import com.securebank.app.ui.viewmodel.SignupViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.securebank.app.ui.viewmodel.SignupViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.MapBuilder;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

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
public final class DaggerSecureBankApplication_HiltComponents_SingletonC {
  private DaggerSecureBankApplication_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public SecureBankApplication_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements SecureBankApplication_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public SecureBankApplication_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements SecureBankApplication_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public SecureBankApplication_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements SecureBankApplication_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public SecureBankApplication_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements SecureBankApplication_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public SecureBankApplication_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements SecureBankApplication_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public SecureBankApplication_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements SecureBankApplication_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public SecureBankApplication_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements SecureBankApplication_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public SecureBankApplication_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends SecureBankApplication_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends SecureBankApplication_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    FragmentCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends SecureBankApplication_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends SecureBankApplication_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    ActivityCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity mainActivity) {
      injectMainActivity2(mainActivity);
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(MapBuilder.<String, Boolean>newMapBuilder(4).put(AuthViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, AuthViewModel_HiltModules.KeyModule.provide()).put(BankingViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, BankingViewModel_HiltModules.KeyModule.provide()).put(ExperimentViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, ExperimentViewModel_HiltModules.KeyModule.provide()).put(SignupViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, SignupViewModel_HiltModules.KeyModule.provide()).build());
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    private MainActivity injectMainActivity2(MainActivity instance) {
      MainActivity_MembersInjector.injectSensorDataCollector(instance, singletonCImpl.sensorDataCollectorProvider.get());
      MainActivity_MembersInjector.injectTouchDataCollector(instance, singletonCImpl.touchDataCollectorProvider.get());
      return instance;
    }
  }

  private static final class ViewModelCImpl extends SecureBankApplication_HiltComponents.ViewModelC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    Provider<AuthViewModel> authViewModelProvider;

    Provider<BankingViewModel> bankingViewModelProvider;

    Provider<ExperimentViewModel> experimentViewModelProvider;

    Provider<SignupViewModel> signupViewModelProvider;

    ViewModelCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        SavedStateHandle savedStateHandleParam, ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;

      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.authViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.bankingViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.experimentViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.signupViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(MapBuilder.<String, javax.inject.Provider<ViewModel>>newMapBuilder(4).put(AuthViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) authViewModelProvider)).put(BankingViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) bankingViewModelProvider)).put(ExperimentViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) experimentViewModelProvider)).put(SignupViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) signupViewModelProvider)).build());
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return Collections.<Class<?>, Object>emptyMap();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.securebank.app.ui.viewmodel.AuthViewModel
          return (T) new AuthViewModel(singletonCImpl.userRepositoryProvider.get(), singletonCImpl.behavioralRepositoryProvider.get(), singletonCImpl.keystrokeCollectorProvider.get(), singletonCImpl.behaviorAnalyzerProvider.get());

          case 1: // com.securebank.app.ui.viewmodel.BankingViewModel
          return (T) new BankingViewModel(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.userRepositoryProvider.get(), singletonCImpl.behavioralRepositoryProvider.get(), singletonCImpl.sensorDataCollectorProvider.get(), singletonCImpl.touchDataCollectorProvider.get(), singletonCImpl.keystrokeCollectorProvider.get(), singletonCImpl.behaviorAnalyzerProvider.get());

          case 2: // com.securebank.app.ui.viewmodel.ExperimentViewModel
          return (T) new ExperimentViewModel(singletonCImpl.behavioralRepositoryProvider.get(), singletonCImpl.keystrokeCollectorProvider.get(), singletonCImpl.touchDataCollectorProvider.get(), singletonCImpl.sensorDataCollectorProvider.get(), singletonCImpl.dataExporterProvider.get(), singletonCImpl.featureExtractorProvider.get());

          case 3: // com.securebank.app.ui.viewmodel.SignupViewModel
          return (T) new SignupViewModel(singletonCImpl.userRepositoryProvider.get(), singletonCImpl.behavioralRepositoryProvider.get(), singletonCImpl.touchDataCollectorProvider.get(), singletonCImpl.sensorDataCollectorProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends SecureBankApplication_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends SecureBankApplication_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }
  }

  private static final class SingletonCImpl extends SecureBankApplication_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    Provider<SensorDataCollector> sensorDataCollectorProvider;

    Provider<TouchDataCollector> touchDataCollectorProvider;

    Provider<SecureBankDatabase> provideDatabaseProvider;

    Provider<UserDao> provideUserDaoProvider;

    Provider<TransactionDao> provideTransactionDaoProvider;

    Provider<UserRepository> userRepositoryProvider;

    Provider<KeystrokeDao> provideKeystrokeDaoProvider;

    Provider<TouchDao> provideTouchDaoProvider;

    Provider<MotionDao> provideMotionDaoProvider;

    Provider<BehavioralSessionDao> provideBehavioralSessionDaoProvider;

    Provider<BehavioralProfileDao> provideBehavioralProfileDaoProvider;

    Provider<BehavioralRepository> behavioralRepositoryProvider;

    Provider<KeystrokeCollector> keystrokeCollectorProvider;

    Provider<MLModelInference> mLModelInferenceProvider;

    Provider<MLFeatureExtractor> mLFeatureExtractorProvider;

    Provider<BehaviorAnalyzer> behaviorAnalyzerProvider;

    Provider<DataExporter> dataExporterProvider;

    Provider<FeatureExtractor> featureExtractorProvider;

    SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.sensorDataCollectorProvider = DoubleCheck.provider(new SwitchingProvider<SensorDataCollector>(singletonCImpl, 0));
      this.touchDataCollectorProvider = DoubleCheck.provider(new SwitchingProvider<TouchDataCollector>(singletonCImpl, 1));
      this.provideDatabaseProvider = DoubleCheck.provider(new SwitchingProvider<SecureBankDatabase>(singletonCImpl, 4));
      this.provideUserDaoProvider = DoubleCheck.provider(new SwitchingProvider<UserDao>(singletonCImpl, 3));
      this.provideTransactionDaoProvider = DoubleCheck.provider(new SwitchingProvider<TransactionDao>(singletonCImpl, 5));
      this.userRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<UserRepository>(singletonCImpl, 2));
      this.provideKeystrokeDaoProvider = DoubleCheck.provider(new SwitchingProvider<KeystrokeDao>(singletonCImpl, 7));
      this.provideTouchDaoProvider = DoubleCheck.provider(new SwitchingProvider<TouchDao>(singletonCImpl, 8));
      this.provideMotionDaoProvider = DoubleCheck.provider(new SwitchingProvider<MotionDao>(singletonCImpl, 9));
      this.provideBehavioralSessionDaoProvider = DoubleCheck.provider(new SwitchingProvider<BehavioralSessionDao>(singletonCImpl, 10));
      this.provideBehavioralProfileDaoProvider = DoubleCheck.provider(new SwitchingProvider<BehavioralProfileDao>(singletonCImpl, 11));
      this.behavioralRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<BehavioralRepository>(singletonCImpl, 6));
      this.keystrokeCollectorProvider = DoubleCheck.provider(new SwitchingProvider<KeystrokeCollector>(singletonCImpl, 12));
      this.mLModelInferenceProvider = DoubleCheck.provider(new SwitchingProvider<MLModelInference>(singletonCImpl, 14));
      this.mLFeatureExtractorProvider = DoubleCheck.provider(new SwitchingProvider<MLFeatureExtractor>(singletonCImpl, 15));
      this.behaviorAnalyzerProvider = DoubleCheck.provider(new SwitchingProvider<BehaviorAnalyzer>(singletonCImpl, 13));
      this.dataExporterProvider = DoubleCheck.provider(new SwitchingProvider<DataExporter>(singletonCImpl, 16));
      this.featureExtractorProvider = DoubleCheck.provider(new SwitchingProvider<FeatureExtractor>(singletonCImpl, 17));
    }

    @Override
    public void injectSecureBankApplication(SecureBankApplication secureBankApplication) {
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return Collections.<Boolean>emptySet();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.securebank.app.sensor.SensorDataCollector
          return (T) new SensorDataCollector(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 1: // com.securebank.app.sensor.TouchDataCollector
          return (T) new TouchDataCollector();

          case 2: // com.securebank.app.data.repository.UserRepository
          return (T) new UserRepository(singletonCImpl.provideUserDaoProvider.get(), singletonCImpl.provideTransactionDaoProvider.get());

          case 3: // com.securebank.app.data.local.dao.UserDao
          return (T) AppModule_ProvideUserDaoFactory.provideUserDao(singletonCImpl.provideDatabaseProvider.get());

          case 4: // com.securebank.app.data.local.SecureBankDatabase
          return (T) AppModule_ProvideDatabaseFactory.provideDatabase(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 5: // com.securebank.app.data.local.dao.TransactionDao
          return (T) AppModule_ProvideTransactionDaoFactory.provideTransactionDao(singletonCImpl.provideDatabaseProvider.get());

          case 6: // com.securebank.app.data.repository.BehavioralRepository
          return (T) new BehavioralRepository(singletonCImpl.provideKeystrokeDaoProvider.get(), singletonCImpl.provideTouchDaoProvider.get(), singletonCImpl.provideMotionDaoProvider.get(), singletonCImpl.provideBehavioralSessionDaoProvider.get(), singletonCImpl.provideBehavioralProfileDaoProvider.get());

          case 7: // com.securebank.app.data.local.dao.KeystrokeDao
          return (T) AppModule_ProvideKeystrokeDaoFactory.provideKeystrokeDao(singletonCImpl.provideDatabaseProvider.get());

          case 8: // com.securebank.app.data.local.dao.TouchDao
          return (T) AppModule_ProvideTouchDaoFactory.provideTouchDao(singletonCImpl.provideDatabaseProvider.get());

          case 9: // com.securebank.app.data.local.dao.MotionDao
          return (T) AppModule_ProvideMotionDaoFactory.provideMotionDao(singletonCImpl.provideDatabaseProvider.get());

          case 10: // com.securebank.app.data.local.dao.BehavioralSessionDao
          return (T) AppModule_ProvideBehavioralSessionDaoFactory.provideBehavioralSessionDao(singletonCImpl.provideDatabaseProvider.get());

          case 11: // com.securebank.app.data.local.dao.BehavioralProfileDao
          return (T) AppModule_ProvideBehavioralProfileDaoFactory.provideBehavioralProfileDao(singletonCImpl.provideDatabaseProvider.get());

          case 12: // com.securebank.app.sensor.KeystrokeCollector
          return (T) new KeystrokeCollector();

          case 13: // com.securebank.app.domain.BehaviorAnalyzer
          return (T) new BehaviorAnalyzer(singletonCImpl.behavioralRepositoryProvider.get(), singletonCImpl.keystrokeCollectorProvider.get(), singletonCImpl.mLModelInferenceProvider.get(), singletonCImpl.mLFeatureExtractorProvider.get());

          case 14: // com.securebank.app.domain.MLModelInference
          return (T) new MLModelInference(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 15: // com.securebank.app.domain.MLFeatureExtractor
          return (T) new MLFeatureExtractor();

          case 16: // com.securebank.app.data.export.DataExporter
          return (T) new DataExporter(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.behavioralRepositoryProvider.get());

          case 17: // com.securebank.app.domain.FeatureExtractor
          return (T) new FeatureExtractor();

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
