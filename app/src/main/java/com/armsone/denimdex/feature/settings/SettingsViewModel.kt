package com.armsone.denimdex.feature.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.armsone.denimdex.core.aibi.AIBILoginStatusStore
import com.armsone.denimdex.core.aibi.AIBIProviderRegistry
import com.armsone.denimdex.core.aibi.LoginStatus
import com.armsone.denimdex.core.data.local.CollectionRepository
import com.armsone.denimdex.core.data.local.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    val registry = AIBIProviderRegistry(application)
    val loginStore = AIBILoginStatusStore(application, registry)
    val repository = CollectionRepository(application)
    val userPreferences = UserPreferences(application)

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
}
