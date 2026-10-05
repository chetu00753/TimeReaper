package com.timereaper.app.alarm

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

object AlarmScheduler {

    suspend fun schedule(
        context: Context,
        startHour: Int,
        startMinute: Int,
        count: Int,
        intervalMinutes: Int,
        label: String
    ): Pair<List<String>, Int> {
        val created = mutableListOf<String>()
        val seen = HashSet<String>()
        var skipped = 0

        var hour = startHour
        var minute = startMinute

        repeat(count) {
            val key = "%02d:%02d".format(hour, minute)
            if (seen.add(key)) {
                val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_HOUR, hour)
                    putExtra(AlarmClock.EXTRA_MINUTES, minute)
                    putExtra(AlarmClock.EXTRA_MESSAGE, label)
                    putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                withContext(Dispatchers.Main) {
                    context.startActivity(intent)
                }
                created.add(key)
                delay(80)
            } else {
                skipped++
            }

            val totalMinutes = hour * 60 + minute + intervalMinutes
            hour = (totalMinutes / 60) % 24
            minute = totalMinutes % 60
        }

        return Pair(created, skipped)
    }
}
