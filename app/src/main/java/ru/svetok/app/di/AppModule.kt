package ru.svetok.app.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import ru.svetok.app.data.admin.AdminSessionPrefs
import ru.svetok.app.data.admin.HttpAdminRepository
import ru.svetok.app.data.complaint.HttpComplaintRepository
import ru.svetok.app.data.geo.GeoJsonRepository
import ru.svetok.app.data.onboarding.OnboardingPrefs
import ru.svetok.app.data.outage.HttpOutageRepository
import ru.svetok.app.data.outage.LocalOutageRepository
import ru.svetok.app.data.outage.OutageDatabase
import ru.svetok.app.data.outage.OutageRepository
import ru.svetok.app.data.outage.RoomOutageRepository
import ru.svetok.app.data.outage.createHttpClient
import ru.svetok.app.data.outage.createOutageDatabase
import ru.svetok.app.data.outage.loadApiConfig
import ru.svetok.app.data.subscription.HttpSubscriptionRepository
import ru.svetok.app.data.subscription.SubscriptionPrefs
import ru.svetok.app.ui.admin.AdminViewModel
import ru.svetok.app.ui.map.MapViewModel
import ru.svetok.app.ui.outages.OutagesListViewModel
import ru.svetok.app.ui.settings.FcmTokenProvider
import ru.svetok.app.ui.settings.SettingsViewModel
import ru.svetok.app.util.DeviceIdProvider

val appModule = module {
    single { GeoJsonRepository(get()) }
    single { LocalOutageRepository() }
    single { loadApiConfig() }
    single { createHttpClient() }
    single { DeviceIdProvider(get()) }
    single { createOutageDatabase(get()) }
    single { get<OutageDatabase>().outageCacheDao() }
    single { HttpOutageRepository(get(), get(), get()) }
    single { HttpComplaintRepository(get(), get(), get()) }
    single { HttpSubscriptionRepository(get(), get(), get()) }
    single { SubscriptionPrefs(get()) }
    single { FcmTokenProvider() }
    single<OutageRepository> { RoomOutageRepository(get(), get(), get()) }
    // Admin
    single { AdminSessionPrefs(get()) }
    single { HttpAdminRepository(get(), get(), get()) }
    // Onboarding
    single { OnboardingPrefs(get()) }
    // ViewModels
    viewModel { MapViewModel(get(), get()) }
    viewModel { SettingsViewModel(get(), get(), get(), get()) }
    viewModel { AdminViewModel(get(), get()) }
    viewModel { OutagesListViewModel(get(), get(), get()) }
}
