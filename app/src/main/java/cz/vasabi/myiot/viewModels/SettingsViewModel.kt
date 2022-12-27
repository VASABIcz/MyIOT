package cz.vasabi.myiot.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.vasabi.myiot.backend.database.AppDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(val database: AppDatabase): ViewModel() {
    fun clearDatabase() = viewModelScope.launch(Dispatchers.IO) {
        database.deviceDao().wipeTable()
        database.httpCapabilityDao().wipeTable()
        database.httpConnectionsDao().wipeTable()
    }
}