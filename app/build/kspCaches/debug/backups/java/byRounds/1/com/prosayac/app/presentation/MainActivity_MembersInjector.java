package com.prosayac.app.presentation;

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

  public MainActivity_MembersInjector(Provider<MBusSerialManager> serialManagerProvider) {
    this.serialManagerProvider = serialManagerProvider;
  }

  public static MembersInjector<MainActivity> create(
      Provider<MBusSerialManager> serialManagerProvider) {
    return new MainActivity_MembersInjector(serialManagerProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectSerialManager(instance, serialManagerProvider.get());
  }

  @InjectedFieldSignature("com.prosayac.app.presentation.MainActivity.serialManager")
  public static void injectSerialManager(MainActivity instance, MBusSerialManager serialManager) {
    instance.serialManager = serialManager;
  }
}
