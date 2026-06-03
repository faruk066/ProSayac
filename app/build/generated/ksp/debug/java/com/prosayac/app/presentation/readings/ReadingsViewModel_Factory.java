package com.prosayac.app.presentation.readings;

import android.content.Context;
import com.prosayac.app.domain.repository.MeterRepository;
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
public final class ReadingsViewModel_Factory implements Factory<ReadingsViewModel> {
  private final Provider<MeterRepository> meterRepositoryProvider;

  private final Provider<Context> contextProvider;

  public ReadingsViewModel_Factory(Provider<MeterRepository> meterRepositoryProvider,
      Provider<Context> contextProvider) {
    this.meterRepositoryProvider = meterRepositoryProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public ReadingsViewModel get() {
    return newInstance(meterRepositoryProvider.get(), contextProvider.get());
  }

  public static ReadingsViewModel_Factory create(Provider<MeterRepository> meterRepositoryProvider,
      Provider<Context> contextProvider) {
    return new ReadingsViewModel_Factory(meterRepositoryProvider, contextProvider);
  }

  public static ReadingsViewModel newInstance(MeterRepository meterRepository, Context context) {
    return new ReadingsViewModel(meterRepository, context);
  }
}
