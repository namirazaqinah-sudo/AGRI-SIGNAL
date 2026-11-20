package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.configureAppCheck
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.MainAppShell
import com.example.ui.screens.WelcomeScreen
import com.example.ui.theme.AgriSignalTheme
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.SessionState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Step 1: Configure Firebase App Check with launch intent before any UI or ViewModels
        configureAppCheck(this, intent)

        // Step 2: Enable edge-to-edge layout
        enableEdgeToEdge()

        // Step 3: Render Compose UI
        setContent {
            AgriSignalTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AgriSignalRoot()
                }
            }
        }
    }
}

@Composable
fun AgriSignalRoot(authViewModel: AuthViewModel = viewModel()) {
    val sessionState by authViewModel.sessionState.collectAsStateWithLifecycle()
    val formState by authViewModel.formState.collectAsStateWithLifecycle()

    when (val state = sessionState) {
        is SessionState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        is SessionState.Unauthenticated -> {
            AuthScreen(
                formState = formState,
                onModeChange = authViewModel::switchMode,
                onNameChange = authViewModel::onNameChanged,
                onPhoneChange = authViewModel::onPhoneChanged,
                onSubmit = authViewModel::submit
            )
        }

        is SessionState.Welcome -> {
            WelcomeScreen(
                farmer = state.farmer,
                onContinue = authViewModel::proceedFromWelcome
            )
        }

        is SessionState.Authenticated -> {
            key(state.farmer.phoneNumber) {
                MainAppShell(
                    farmer = state.farmer,
                    onLogout = authViewModel::logout
                )
            }
        }
    }
}
