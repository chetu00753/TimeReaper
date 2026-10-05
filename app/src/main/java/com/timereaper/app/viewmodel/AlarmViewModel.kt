package com.timereaper.app.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timereaper.app.alarm.AlarmScheduler
import com.timereaper.app.model.AlarmResult
import com.timereaper.app.model.QuickPreset
import kotlinx.coroutines.launch
import java.util.Calendar

class AlarmViewModel : ViewModel() {

    var startHour by mutableStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
        private set
    var startMinute by mutableStateOf(Calendar.getInstance().get(Calendar.MINUTE))
        private set
    var count by mutableStateOf("5")
    var intervalMinutes by mutableStateOf("")
    var label by mutableStateOf("")
    var selectedPreset by mutableStateOf<QuickPreset?>(null)
        private set
    var result by mutableStateOf<AlarmResult?>(null)
        private set
    var isCreating by mutableStateOf(false)
        private set
    var showTimePicker by mutableStateOf(false)

    fun setTime(hour: Int, minute: Int) {
        startHour = hour
        startMinute = minute
        selectedPreset = null
    }

    fun applyPreset(preset: QuickPreset) {
        selectedPreset = if (selectedPreset == preset) null else preset
        if (selectedPreset != null) {
            val cal = Calendar.getInstance().apply { add(Calendar.MINUTE, preset.minutes) }
            startHour = cal.get(Calendar.HOUR_OF_DAY)
            startMinute = cal.get(Calendar.MINUTE)
        }
    }

    fun createAlarms(context: Context) {
        val countVal = count.toIntOrNull()?.coerceIn(1, 500) ?: return
        val intervalVal = intervalMinutes.toIntOrNull()?.coerceIn(1, 1440) ?: return

        isCreating = true
        result = null

        viewModelScope.launch {
            val (created, skipped) = AlarmScheduler.schedule(
                context = context,
                startHour = startHour,
                startMinute = startMinute,
                count = countVal,
                intervalMinutes = intervalVal,
                label = label.ifBlank { "Time Reaper" }
            )
            result = AlarmResult(created = created, skipped = skipped)
            isCreating = false
        }
    }
}
