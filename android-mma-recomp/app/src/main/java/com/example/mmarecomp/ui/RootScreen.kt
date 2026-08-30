package com.example.mmarecomp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mmarecomp.data.AuthRepository
import com.example.mmarecomp.model.Phase
import com.example.mmarecomp.ui.auth.AuthScreen
import com.example.mmarecomp.ui.nav.MainNav
import com.example.mmarecomp.viewmodel.AuthViewModel
import com.example.mmarecomp.viewmodel.PhaseState
import com.example.mmarecomp.viewmodel.SessionProfileViewModel
import io.github.jan.supabase.auth.status.SessionStatus

@Composable
fun RootScreen() {
    val authRepository = remember { AuthRepository() }
    val authViewModel = remember { AuthViewModel(authRepository) }

    val sessionStatus by authRepository.sessionStatus.collectAsState()

    when (val status = sessionStatus) {
        is SessionStatus.Authenticated -> {
            val userId = status.session.user?.id.orEmpty()
            val sessionProfileViewModel = remember(userId) { SessionProfileViewModel(userId) }
            val phaseState = sessionProfileViewModel.phaseState

            LaunchedEffect(userId) {
                sessionProfileViewModel.load()
            }

            when (val state = phaseState) {
                is PhaseState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is PhaseState.Ready -> {
                    var currentPhase by remember(state.phase) { mutableStateOf(state.phase) }
                    LaunchedEffect(state.phase) { currentPhase = state.phase }
                    MainNav(
                        userId = userId,
                        userEmail = status.session.user?.email.orEmpty(),
                        authRepository = authRepository,
                        currentPhase = currentPhase,
                        onPhaseChange = { currentPhase = it },
                    )
                }
                is PhaseState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(horizontal = 24.dp),
                            )
                            Button(
                                onClick = { sessionProfileViewModel.load() },
                                modifier = Modifier.padding(top = 16.dp),
                            ) {
                                Text("Réessayer")
                            }
                        }
                    }
                }
            }
        }
        is SessionStatus.NotAuthenticated -> AuthScreen(authViewModel)
        else -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}
