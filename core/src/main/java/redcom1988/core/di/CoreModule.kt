package redcom1988.core.di

import android.content.Context
import redcom1988.core.network.NetworkHelper
import redcom1988.core.preference.AndroidPreferenceStore
import redcom1988.core.preference.PreferenceStore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val coreModule = module {
    single { NetworkHelper(androidContext(), true) }
    single<PreferenceStore> {
        AndroidPreferenceStore(
            androidContext().getSharedPreferences("app_pref", Context.MODE_PRIVATE)
        )
    }
}