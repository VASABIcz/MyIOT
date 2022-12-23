package cz.vasabi.myiot.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.vasabi.myiot.backend.database.AppDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(val database: AppDatabase): ViewModel() {
    private val scope = viewModelScope
    private val ioScope = CoroutineScope(Dispatchers.IO)

    fun clearDatabase() = ioScope.launch {
        database.deviceDao().wipeTable()
        database.httpCapabilityDao().wipeTable()
        database.httpConnectionsDao().wipeTable()
    }
}