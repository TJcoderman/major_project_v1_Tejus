package com.securebank.app.data.repository;

import com.securebank.app.data.local.dao.BehavioralSessionDao;
import com.securebank.app.data.local.dao.KeystrokeDao;
import com.securebank.app.data.local.dao.MotionDao;
import com.securebank.app.data.local.dao.TouchDao;
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
public final class BehavioralRepository_Factory implements Factory<BehavioralRepository> {
  private final Provider<KeystrokeDao> keystrokeDaoProvider;

  private final Provider<TouchDao> touchDaoProvider;

  private final Provider<MotionDao> motionDaoProvider;

  private final Provider<BehavioralSessionDao> sessionDaoProvider;

  public BehavioralRepository_Factory(Provider<KeystrokeDao> keystrokeDaoProvider,
      Provider<TouchDao> touchDaoProvider, Provider<MotionDao> motionDaoProvider,
      Provider<BehavioralSessionDao> sessionDaoProvider) {
    this.keystrokeDaoProvider = keystrokeDaoProvider;
    this.touchDaoProvider = touchDaoProvider;
    this.motionDaoProvider = motionDaoProvider;
    this.sessionDaoProvider = sessionDaoProvider;
  }

  @Override
  public BehavioralRepository get() {
    return newInstance(keystrokeDaoProvider.get(), touchDaoProvider.get(), motionDaoProvider.get(), sessionDaoProvider.get());
  }

  public static BehavioralRepository_Factory create(Provider<KeystrokeDao> keystrokeDaoProvider,
      Provider<TouchDao> touchDaoProvider, Provider<MotionDao> motionDaoProvider,
      Provider<BehavioralSessionDao> sessionDaoProvider) {
    return new BehavioralRepository_Factory(keystrokeDaoProvider, touchDaoProvider, motionDaoProvider, sessionDaoProvider);
  }

  public static BehavioralRepository newInstance(KeystrokeDao keystrokeDao, TouchDao touchDao,
      MotionDao motionDao, BehavioralSessionDao sessionDao) {
    return new BehavioralRepository(keystrokeDao, touchDao, motionDao, sessionDao);
  }
}
