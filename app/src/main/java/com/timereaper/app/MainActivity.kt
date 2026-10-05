package com.timereaper.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.timereaper.app.ui.screen.MainScreen
import com.timereaper.app.ui.theme.TimeReaperTheme
import com.timereaper.app.viewmodel.AlarmViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)

        setContent {
            var isDark by rememberSaveable {
                mutableStateOf(prefs.getBoolean("dark_theme", true))
            }

            TimeReaperTheme(darkTheme = isDark) {
                val vm: AlarmViewModel = viewModel()
                MainScreen(
                    viewModel = vm,
                    isDark = isDark,
                    onToggleTheme = {
                        isDark = !isDark
                        prefs.edit().putBoolean("dark_theme", isDark).apply()
                    },
                    context = this
                )
            }
        }
    }
}
