package com.letsdoit.app.integrations.alarm

import android.app.AlarmManager
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ExactAlarmPermissionStatus(
    val allowed: Boolean,
    val requestAvailable: Boolean
)

interface ExactAlarmPermissionRepository {
    val status: StateFlow<ExactAlarmPermissionStatus>
    fun isExactAlarmAllowed(): Boolean
    suspend fun refresh()
    suspend fun setExactAlarmAllowed(allowed: Boolean)
}

@Singleton
class DefaultExactAlarmPermissionRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val alarmManager: AlarmManager,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ExactAlarmPermissionRepository {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val key = booleanPreferencesKey("exact_alarm_allowed")
    private val defaultAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S
    private val defaultStatus = ExactAlarmPermissionStatus(defaultAllowed, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
    private val _status = MutableStateFlow(defaultStatus)

    override val status: StateFlow<ExactAlarmPermissionStatus> = _status.asStateFlow()

    init {
        scope.launch {
            dataStore.data
                .map { preferences -> preferences[key] ?: defaultAllowed }
                .collect { allowed ->
                    _status.value = ExactAlarmPermissionStatus(allowed, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                }
        }
        scope.launch {
            if (defaultAllowed) {
                dataStore.edit { preferences -> preferences[key] = true }
            } else {
                refresh()
            }
        }
    }

    override fun isExactAlarmAllowed(): Boolean = status.value.allowed

    override suspend fun refresh() {
        val allowed = determineAllowed()
        setExactAlarmAllowed(allowed)
    }

    private fun determineAllowed(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    override suspend fun setExactAlarmAllowed(allowed: Boolean) {
        withContext(dispatcher) {
            dataStore.edit { preferences -> preferences[key] = allowed }
        }
    }
}
