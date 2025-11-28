package com.securebank.app.di;

import com.securebank.app.data.local.SecureBankDatabase;
import com.securebank.app.data.local.dao.KeystrokeDao;
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
public final class AppModule_ProvideKeystrokeDaoFactory implements Factory<KeystrokeDao> {
  private final Provider<SecureBankDatabase> databaseProvider;

  public AppModule_ProvideKeystrokeDaoFactory(Provider<SecureBankDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public KeystrokeDao get() {
    return provideKeystrokeDao(databaseProvider.get());
  }

  public static AppModule_ProvideKeystrokeDaoFactory create(
      Provider<SecureBankDatabase> databaseProvider) {
    return new AppModule_ProvideKeystrokeDaoFactory(databaseProvider);
  }

  public static KeystrokeDao provideKeystrokeDao(SecureBankDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideKeystrokeDao(database));
  }
}
