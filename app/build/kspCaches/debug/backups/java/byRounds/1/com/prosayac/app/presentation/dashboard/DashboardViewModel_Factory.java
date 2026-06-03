package com.prosayac.app.presentation.dashboard;

import com.prosayac.app.domain.repository.MeterRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class DashboardViewModel_Factory implements Factory<DashboardViewModel> {
  private final Provider<MeterRepository> meterRepositoryProvider;

  public DashboardViewModel_Factory(Provider<MeterRepository> meterRepositoryProvider) {
    this.meterRepositoryProvider = meterRepositoryProvider;
  }

  @Override
  public DashboardViewModel get() {
    return newInstance(meterRepositoryProvider.get());
  }

  public static DashboardViewModel_Factory create(
      Provider<MeterRepository> meterRepositoryProvider) {
    return new DashboardViewModel_Factory(meterRepositoryProvider);
  }

  public static DashboardViewModel newInstance(MeterRepository meterRepository) {
    return new DashboardViewModel(meterRepository);
  }
}
