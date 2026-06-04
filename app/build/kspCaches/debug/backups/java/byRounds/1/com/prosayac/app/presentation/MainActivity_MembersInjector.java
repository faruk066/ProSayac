package com.prosayac.app.presentation;

import com.prosayac.app.data.datastore.UserPreferences;
import com.prosayac.app.util.serial.MBusSerialManager;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<MBusSerialManager> serialManagerProvider;

  private final Provider<UserPreferences> userPreferencesProvider;

  public MainActivity_MembersInjector(Provider<MBusSerialManager> serialManagerProvider,
      Provider<UserPreferences> userPreferencesProvider) {
    this.serialManagerProvider = serialManagerProvider;
    this.userPreferencesProvider = userPreferencesProvider;
  }

  public static MembersInjector<MainActivity> create(
      Provider<MBusSerialManager> serialManagerProvider,
      Provider<UserPreferences> userPreferencesProvider) {
    return new MainActivity_MembersInjector(serialManagerProvider, userPreferencesProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectSerialManager(instance, serialManagerProvider.get());
    injectUserPreferences(instance, userPreferencesProvider.get());
  }

  @InjectedFieldSignature("com.prosayac.app.presentation.MainActivity.serialManager")
  public static void injectSerialManager(MainActivity instance, MBusSerialManager serialManager) {
    instance.serialManager = serialManager;
  }

  @InjectedFieldSignature("com.prosayac.app.presentation.MainActivity.userPreferences")
  public static void injectUserPreferences(MainActivity instance, UserPreferences userPreferences) {
    instance.userPreferences = userPreferences;
  }
}
