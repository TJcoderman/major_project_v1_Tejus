package com.securebank.app.di;

import com.securebank.app.data.local.SecureBankDatabase;
import com.securebank.app.data.local.dao.BehavioralSessionDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
    "KotlinInternalInJava"
})
public final class AppModule_ProvideBehavioralSessionDaoFactory implements Factory<BehavioralSessionDao> {
  private final Provider<SecureBankDatabase> databaseProvider;

  public AppModule_ProvideBehavioralSessionDaoFactory(
      Provider<SecureBankDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public BehavioralSessionDao get() {
    return provideBehavioralSessionDao(databaseProvider.get());
  }

  public static AppModule_ProvideBehavioralSessionDaoFactory create(
      Provider<SecureBankDatabase> databaseProvider) {
    return new AppModule_ProvideBehavioralSessionDaoFactory(databaseProvider);
  }

  public static BehavioralSessionDao provideBehavioralSessionDao(SecureBankDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideBehavioralSessionDao(database));
  }
}
