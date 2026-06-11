package com.prosayac.app.presentation.meters;

import com.prosayac.app.domain.repository.MeterRepository;
import com.prosayac.app.util.excel.ExcelParser;
import com.prosayac.app.util.serial.MBusSerialManager;
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
public final class MetersViewModel_Factory implements Factory<MetersViewModel> {
  private final Provider<MeterRepository> meterRepositoryProvider;

  private final Provider<MBusSerialManager> serialManagerProvider;

  private final Provider<ExcelParser> excelParserProvider;

  public MetersViewModel_Factory(Provider<MeterRepository> meterRepositoryProvider,
      Provider<MBusSerialManager> serialManagerProvider,
      Provider<ExcelParser> excelParserProvider) {
    this.meterRepositoryProvider = meterRepositoryProvider;
    this.serialManagerProvider = serialManagerProvider;
    this.excelParserProvider = excelParserProvider;
  }

  @Override
  public MetersViewModel get() {
    return newInstance(meterRepositoryProvider.get(), serialManagerProvider.get(), excelParserProvider.get());
  }

  public static MetersViewModel_Factory create(Provider<MeterRepository> meterRepositoryProvider,
      Provider<MBusSerialManager> serialManagerProvider,
      Provider<ExcelParser> excelParserProvider) {
    return new MetersViewModel_Factory(meterRepositoryProvider, serialManagerProvider, excelParserProvider);
  }

  public static MetersViewModel newInstance(MeterRepository meterRepository,
      MBusSerialManager serialManager, ExcelParser excelParser) {
    return new MetersViewModel(meterRepository, serialManager, excelParser);
  }
}
