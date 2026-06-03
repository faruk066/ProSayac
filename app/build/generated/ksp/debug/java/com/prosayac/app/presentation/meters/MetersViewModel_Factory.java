package com.prosayac.app.presentation.meters;

import android.content.Context;
import com.prosayac.app.domain.repository.MeterRepository;
import com.prosayac.app.util.serial.MBusSerialManager;
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
public final class MetersViewModel_Factory implements Factory<MetersViewModel> {
  private final Provider<MeterRepository> meterRepositoryProvider;

  private final Provider<MBusSerialManager> serialManagerProvider;

  private final Provider<Context> contextProvider;

  public MetersViewModel_Factory(Provider<MeterRepository> meterRepositoryProvider,
      Provider<MBusSerialManager> serialManagerProvider, Provider<Context> contextProvider) {
    this.meterRepositoryProvider = meterRepositoryProvider;
    this.serialManagerProvider = serialManagerProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public MetersViewModel get() {
    return newInstance(meterRepositoryProvider.get(), serialManagerProvider.get(), contextProvider.get());
  }

  public static MetersViewModel_Factory create(Provider<MeterRepository> meterRepositoryProvider,
      Provider<MBusSerialManager> serialManagerProvider, Provider<Context> contextProvider) {
    return new MetersViewModel_Factory(meterRepositoryProvider, serialManagerProvider, contextProvider);
  }

  public static MetersViewModel newInstance(MeterRepository meterRepository,
      MBusSerialManager serialManager, Context context) {
    return new MetersViewModel(meterRepository, serialManager, context);
  }
}
