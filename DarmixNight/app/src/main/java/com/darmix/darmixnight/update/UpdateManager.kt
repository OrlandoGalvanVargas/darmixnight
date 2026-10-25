package com.darmix.darmixnight.update

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface UpdateState {
    data object Hidden : UpdateState
    data class Available(val release: ReleaseInfo, val installed: String) : UpdateState
    data class NeedsPermission(val release: ReleaseInfo, val installed: String) : UpdateState
    data class Downloading(val release: ReleaseInfo, val installed: String, val downloaded: Long, val total: Long) : UpdateState
    data class Installing(val release: ReleaseInfo, val installed: String, val waitingUser: Boolean) : UpdateState
    data class Failed(val release: ReleaseInfo, val installed: String, val message: String) : UpdateState
}

enum class ManualCheck { IDLE, CHECKING, UP_TO_DATE, FOUND, OFFLINE }

object UpdateManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Hidden)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private val _manualCheck = MutableStateFlow(ManualCheck.IDLE)
    val manualCheck: StateFlow<ManualCheck> = _manualCheck.asStateFlow()

    private var checkJob: Job? = null
    private var manualJob: Job? = null
    private var downloadJob: Job? = null

    fun checkOnLaunch(context: Context) {
        val app = context.applicationContext
        if (checkJob?.isActive == true) return
        if (_state.value !is UpdateState.Hidden) return
        checkJob = scope.launch {
            val release = try {
                UpdateRepository.latestNewerRelease(app)
            } catch (_: Exception) {
                null
            }
            UpdateInstaller.clearOldDownloads(app, release)
            if (release != null && _state.value is UpdateState.Hidden) {
                _state.value = UpdateState.Available(release, UpdateRepository.installedVersion(app))
            }
        }
    }

    fun checkNow(context: Context) {
        val app = context.applicationContext
        if (manualJob?.isActive == true) return
        manualJob = scope.launch {
            _manualCheck.value = ManualCheck.CHECKING
            val release = try {
                UpdateRepository.refreshNewerRelease(app)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _manualCheck.value = ManualCheck.OFFLINE
                return@launch
            }
            if (release == null) {
                _manualCheck.value = ManualCheck.UP_TO_DATE
                return@launch
            }
            _manualCheck.value = ManualCheck.FOUND
            val s = _state.value
            if (s is UpdateState.Hidden || s is UpdateState.Failed) {
                _state.value = UpdateState.Available(release, UpdateRepository.installedVersion(app))
            }
        }
    }

    fun hasPendingRelease(context: Context): Boolean =
        UpdateRepository.cachedNewerRelease(context.applicationContext) != null

    fun dismiss() {
        downloadJob?.cancel()
        _state.value = UpdateState.Hidden
    }

    fun startUpdate(context: Context) {
        val app = context.applicationContext
        val (release, installed) = current() ?: return
        if (!UpdateInstaller.canInstall(app)) {
            _state.value = UpdateState.NeedsPermission(release, installed)
            return
        }
        if (downloadJob?.isActive == true) return
        downloadJob = scope.launch {
            _state.value = UpdateState.Downloading(release, installed, 0L, release.apkSize)
            try {
                val apk = UpdateInstaller.download(app, release) { downloaded, total ->
                    scope.launch {
                        if (_state.value is UpdateState.Downloading) {
                            _state.value = UpdateState.Downloading(release, installed, downloaded, total)
                        }
                    }
                }
                _state.value = UpdateState.Installing(release, installed, waitingUser = false)
                UpdateInstaller.install(app, apk)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = UpdateState.Failed(
                    release,
                    installed,
                    "No se pudo descargar la actualización. Revisa tu conexión e inténtalo de nuevo."
                )
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        val (release, installed) = current() ?: return
        _state.value = UpdateState.Available(release, installed)
    }

    fun onResume(context: Context) {
        val s = _state.value
        if (s is UpdateState.NeedsPermission && UpdateInstaller.canInstall(context.applicationContext)) {
            startUpdate(context)
        }
    }

    internal fun onInstallerOpened() {
        val (release, installed) = current() ?: return
        _state.value = UpdateState.Installing(release, installed, waitingUser = true)
    }

    internal fun onInstalled() {
        _state.value = UpdateState.Hidden
    }

    internal fun onInstallCancelled() {
        val (release, installed) = current() ?: return
        _state.value = UpdateState.Available(release, installed)
    }

    internal fun onInstallFailed(message: String) {
        val (release, installed) = current() ?: return
        _state.value = UpdateState.Failed(release, installed, message)
    }

    private fun current(): Pair<ReleaseInfo, String>? = when (val s = _state.value) {
        is UpdateState.Available -> s.release to s.installed
        is UpdateState.NeedsPermission -> s.release to s.installed
        is UpdateState.Downloading -> s.release to s.installed
        is UpdateState.Installing -> s.release to s.installed
        is UpdateState.Failed -> s.release to s.installed
        UpdateState.Hidden -> null
    }
}
