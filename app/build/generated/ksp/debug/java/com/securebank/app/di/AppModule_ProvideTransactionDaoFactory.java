package com.securebank.app.di;

import com.securebank.app.data.local.SecureBankDatabase;
import com.securebank.app.data.local.dao.TransactionDao;
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
public final class AppModule_ProvideTransactionDaoFactory implements Factory<TransactionDao> {
  private final Provider<SecureBankDatabase> databaseProvider;

  public AppModule_ProvideTransactionDaoFactory(Provider<SecureBankDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public TransactionDao get() {
    return provideTransactionDao(databaseProvider.get());
  }

  public static AppModule_ProvideTransactionDaoFactory create(
      Provider<SecureBankDatabase> databaseProvider) {
    return new AppModule_ProvideTransactionDaoFactory(databaseProvider);
  }

  public static TransactionDao provideTransactionDao(SecureBankDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideTransactionDao(database));
  }
}
