package com.example.mmarecomp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.mmarecomp.data.AuthRepository
import com.example.mmarecomp.model.Phase
import com.example.mmarecomp.ui.auth.AuthScreen
import com.example.mmarecomp.ui.nav.MainNav
import com.example.mmarecomp.viewmodel.AuthViewModel
import io.github.jan.supabase.auth.status.SessionStatus

@Composable
fun RootScreen() {
    val authRepository = remember { AuthRepository() }
    val authViewModel = remember { AuthViewModel(authRepository) }
    var currentPhase by remember { mutableStateOf(Phase.Ete) }

    val sessionStatus by authRepository.sessionStatus.collectAsState()

    when (val status = sessionStatus) {
        is SessionStatus.Authenticated -> {
            MainNav(
                userId = status.session.user?.id.orEmpty(),
                authRepository = authRepository,
                currentPhase = currentPhase,
                onPhaseChange = { currentPhase = it },
            )
        }
        is SessionStatus.NotAuthenticated -> AuthScreen(authViewModel)
        else -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}
