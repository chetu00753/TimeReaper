package com.timereaper.app.alarm

import android.content.Intent
import android.provider.AlarmClock
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleDestroyedException
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.withResumed
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

object AlarmScheduler {

    // How long to wait for the Clock app to take the foreground before treating the intent as dropped.
    private const val HANDOFF_TIMEOUT_MS = 3000L
    // Pause after returning from Clock so it can persist the alarm before the next intent.
    private const val SETTLE_DELAY_MS = 300L

    /**
     * Sends ACTION_SET_ALARM intents one at a time. Each intent is only launched while our activity
     * is in the foreground (otherwise Android blocks background activity starts), and the next one
     * waits until the Clock app has handed control back to us (otherwise Clock coalesces them).
     */
    suspend fun schedule(
        activity: ComponentActivity,
        startHour: Int,
        startMinute: Int,
        count: Int,
        intervalMinutes: Int,
        label: String,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }
    ): Pair<List<String>, Int> {
        val created = mutableListOf<String>()
        val seen = HashSet<String>()
        var skipped = 0

        var hour = startHour
        var minute = startMinute

        for (i in 0 until count) {
            val key = "%02d:%02d".format(hour, minute)
            if (seen.add(key)) {
                val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_HOUR, hour)
                    putExtra(AlarmClock.EXTRA_MINUTES, minute)
                    putExtra(AlarmClock.EXTRA_MESSAGE, label)
                    putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                }
                when (launchAndAwaitReturn(activity, intent)) {
                    LaunchOutcome.RETURNED -> created.add(key)
                    LaunchOutcome.NOT_DELIVERED -> {}
                    LaunchOutcome.ACTIVITY_GONE -> break
                }
                delay(SETTLE_DELAY_MS)
            } else {
                skipped++
            }
            onProgress(i + 1, count)

            val totalMinutes = hour * 60 + minute + intervalMinutes
            hour = (totalMinutes / 60) % 24
            minute = totalMinutes % 60
        }

        return Pair(created, skipped)
    }

    private enum class LaunchOutcome { RETURNED, NOT_DELIVERED, ACTIVITY_GONE }

    private suspend fun launchAndAwaitReturn(
        activity: ComponentActivity,
        intent: Intent
    ): LaunchOutcome = withContext(Dispatchers.Main) {
        val lifecycle = activity.lifecycle
        val handedOff = CompletableDeferred<Unit>()
        val returned = CompletableDeferred<Boolean>()

        // Observer (not a state flow) so a quick pause→resume from a no-UI Clock handler isn't conflated away.
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> handedOff.complete(Unit)
                Lifecycle.Event.ON_RESUME -> if (handedOff.isCompleted) returned.complete(true)
                Lifecycle.Event.ON_DESTROY -> returned.complete(false)
                else -> {}
            }
        }

        try {
            // Only launch while we're in the foreground; suspends until the user returns to the app.
            lifecycle.withResumed {
                lifecycle.addObserver(observer)
                activity.startActivity(intent)
            }

            if (withTimeoutOrNull(HANDOFF_TIMEOUT_MS) { handedOff.await() } == null) {
                return@withContext LaunchOutcome.NOT_DELIVERED
            }
            // No timeout: on Samsung the Clock UI may stay open until the user presses back.
            if (returned.await()) LaunchOutcome.RETURNED else LaunchOutcome.ACTIVITY_GONE
        } catch (e: LifecycleDestroyedException) {
            LaunchOutcome.ACTIVITY_GONE
        } finally {
            lifecycle.removeObserver(observer)
        }
    }
}
