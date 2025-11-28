package com.securebank.app.di;

import com.securebank.app.data.local.SecureBankDatabase;
import com.securebank.app.data.local.dao.MotionDao;
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
public final class AppModule_ProvideMotionDaoFactory implements Factory<MotionDao> {
  private final Provider<SecureBankDatabase> databaseProvider;

  public AppModule_ProvideMotionDaoFactory(Provider<SecureBankDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public MotionDao get() {
    return provideMotionDao(databaseProvider.get());
  }

  public static AppModule_ProvideMotionDaoFactory create(
      Provider<SecureBankDatabase> databaseProvider) {
    return new AppModule_ProvideMotionDaoFactory(databaseProvider);
  }

  public static MotionDao provideMotionDao(SecureBankDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideMotionDao(database));
  }
}
