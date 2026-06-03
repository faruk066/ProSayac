package com.prosayac.app.util.serial;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class MBusSerialManager_Factory implements Factory<MBusSerialManager> {
  private final Provider<Context> contextProvider;

  public MBusSerialManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public MBusSerialManager get() {
    return newInstance(contextProvider.get());
  }

  public static MBusSerialManager_Factory create(Provider<Context> contextProvider) {
    return new MBusSerialManager_Factory(contextProvider);
  }

  public static MBusSerialManager newInstance(Context context) {
    return new MBusSerialManager(context);
  }
}
