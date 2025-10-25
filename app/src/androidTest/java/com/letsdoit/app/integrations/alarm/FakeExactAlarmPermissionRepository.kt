package com.letsdoit.app.integrations.alarm

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class FakeExactAlarmPermissionRepository @Inject constructor() : ExactAlarmPermissionRepository {
    private val _status = MutableStateFlow(ExactAlarmPermissionStatus(allowed = false, requestAvailable = true))
    override val status: StateFlow<ExactAlarmPermissionStatus> = _status.asStateFlow()

    override fun isExactAlarmAllowed(): Boolean = _status.value.allowed

    override suspend fun refresh() = Unit

    override suspend fun setExactAlarmAllowed(allowed: Boolean) {
        _status.value = _status.value.copy(allowed = allowed)
    }

    fun setState(allowed: Boolean, requestAvailable: Boolean) {
        _status.value = ExactAlarmPermissionStatus(allowed, requestAvailable)
    }
}
