package com.redcom1988.srwagent.di

import com.redcom1988.srwagent.data.RouteService
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single { RouteService(androidContext()) }
}
