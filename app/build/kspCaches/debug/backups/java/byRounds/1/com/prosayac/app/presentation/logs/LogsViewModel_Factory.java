package com.prosayac.app.presentation.logs;

import android.content.Context;
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
public final class LogsViewModel_Factory implements Factory<LogsViewModel> {
  private final Provider<Context> contextProvider;

  public LogsViewModel_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public LogsViewModel get() {
    return newInstance(contextProvider.get());
  }

  public static LogsViewModel_Factory create(Provider<Context> contextProvider) {
    return new LogsViewModel_Factory(contextProvider);
  }

  public static LogsViewModel newInstance(Context context) {
    return new LogsViewModel(context);
  }
}
