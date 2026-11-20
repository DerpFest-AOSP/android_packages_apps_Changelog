/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.viewmodels

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.derpfest.changelog.ChangelogApplication
import org.derpfest.changelog.ext.DEVICE_FILTER_KEYS
import org.derpfest.changelog.ext.SHOW_ALL_DEVICES_KEY
import org.derpfest.changelog.ext.WATCHED_DEVICES_KEY
import org.derpfest.changelog.ext.preferenceFlow
import org.derpfest.changelog.ext.sharedPreferences
import org.derpfest.changelog.ext.showAllDevices
import org.derpfest.changelog.ext.watchedDevices
import org.derpfest.changelog.models.ChangelogError
import org.derpfest.changelog.models.Device
import org.derpfest.changelog.repositories.DevicesRepository

/**
 * View model of the devices screen.
 */
class DevicesViewModel(application: Application) : AndroidViewModel(application) {
    private val devicesRepository = getApplication<ChangelogApplication>().devicesRepository

    private val sharedPreferences = application.sharedPreferences

    /**
     * Whether changes of every DerpFest-Devices repository are shown.
     */
    val showAllDevices = sharedPreferences.preferenceFlow(
        SHOW_ALL_DEVICES_KEY,
        getter = SharedPreferences::showAllDevices,
    )
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = sharedPreferences.showAllDevices,
        )

    /**
     * The codenames of the watched devices.
     */
    val watchedDevices = sharedPreferences.preferenceFlow(
        WATCHED_DEVICES_KEY,
        getter = SharedPreferences::watchedDevices,
    )
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = sharedPreferences.watchedDevices,
        )

    /**
     * The DerpFest-Devices repositories of the watched devices.
     */
    val watchedRepositories = devicesRepository.watchedRepositories

    /**
     * The devices DerpFest ships.
     */
    val devices = devicesRepository.devices

    /**
     * The query used to filter the devices list.
     */
    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    /**
     * The devices matching the search query.
     */
    val filteredDevices = combine(devices, query) { devices, query ->
        devices?.filter { it.matches(query) }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    /**
     * Whether the devices are being refreshed.
     */
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    /**
     * The error of the last refresh, until it got dismissed.
     */
    private val _error = MutableStateFlow<ChangelogError?>(null)
    val error = _error.asStateFlow()

    /**
     * The codename of the device running the app.
     */
    val thisDeviceCodename = DevicesRepository.thisDeviceCodename

    private var refreshJob: Job? = null
    private var resolveJob: Job? = null

    init {
        viewModelScope.launch {
            devicesRepository.load()

            if (devices.value == null) {
                refresh()
            }
        }

        viewModelScope.launch {
            combine(
                sharedPreferences.preferenceFlow(
                    *DEVICE_FILTER_KEYS,
                    getter = {
                        showAllDevices to watchedDevices
                    },
                ),
                devices,
            ) { (showAllDevices, watchedDevices), devices ->
                Triple(showAllDevices, watchedDevices, devices)
            }
                .distinctUntilChanged()
                .collect { (showAllDevices, watchedDevices, _) ->
                    if (showAllDevices) {
                        resolveJob?.cancel()
                        return@collect
                    }

                    resolveWatchedRepositories(watchedDevices)
                }
        }
    }

    fun setQuery(query: String) {
        _query.value = query
    }

    fun setShowAllDevices(showAllDevices: Boolean) {
        sharedPreferences.showAllDevices = showAllDevices
    }

    fun setDeviceWatched(codename: String, watched: Boolean) {
        sharedPreferences.watchedDevices = when (watched) {
            true -> sharedPreferences.watchedDevices + codename
            false -> sharedPreferences.watchedDevices - codename
        }
    }

    /**
     * Refresh the list of devices.
     */
    fun refresh() {
        if (refreshJob?.isActive == true) {
            return
        }

        refreshJob = viewModelScope.launch {
            _isRefreshing.value = true
            _error.value = null

            try {
                devicesRepository.refresh()
            } catch (e: ChangelogError) {
                _error.value = e
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun dismissError() {
        _error.value = null
    }

    private fun resolveWatchedRepositories(codenames: Set<String>) {
        resolveJob?.cancel()
        resolveJob = viewModelScope.launch {
            try {
                devicesRepository.resolveRepositories(codenames)
            } catch (_: ChangelogError) {
                // Keep the previous resolution; the devices screen shows its own errors
            }
        }
    }
}

/**
 * Whether the device matches the search query.
 *
 * @param query The search query
 */
fun Device.matches(query: String): Boolean {
    if (query.isBlank()) {
        return true
    }

    val needle = query.trim()

    return displayName.contains(needle, ignoreCase = true) ||
            codename.contains(needle, ignoreCase = true) ||
            maintainer?.contains(needle, ignoreCase = true) == true
}
