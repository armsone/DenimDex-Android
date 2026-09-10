package com.armsone.denimdex.feature.settings

import android.app.Application
import android.content.Intent
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.armsone.denimdex.core.aibi.AIBIDiagnosticsStore
import com.armsone.denimdex.core.aibi.AIBILoginStatusStore
import com.armsone.denimdex.core.aibi.AIBIProviderRegistry
import com.armsone.denimdex.core.aibi.LoginStatus
import com.armsone.denimdex.core.data.local.CollectionRepository
import com.armsone.denimdex.core.data.local.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    val registry = AIBIProviderRegistry(application)
    val loginStore = AIBILoginStatusStore(application, registry)
    val repository = CollectionRepository(application)
    val userPreferences = UserPreferences(application)
    val diagnosticsStore = AIBIDiagnosticsStore.getInstance(application)

    val loginStatus: StateFlow<LoginStatus> = loginStore.status

    private val _showLoginSheet = MutableStateFlow(false)
    val showLoginSheet: StateFlow<Boolean> = _showLoginSheet.asStateFlow()

    private val _showClearSessionConfirm = MutableStateFlow(false)
    val showClearSessionConfirm: StateFlow<Boolean> = _showClearSessionConfirm.asStateFlow()

    private val _showSessionClearedAlert = MutableStateFlow(false)
    val showSessionClearedAlert: StateFlow<Boolean> = _showSessionClearedAlert.asStateFlow()

    private val _showClearArchiveConfirm = MutableStateFlow(false)
    val showClearArchiveConfirm: StateFlow<Boolean> = _showClearArchiveConfirm.asStateFlow()

    val archiveCount: StateFlow<Int> get() = _archiveCount
    private val _archiveCount = MutableStateFlow(0)

    private val _diagnosticsShareIntent = MutableStateFlow<Intent?>(null)
    val diagnosticsShareIntent: StateFlow<Intent?> = _diagnosticsShareIntent.asStateFlow()

    private val _diagnosticsMessage = MutableStateFlow<String?>(null)
    val diagnosticsMessage: StateFlow<String?> = _diagnosticsMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.items.collect { items ->
                _archiveCount.value = items.size
            }
        }
    }

    fun openLoginSheet() {
        _showLoginSheet.value = true
    }

    fun dismissLoginSheet() {
        _showLoginSheet.value = false
        loginStore.checkStatus()
    }

    fun refreshLoginStatus(container: ViewGroup) {
        loginStore.checkStatus(container)
    }

    fun onLoginSuccess() {
        _showLoginSheet.value = false
        userPreferences.explicitChatGPTLogout = false
        loginStore.setStatus(LoginStatus.LOGGED_IN)
    }

    fun requestClearSession() {
        _showClearSessionConfirm.value = true
    }

    fun dismissClearSessionConfirm() {
        _showClearSessionConfirm.value = false
    }

    fun confirmClearSession() {
        _showClearSessionConfirm.value = false
        AIBIProviderRegistry.clearChatGPTWebSession {
            userPreferences.explicitChatGPTLogout = true
            loginStore.setStatus(LoginStatus.LOGIN_REQUIRED)
            _showSessionClearedAlert.value = true
        }
    }

    fun dismissSessionClearedAlert() {
        _showSessionClearedAlert.value = false
    }

    fun requestClearArchive() {
        if (_archiveCount.value > 0) {
            _showClearArchiveConfirm.value = true
        }
    }

    /** Debug catalog only: exposes the confirmation surface without mutating archive data. */
    fun showClearArchiveConfirmDirectly() {
        _showClearArchiveConfirm.value = true
    }

    fun dismissClearArchiveConfirm() {
        _showClearArchiveConfirm.value = false
    }

    fun setLoginStatus(status: LoginStatus) {
        loginStore.setStatus(status)
    }

    fun showSessionClearedAlertDirectly() {
        _showSessionClearedAlert.value = true
    }

    fun confirmClearArchive() {
        _showClearArchiveConfirm.value = false
        viewModelScope.launch {
            repository.clearAllItems()
        }
    }

    fun shareLatestDiagnostics() {
        val latestFile = diagnosticsStore.latestExportFile
        if (latestFile == null) {
            val error = diagnosticsStore.storageError ?: "공유할 최근 진단 로그가 없습니다."
            _diagnosticsMessage.value = error
            return
        }

        try {
            val app = getApplication<Application>()
            val exportDir = File(app.cacheDir, "aibi_diagnostics").apply { mkdirs() }
            exportDir.listFiles()?.forEach { it.delete() }

            val exportFile = File(exportDir, latestFile.name)
            latestFile.copyTo(exportFile, overwrite = true)

            val uri = FileProvider.getUriForFile(
                app,
                "${app.packageName}.fileprovider",
                exportFile
            )

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "AIBI Diagnostic Log: ${latestFile.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(sendIntent, "최근 진단 로그 공유").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            _diagnosticsShareIntent.value = chooser
        } catch (e: Throwable) {
            _diagnosticsMessage.value = "진단 로그 공유 준비 실패: ${e.message ?: "알 수 없는 오류"}"
        }
    }

    fun onDiagnosticsShareConsumed() {
        _diagnosticsShareIntent.value = null
    }

    fun dismissDiagnosticsMessage() {
        _diagnosticsMessage.value = null
    }

    override fun onCleared() {
        loginStore.close()
        super.onCleared()
    }
}
