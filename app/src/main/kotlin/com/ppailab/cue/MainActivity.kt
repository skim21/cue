package com.ppailab.cue

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.ppailab.cue.home.HomeScreen
import com.ppailab.cue.onboarding.SetupScreen
import com.ppailab.cue.onboarding.SetupViewModel
import com.ppailab.cue.persona.SavedPersona
import com.ppailab.cue.reply.ReplyScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                CueApp()
            }
        }
    }
}

private sealed class Screen {
    object Home : Screen()
    data class Reply(val persona: SavedPersona) : Screen()
}

@Composable
fun CueApp() {
    val setupVm: SetupViewModel = hiltViewModel()
    val isSetupDone by setupVm.isSetupDone.collectAsState()
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }

    AnimatedContent(targetState = isSetupDone) { done ->
        if (!done) {
            SetupScreen(onDone = { setupVm.markDone() })
        } else {
            when (val s = screen) {
                is Screen.Home -> HomeScreen(
                    onPersonaTap = { screen = Screen.Reply(it) },
                    onImportTap = { /* ImportActivity launched via share — show instructions */ },
                )
                is Screen.Reply -> ReplyScreen(
                    personaId = s.persona.id,
                    onBack = { screen = Screen.Home },
                )
            }
        }
    }
}
