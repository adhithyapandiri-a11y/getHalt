package com.example.indianbloom.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.indianbloom.data.database.BlockedAppEntity
import com.example.indianbloom.data.database.FocusSessionEntity
import com.example.indianbloom.data.database.NfcCardEntity
import com.example.indianbloom.data.repository.BloomRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainScreenViewModel(private val repository: BloomRepository) : ViewModel() {

    val blockedApps: StateFlow<List<BlockedAppEntity>> =
        repository.getBlockedAppsFlow().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val pairedCards: StateFlow<List<NfcCardEntity>> =
        repository.getCardsFlow().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val activeSession: StateFlow<FocusSessionEntity?> =
        repository.getActiveSessionFlow().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )

    fun addAppToBlocklist(packageName: String, appName: String) {
        viewModelScope.launch {
            repository.addAppToBlocklist(packageName, appName, isCustom = true)
        }
    }

    fun removeAppFromBlocklist(packageName: String) {
        viewModelScope.launch {
            repository.removeAppFromBlocklist(packageName)
        }
    }

    fun unregisterCard(uid: String) {
        viewModelScope.launch {
            repository.unregisterCard(uid)
        }
    }
}
