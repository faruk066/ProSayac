package com.prosayac.app.presentation.readings;

import com.prosayac.app.domain.repository.MeterRepository;
import com.prosayac.app.util.excel.ExcelExporter;
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
public final class ReadingsViewModel_Factory implements Factory<ReadingsViewModel> {
  private final Provider<MeterRepository> meterRepositoryProvider;

  private final Provider<ExcelExporter> excelExporterProvider;

  public ReadingsViewModel_Factory(Provider<MeterRepository> meterRepositoryProvider,
      Provider<ExcelExporter> excelExporterProvider) {
    this.meterRepositoryProvider = meterRepositoryProvider;
    this.excelExporterProvider = excelExporterProvider;
  }

  @Override
  public ReadingsViewModel get() {
    return newInstance(meterRepositoryProvider.get(), excelExporterProvider.get());
  }

  public static ReadingsViewModel_Factory create(Provider<MeterRepository> meterRepositoryProvider,
      Provider<ExcelExporter> excelExporterProvider) {
    return new ReadingsViewModel_Factory(meterRepositoryProvider, excelExporterProvider);
  }

  public static ReadingsViewModel newInstance(MeterRepository meterRepository,
      ExcelExporter excelExporter) {
    return new ReadingsViewModel(meterRepository, excelExporter);
  }
}
