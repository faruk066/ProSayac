package com.prosayac.app.data.repository;

import com.prosayac.app.data.local.dao.MeterDao;
import com.prosayac.app.data.local.dao.ReadingDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class MeterRepositoryImpl_Factory implements Factory<MeterRepositoryImpl> {
  private final Provider<MeterDao> meterDaoProvider;

  private final Provider<ReadingDao> readingDaoProvider;

  public MeterRepositoryImpl_Factory(Provider<MeterDao> meterDaoProvider,
      Provider<ReadingDao> readingDaoProvider) {
    this.meterDaoProvider = meterDaoProvider;
    this.readingDaoProvider = readingDaoProvider;
  }

  @Override
  public MeterRepositoryImpl get() {
    return newInstance(meterDaoProvider.get(), readingDaoProvider.get());
  }

  public static MeterRepositoryImpl_Factory create(Provider<MeterDao> meterDaoProvider,
      Provider<ReadingDao> readingDaoProvider) {
    return new MeterRepositoryImpl_Factory(meterDaoProvider, readingDaoProvider);
  }

  public static MeterRepositoryImpl newInstance(MeterDao meterDao, ReadingDao readingDao) {
    return new MeterRepositoryImpl(meterDao, readingDao);
  }
}
