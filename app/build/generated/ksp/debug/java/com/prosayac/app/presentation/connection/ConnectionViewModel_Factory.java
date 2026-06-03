package com.prosayac.app.presentation.connection;

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
public final class ConnectionViewModel_Factory implements Factory<ConnectionViewModel> {
  private final Provider<MBusSerialManager> serialManagerProvider;

  public ConnectionViewModel_Factory(Provider<MBusSerialManager> serialManagerProvider) {
    this.serialManagerProvider = serialManagerProvider;
  }

  @Override
  public ConnectionViewModel get() {
    return newInstance(serialManagerProvider.get());
  }

  public static ConnectionViewModel_Factory create(
      Provider<MBusSerialManager> serialManagerProvider) {
    return new ConnectionViewModel_Factory(serialManagerProvider);
  }

  public static ConnectionViewModel newInstance(MBusSerialManager serialManager) {
    return new ConnectionViewModel(serialManager);
  }
}
