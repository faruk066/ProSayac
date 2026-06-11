package com.prosayac.app.di

import com.prosayac.app.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = "https://pjucsatqzgfdfokauqzf.supabase.co",
            supabaseKey = "sb_publishable_SDsWK92q0lihDUJHTUvVvg_QJ4YAJzO"
        ) {
            install(Postgrest)
            install(Auth)
        }
    }
}
