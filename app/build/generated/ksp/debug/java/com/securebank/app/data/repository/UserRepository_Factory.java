package com.securebank.app.data.repository;

import com.securebank.app.data.local.dao.TransactionDao;
import com.securebank.app.data.local.dao.UserDao;
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
public final class UserRepository_Factory implements Factory<UserRepository> {
  private final Provider<UserDao> userDaoProvider;

  private final Provider<TransactionDao> transactionDaoProvider;

  public UserRepository_Factory(Provider<UserDao> userDaoProvider,
      Provider<TransactionDao> transactionDaoProvider) {
    this.userDaoProvider = userDaoProvider;
    this.transactionDaoProvider = transactionDaoProvider;
  }

  @Override
  public UserRepository get() {
    return newInstance(userDaoProvider.get(), transactionDaoProvider.get());
  }

  public static UserRepository_Factory create(Provider<UserDao> userDaoProvider,
      Provider<TransactionDao> transactionDaoProvider) {
    return new UserRepository_Factory(userDaoProvider, transactionDaoProvider);
  }

  public static UserRepository newInstance(UserDao userDao, TransactionDao transactionDao) {
    return new UserRepository(userDao, transactionDao);
  }
}
