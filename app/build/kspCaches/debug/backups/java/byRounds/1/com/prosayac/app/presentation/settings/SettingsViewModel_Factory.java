package com.prosayac.app.presentation.settings;

import com.prosayac.app.data.datastore.UserPreferences;
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

  public SettingsViewModel_Factory(Provider<UserPreferences> userPreferencesProvider) {
    this.userPreferencesProvider = userPreferencesProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(userPreferencesProvider.get());
  }

  public static SettingsViewModel_Factory create(
      Provider<UserPreferences> userPreferencesProvider) {
    return new SettingsViewModel_Factory(userPreferencesProvider);
  }

  public static SettingsViewModel newInstance(UserPreferences userPreferences) {
    return new SettingsViewModel(userPreferences);
  }
}
