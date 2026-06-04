package com.prosayac.app.presentation.settings;

import com.prosayac.app.data.datastore.UserPreferences;
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<UserPreferences> userPreferencesProvider;

  private final Provider<MeterRepository> meterRepositoryProvider;

  public SettingsViewModel_Factory(Provider<UserPreferences> userPreferencesProvider,
      Provider<MeterRepository> meterRepositoryProvider) {
    this.userPreferencesProvider = userPreferencesProvider;
    this.meterRepositoryProvider = meterRepositoryProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(userPreferencesProvider.get(), meterRepositoryProvider.get());
  }

  public static SettingsViewModel_Factory create(Provider<UserPreferences> userPreferencesProvider,
      Provider<MeterRepository> meterRepositoryProvider) {
    return new SettingsViewModel_Factory(userPreferencesProvider, meterRepositoryProvider);
  }

  public static SettingsViewModel newInstance(UserPreferences userPreferences,
      MeterRepository meterRepository) {
    return new SettingsViewModel(userPreferences, meterRepository);
  }
}
