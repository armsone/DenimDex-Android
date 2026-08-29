package com.armsone.denimdex.feature.archive

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.armsone.denimdex.core.data.local.CollectionRepository
import com.armsone.denimdex.core.data.local.UserPreferences
import com.armsone.denimdex.core.model.CollectionItem
import com.armsone.denimdex.core.model.SyncEligibilityState
import com.armsone.denimdex.core.model.VerificationState
import com.armsone.denimdex.core.sync.DisabledDenimDexSyncClient
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ArchiveViewModel(application: Application) : AndroidViewModel(application) {

    val repository = CollectionRepository(application)
    val userPreferences = UserPreferences(application)
    val syncClient = DisabledDenimDexSyncClient()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedItem = MutableStateFlow<CollectionItem?>(null)
    val selectedItem: StateFlow<CollectionItem?> = _selectedItem.asStateFlow()

    private val _showSyncInvite = MutableStateFlow(false)
    val showSyncInvite: StateFlow<Boolean> = _showSyncInvite.asStateFlow()

    private val _showSyncDisclosure = MutableStateFlow(false)
    val showSyncDisclosure: StateFlow<Boolean> = _showSyncDisclosure.asStateFlow()

    private val _syncErrorMessage = MutableStateFlow<String?>(null)
    val syncErrorMessage: StateFlow<String?> = _syncErrorMessage.asStateFlow()

    val filteredItems: StateFlow<List<CollectionItem>> = combine(
        repository.items,
        _searchQuery
    ) { items, query ->
        if (query.isBlank()) {
            items
        } else {
            val q = query.trim().lowercase()
            items.filter { item ->
                item.brandGuess.lowercase().contains(q) ||
                    item.modelGuess.lowercase().contains(q) ||
                    item.userTitle.lowercase().contains(q) ||
                    item.userNotes.lowercase().contains(q) ||
                    item.summary.lowercase().contains(q)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun selectItem(item: CollectionItem) {
        _selectedItem.value = item
    }

    fun clearSelectedItem() {
        _selectedItem.value = null
    }

    fun updateItem(item: CollectionItem) {
        viewModelScope.launch {
            repository.updateItem(item)
            _selectedItem.value = item
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            repository.deleteItem(id)
            if (_selectedItem.value?.id == id) {
                _selectedItem.value = null
            }
        }
    }

    fun checkSyncInvitationOnAppear() {
        val totalCount = repository.items.value.size
        val lastCount = userPreferences.lastSyncPromptCount
        if (totalCount >= 10 && (totalCount - lastCount) >= 10) {
            _showSyncInvite.value = true
        }
    }

    fun onDismissSyncInvite() {
        _showSyncInvite.value = false
        userPreferences.lastSyncPromptCount = repository.items.value.size
    }

    fun onOpenSyncDisclosure() {
        _showSyncDisclosure.value = true
    }

    fun onDismissSyncDisclosure() {
        _showSyncDisclosure.value = false
    }

    fun onAttemptSync() {
        viewModelScope.launch {
            val result = syncClient.syncCollection(repository.items.value)
            if (result.isFailure) {
                _syncErrorMessage.value = result.exceptionOrNull()?.message ?: "동기화 설정이 되어있지 않습니다."
            }
        }
    }

    /** Debug catalog only: replaces the observable list without touching the user's SQLite data. */
    fun setCatalogItems(seedList: List<CollectionItem>) {
        repository.setInMemoryItems(seedList)
    }

    fun openSyncInvite() {
        _showSyncInvite.value = true
    }

    fun dismissSyncInvite() {
        _showSyncInvite.value = false
    }

    fun openSyncDisclosure() {
        _showSyncDisclosure.value = true
    }

    fun dismissSyncDisclosure() {
        _showSyncDisclosure.value = false
    }

    fun dismissSyncError() {
        _syncErrorMessage.value = null
    }
}
