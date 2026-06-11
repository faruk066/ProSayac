package com.prosayac.app.presentation.logs;

import com.prosayac.app.util.log.LogExportService;
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
public final class LogsViewModel_Factory implements Factory<LogsViewModel> {
  private final Provider<LogExportService> logExportServiceProvider;

  public LogsViewModel_Factory(Provider<LogExportService> logExportServiceProvider) {
    this.logExportServiceProvider = logExportServiceProvider;
  }

  @Override
  public LogsViewModel get() {
    return newInstance(logExportServiceProvider.get());
  }

  public static LogsViewModel_Factory create(Provider<LogExportService> logExportServiceProvider) {
    return new LogsViewModel_Factory(logExportServiceProvider);
  }

  public static LogsViewModel newInstance(LogExportService logExportService) {
    return new LogsViewModel(logExportService);
  }
}
