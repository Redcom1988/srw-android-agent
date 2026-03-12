package com.redcom1988.data.di

import com.redcom1988.core.network.NetworkHelper
import com.redcom1988.core.network.NetworkPreference
import com.redcom1988.data.network.DataNetworkHelper
import com.redcom1988.data.remote.SRWApi
import com.redcom1988.data.repository.AuthRepositoryImpl
import com.redcom1988.data.repository.SubmissionRepositoryImpl
import com.redcom1988.data.repository.TokenStorageImpl
import com.redcom1988.domain.auth.repository.AuthRepository
import com.redcom1988.domain.auth.repository.TokenStorage
import com.redcom1988.domain.submission.repository.SubmissionRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val dataModule = module {
    single<NetworkHelper>(named("authenticated")) {
        DataNetworkHelper(
            networkHelper = get(named("default")),
            context = androidContext(),
            isDebugBuild = true,
            preference = get(),
        )
    }

    single { SRWApi(get(named("authenticated")), get()) }

    single<TokenStorage> { TokenStorageImpl(get()) }
    single<AuthRepository> { AuthRepositoryImpl(get()) }
    single<SubmissionRepository> { SubmissionRepositoryImpl(get()) }
}