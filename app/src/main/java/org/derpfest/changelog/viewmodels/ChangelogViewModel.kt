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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.derpfest.changelog.ChangelogApplication
import org.derpfest.changelog.ext.TAP_ACTION_KEY
import org.derpfest.changelog.ext.preferenceFlow
import org.derpfest.changelog.ext.sharedPreferences
import org.derpfest.changelog.ext.tapAction
import org.derpfest.changelog.models.Change
import org.derpfest.changelog.models.ChangelogEntry
import org.derpfest.changelog.models.ChangelogError
import org.derpfest.changelog.models.Period
import java.time.Instant
import java.time.ZoneId

/**
 * View model of the changelog screen.
 */
class ChangelogViewModel(application: Application) : AndroidViewModel(application) {
    private val changelogRepository = getApplication<ChangelogApplication>().changelogRepository

    private val sharedPreferences = application.sharedPreferences

    /**
     * What tapping a change does.
     */
    val tapAction = sharedPreferences.preferenceFlow(
        TAP_ACTION_KEY,
        getter = SharedPreferences::tapAction,
    )
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = sharedPreferences.tapAction,
        )

    /**
     * The changelog, null until it got loaded or fetched.
     */
    val changelog = changelogRepository.changelog

    /**
     * The entries of the list, null until the changelog got loaded or fetched.
     */
    val entries = changelog
        .map { changelog ->
            changelog?.let {
                val since = PERIOD.startTime(System.currentTimeMillis())
                it.changes.filter { change -> change.timestamp >= since }.toEntries()
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    /**
     * Whether the changelog is being refreshed.
     */
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    /**
     * The error of the last refresh, until it got dismissed.
     */
    private val _error = MutableStateFlow<ChangelogError?>(null)
    val error = _error.asStateFlow()

    private var refreshJob: Job? = null

    init {
        refresh(force = false)
    }

    /**
     * Refresh the changelog, unless a refresh is already running.
     *
     * @param force Whether to refresh even if the changelog got refreshed moments ago
     */
    fun refresh(force: Boolean = true) {
        if (refreshJob?.isActive == true) {
            return
        }

        refreshJob = viewModelScope.launch {
            _isRefreshing.value = true
            _error.value = null

            try {
                changelogRepository.refresh(PERIOD, force)
            } catch (e: ChangelogError) {
                _error.value = e
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /**
     * Dismiss the error of the last refresh.
     */
    fun dismissError() {
        _error.value = null
    }

    companion object {
        private val PERIOD = Period.SINCE_BUILD

        /**
         * Group the changes by the day they got merged on, keeping their order.
         */
        private fun List<Change>.toEntries(): List<ChangelogEntry> {
            val zoneId = ZoneId.systemDefault()

            return groupBy {
                Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate()
            }.flatMap { (date, changes) ->
                listOf(ChangelogEntry.Day(date)) + changes.mapIndexed { index, change ->
                    ChangelogEntry.Item(
                        change = change,
                        index = index,
                        count = changes.size,
                    )
                }
            }
        }
    }
}
