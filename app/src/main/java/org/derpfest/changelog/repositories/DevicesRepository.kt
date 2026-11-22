/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.repositories

import android.content.Context
import android.os.Build
import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.derpfest.changelog.R
import org.derpfest.changelog.datasources.DevicesDataSource
import org.derpfest.changelog.ext.readJson
import org.derpfest.changelog.ext.writeJson
import org.derpfest.changelog.models.ChangelogError
import org.derpfest.changelog.models.Device
import org.derpfest.changelog.utils.RepositoryNames
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Keeps the list of DerpFest devices and resolves which DerpFest-Devices repositories belong to
 * a set of watched devices.
 */
class DevicesRepository(context: Context) {
    private val organization = context.getString(R.string.github_devices_organization)

    private val dataSource = DevicesDataSource(
        indexUrl = context.getString(R.string.devices_index_url),
        organization = organization,
    )

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val devicesCacheFile = AtomicFile(File(context.cacheDir, DEVICES_CACHE_FILE_NAME))
    private val repositoriesCacheFile = AtomicFile(
        File(context.cacheDir, REPOSITORIES_CACHE_FILE_NAME)
    )

    private val mutex = Mutex()

    private val _devices = MutableStateFlow<List<Device>?>(null)

    /**
     * The devices DerpFest ships, null until they got loaded or fetched.
     */
    val devices = _devices.asStateFlow()

    private val _watchedRepositories = MutableStateFlow<Set<String>?>(null)

    /**
     * The DerpFest-Devices repositories of the watched devices, as `owner/name` in lower case.
     * Null until they got resolved.
     */
    val watchedRepositories = _watchedRepositories.asStateFlow()

    private val _thisDeviceRepositories = MutableStateFlow<Set<String>?>(null)

    /**
     * The DerpFest-Devices repositories of the device running the app, as `owner/name` in lower
     * case. Null until they got resolved.
     */
    val thisDeviceRepositories = _thisDeviceRepositories.asStateFlow()

    /**
     * Load the devices kept from the last refresh.
     */
    suspend fun load() = mutex.withLock {
        if (_devices.value == null) {
            _devices.value = withContext(Dispatchers.IO) {
                devicesCacheFile.readJson<DevicesCache>(json)
                    ?.takeIf { System.currentTimeMillis() - it.updatedAt < CACHE_MAX_AGE }
                    ?.devices
            }
        }
    }

    /**
     * Refresh the list of devices.
     *
     * @throws ChangelogError If the devices couldn't be refreshed
     */
    suspend fun refresh(): List<Device> = mutex.withLock {
        loadDevicesLocked(force = true)
    }

    /**
     * Resolve the DerpFest-Devices repositories that belong to [codenames].
     *
     * @param codenames The codenames of the devices
     * @throws ChangelogError If the repositories couldn't be resolved
     */
    suspend fun resolveRepositories(codenames: Set<String>): Set<String> = mutex.withLock {
        val resolved = resolveLocked(codenames)
        _watchedRepositories.value = resolved
        resolved
    }

    /**
     * Resolve the DerpFest-Devices repositories of the device running the app.
     *
     * @throws ChangelogError If the repositories couldn't be resolved
     */
    suspend fun resolveThisDevice(): Set<String> = mutex.withLock {
        val resolved = resolveLocked(setOf(thisDeviceCodename))
        _thisDeviceRepositories.value = resolved
        resolved
    }

    private suspend fun resolveLocked(codenames: Set<String>): Set<String> {
        if (codenames.isEmpty()) {
            return emptySet()
        }

        val repositories = loadRepositoriesLocked(force = false)
        // android_ and proprietary_ are prefixes for the same tree, but both repositories can
        // exist. A dependency names one of them, so every match has to be kept.
        val byNormalizedName = repositories.groupBy {
            RepositoryNames.normalize(it.name).lowercase()
        }

        val roots = repositories.filter { repository ->
            codenames.any { RepositoryNames.matchesCodename(repository.name, it) }
        }

        val resolved = mutableSetOf<String>()
        val queue = ArrayDeque(roots)

        while (queue.isNotEmpty()) {
            val repository = queue.removeFirst()
            val fullName = "$organization/${repository.name}".lowercase()

            if (!resolved.add(fullName)) {
                continue
            }

            // lineage.dependencies and derp.dependencies together name the shared trees.
            val dependencies = try {
                dataSource.getDependencies(repository.name, repository.defaultBranch)
            } catch (e: ChangelogError) {
                // A missing tree shouldn't block the rest of the device
                if (e is ChangelogError.Http && e.code == 404) {
                    emptyList()
                } else {
                    throw e
                }
            }

            dependencies.flatMap { dependency ->
                byNormalizedName[RepositoryNames.normalize(dependency).lowercase()].orEmpty()
            }.forEach(queue::addLast)
        }

        return resolved
    }

    private suspend fun loadDevicesLocked(force: Boolean): List<Device> {
        if (!force) {
            _devices.value?.let { return it }

            val cached = withContext(Dispatchers.IO) {
                devicesCacheFile.readJson<DevicesCache>(json)
            }?.takeIf { System.currentTimeMillis() - it.updatedAt < CACHE_MAX_AGE }

            if (cached != null) {
                _devices.value = cached.devices
                return cached.devices
            }
        }

        val devices = try {
            dataSource.getDevices()
        } catch (e: ChangelogError) {
            val cached = withContext(Dispatchers.IO) {
                devicesCacheFile.readJson<DevicesCache>(json)
            }?.devices

            if (cached != null) {
                _devices.value = cached
                return cached
            }

            throw e
        }

        withContext(Dispatchers.IO) {
            devicesCacheFile.writeJson(
                json,
                DevicesCache(
                    updatedAt = System.currentTimeMillis(),
                    devices = devices,
                ),
            )
        }
        _devices.value = devices
        return devices
    }

    private suspend fun loadRepositoriesLocked(
        force: Boolean,
    ): List<DevicesDataSource.GitHubRepository> {
        if (!force) {
            val cached = withContext(Dispatchers.IO) {
                repositoriesCacheFile.readJson<RepositoriesCache>(json)
            }?.takeIf { System.currentTimeMillis() - it.updatedAt < CACHE_MAX_AGE }

            if (cached != null) {
                return cached.repositories
            }
        }

        val repositories = try {
            dataSource.getRepositories()
        } catch (e: ChangelogError) {
            val cached = withContext(Dispatchers.IO) {
                repositoriesCacheFile.readJson<RepositoriesCache>(json)
            }?.repositories

            if (cached != null) {
                return cached
            }

            throw e
        }

        withContext(Dispatchers.IO) {
            repositoriesCacheFile.writeJson(
                json,
                RepositoriesCache(
                    updatedAt = System.currentTimeMillis(),
                    repositories = repositories,
                ),
            )
        }

        return repositories
    }

    @Serializable
    private data class DevicesCache(
        val updatedAt: Long,
        val devices: List<Device>,
    )

    @Serializable
    private data class RepositoriesCache(
        val updatedAt: Long,
        val repositories: List<DevicesDataSource.GitHubRepository>,
    )

    companion object {
        private const val DEVICES_CACHE_FILE_NAME = "devices.json"
        private const val REPOSITORIES_CACHE_FILE_NAME = "device_repositories.json"

        private val CACHE_MAX_AGE = TimeUnit.DAYS.toMillis(1)

        /**
         * The codename of the device running the app.
         */
        val thisDeviceCodename: String
            get() = Build.DEVICE
    }
}
