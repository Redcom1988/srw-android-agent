package com.redcom1988.domain.di

import com.redcom1988.domain.auth.interactor.Login
import com.redcom1988.domain.auth.interactor.Logout
import com.redcom1988.domain.auth.interactor.RefreshToken
import com.redcom1988.domain.preference.ApplicationPreference
import com.redcom1988.domain.submission.interactor.FinishPickup
import com.redcom1988.domain.submission.interactor.GetRecentSubmissions
import com.redcom1988.domain.submission.interactor.GetSubmissions
import org.koin.dsl.module

val domainModule = module {
    single { ApplicationPreference(get()) }

    single { Login(get(), get()) }
    single { Logout(get(), get()) }
    single { RefreshToken(get(), get()) }

    single { GetSubmissions(get()) }
    single { GetRecentSubmissions(get()) }
    single { FinishPickup(get()) }
}