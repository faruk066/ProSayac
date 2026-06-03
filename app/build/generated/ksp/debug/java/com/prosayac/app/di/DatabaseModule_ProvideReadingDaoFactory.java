package com.prosayac.app.di;

import com.prosayac.app.data.local.dao.ReadingDao;
import com.prosayac.app.data.local.database.AppDatabase;
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
public final class DatabaseModule_ProvideReadingDaoFactory implements Factory<ReadingDao> {
  private final Provider<AppDatabase> databaseProvider;

  public DatabaseModule_ProvideReadingDaoFactory(Provider<AppDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public ReadingDao get() {
    return provideReadingDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideReadingDaoFactory create(
      Provider<AppDatabase> databaseProvider) {
    return new DatabaseModule_ProvideReadingDaoFactory(databaseProvider);
  }

  public static ReadingDao provideReadingDao(AppDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideReadingDao(database));
  }
}
