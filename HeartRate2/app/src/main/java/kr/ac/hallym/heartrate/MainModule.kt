package kr.ac.hallym.heartrate

import android.content.Context
import androidx.health.services.client.HealthServices
import androidx.health.services.client.HealthServicesClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 싱글톤(애플리케이션 범위) 개체를 제공하는 Hilt 모듈
 */
@Module
@InstallIn(SingletonComponent::class)
class MainModule {
    @Singleton
    @Provides
    fun provideHealthServicesClient(@ApplicationContext context: Context) : HealthServicesClient =
        HealthServices.getClient(context)

}