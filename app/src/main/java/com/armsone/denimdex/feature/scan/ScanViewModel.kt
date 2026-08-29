package com.armsone.denimdex.feature.scan

import android.app.Application
import android.net.Uri
import android.view.ViewGroup
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.armsone.denimdex.core.aibi.AIBILoginStatusStore
import com.armsone.denimdex.core.aibi.AIBIProviderRegistry
import com.armsone.denimdex.core.aibi.LoginStatus
import com.armsone.denimdex.core.data.local.CollectionRepository
import com.armsone.denimdex.core.data.local.UserPreferences
import com.armsone.denimdex.core.model.CollectionItem
import com.armsone.denimdex.core.model.QuickValueResult
import com.armsone.denimdex.core.model.VerificationState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    val registry = AIBIProviderRegistry(application)
    val loginStore = AIBILoginStatusStore(application, registry)
    val repository = CollectionRepository(application)
    val userPreferences = UserPreferences(application)
    val runner = QuickValueRunner(application, registry)

    private val _photos = MutableStateFlow<List<ByteArray>>(emptyList())
    val photos: StateFlow<List<ByteArray>> = _photos.asStateFlow()

    private val _latestResult = MutableStateFlow<QuickValueResult?>(null)
    val latestResult: StateFlow<QuickValueResult?> = _latestResult.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    private val _showLoginSheet = MutableStateFlow(false)
    val showLoginSheet: StateFlow<Boolean> = _showLoginSheet.asStateFlow()

    private val _showConsentDialog = MutableStateFlow(false)
    val showConsentDialog: StateFlow<Boolean> = _showConsentDialog.asStateFlow()

    private val _showClearAllConfirm = MutableStateFlow(false)
    val showClearAllConfirm: StateFlow<Boolean> = _showClearAllConfirm.asStateFlow()

    private val _showCamera = MutableStateFlow(false)
    val showCamera: StateFlow<Boolean> = _showCamera.asStateFlow()

    private val _showPhotoSaveAlert = MutableStateFlow(false)
    val showPhotoSaveAlert: StateFlow<Boolean> = _showPhotoSaveAlert.asStateFlow()

    fun addPhotos(newBytes: List<ByteArray>) {
        val current = _photos.value.toMutableList()
        for (b in newBytes) {
            if (current.size < 30) {
                current.add(b)
            }
        }
        _photos.value = current
    }

    fun addPhotosFromUris(uris: List<Uri>) {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) {
                val loaded = mutableListOf<ByteArray>()
                for (uri in uris) {
                    try {
                        val inputStream: InputStream? = getApplication<Application>().contentResolver.openInputStream(uri)
                        val bytes = inputStream?.use { it.readBytes() }
                        if (bytes != null && bytes.isNotEmpty()) {
                            loaded.add(bytes)
                        }
                    } catch (_: Exception) {}
                }
                loaded
            }
            addPhotos(list)
        }
    }

    fun removePhotoAt(index: Int) {
        val current = _photos.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _photos.value = current
        }
    }

    fun reorderPhoto(fromIndex: Int, toIndex: Int) {
        val current = _photos.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices && fromIndex != toIndex) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            _photos.value = current
        }
    }

    fun clearAllPhotos() {
        _photos.value = emptyList()
        _showClearAllConfirm.value = false
    }

    fun requestClearAllPhotos() {
        if (_photos.value.isNotEmpty()) {
            _showClearAllConfirm.value = true
        }
    }

    fun dismissClearAllConfirm() {
        _showClearAllConfirm.value = false
    }

    fun openCamera() {
        _showCamera.value = true
    }

    fun closeCamera() {
        _showCamera.value = false
    }

    fun onPhotoSaveIssue() {
        _showPhotoSaveAlert.value = true
    }

    fun dismissPhotoSaveAlert() {
        _showPhotoSaveAlert.value = false
    }

    fun onStartValuationClicked(hiddenContainer: ViewGroup) {
        if (_photos.value.isEmpty()) return

        // 1. Check Login
        loginStore.checkStatus()
        val currentLogin = loginStore.status.value
        if (currentLogin == LoginStatus.LOGIN_REQUIRED || userPreferences.explicitChatGPTLogout) {
            _showLoginSheet.value = true
            return
        }

        // 2. Check Consent
        if (!userPreferences.didAcknowledgeAITransfer) {
            _showConsentDialog.value = true
            return
        }

        startRunner(hiddenContainer)
    }

    fun onConsentAgreed(hiddenContainer: ViewGroup) {
        userPreferences.didAcknowledgeAITransfer = true
        _showConsentDialog.value = false
        startRunner(hiddenContainer)
    }

    fun onConsentCancelled() {
        _showConsentDialog.value = false
    }

    fun onLoginSuccess(hiddenContainer: ViewGroup) {
        _showLoginSheet.value = false
        userPreferences.explicitChatGPTLogout = false
        loginStore.setStatus(LoginStatus.LOGGED_IN)

        if (!userPreferences.didAcknowledgeAITransfer) {
            _showConsentDialog.value = true
        } else {
            startRunner(hiddenContainer)
        }
    }

    fun dismissLoginSheet() {
        _showLoginSheet.value = false
    }

    private fun startRunner(hiddenContainer: ViewGroup) {
        _isSaved.value = false
        runner.run(_photos.value, hiddenContainer) { result ->
            _latestResult.value = result
        }
    }

    fun saveToArchive() {
        val result = _latestResult.value ?: return
        if (_isSaved.value) return

        viewModelScope.launch {
            val item = CollectionItem(
                userTitle = "",
                brandGuess = result.productGuess.brand,
                modelGuess = result.productGuess.model,
                eraGuess = result.productGuess.era,
                variantGuess = result.productGuess.variant,
                estimatedProductionYear = result.productGuess.estimatedProductionYear,
                estimatedFactory = result.productGuess.estimatedFactory,
                summary = result.summary,
                confidence = result.confidence,
                condition = result.condition,
                rarityLevel = result.rarityLevel,
                raritySummary = result.raritySummary,
                rarityReasons = result.rarityReasons,
                koreaFairPurchaseLow = result.koreaFairPurchaseRange.low,
                koreaFairPurchaseHigh = result.koreaFairPurchaseRange.high,
                japanFairPurchaseLow = result.japanFairPurchaseRange.low,
                japanFairPurchaseHigh = result.japanFairPurchaseRange.high,
                koreaSaleLow = result.koreaSaleRange.low,
                koreaSaleHigh = result.koreaSaleRange.high,
                japanSaleLow = result.japanSaleRange.low,
                japanSaleHigh = result.japanSaleRange.high,
                jpyToKrwRate = result.jpyToKrwRate,
                verificationState = VerificationState.AI_ESTIMATE,
                userNotes = "",
                rawAiResponseJson = result.rawJson
            )

            repository.saveItem(item, _photos.value)
            _isSaved.value = true
        }
    }

    fun setPhotos(newPhotos: List<ByteArray>) {
        _photos.value = newPhotos
    }

    fun setLatestResult(result: QuickValueResult?) {
        _latestResult.value = result
    }

    fun openLoginSheet() {
        _showLoginSheet.value = true
    }

    fun openConsentDialog() {
        _showConsentDialog.value = true
    }

    fun dismissConsentDialog() {
        _showConsentDialog.value = false
    }

    fun openClearAllConfirm() {
        _showClearAllConfirm.value = true
    }

    fun resetValuation() {
        runner.reset()
        _latestResult.value = null
        _isSaved.value = false
    }

    override fun onCleared() {
        super.onCleared()
        runner.cancel()
    }
}
