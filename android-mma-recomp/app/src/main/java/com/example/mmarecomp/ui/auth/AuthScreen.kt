package com.example.mmarecomp.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import com.example.mmarecomp.viewmodel.AuthViewModel

@Composable
fun AuthScreen(viewModel: AuthViewModel) {
    val emailFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { emailFocusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Recomp & MMA", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Suivi entraînement, nutrition et poids — sans stress.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))

        val emailTouched = viewModel.email.isNotBlank()
        val emailValide = android.util.Patterns.EMAIL_ADDRESS.matcher(viewModel.email).matches()
        OutlinedTextField(
            value = viewModel.email,
            onValueChange = { viewModel.email = it },
            label = { Text("Email") },
            isError = emailTouched && !emailValide,
            supportingText = if (emailTouched && !emailValide) {
                { Text("Format d'email invalide") }
            } else null,
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = androidx.compose.ui.text.input.ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth().focusRequester(emailFocusRequester),
        )
        Spacer(Modifier.height(12.dp))
        var passwordVisible by remember { mutableStateOf(false) }
        val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
        OutlinedTextField(
            value = viewModel.password,
            onValueChange = { viewModel.password = it },
            label = { Text("Mot de passe") },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (passwordVisible) "Masquer le mot de passe" else "Afficher le mot de passe",
                    )
                }
            },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = androidx.compose.ui.text.input.ImeAction.Done,
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onDone = { focusManager.clearFocus(); viewModel.signIn() },
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        viewModel.errorMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        val formValide = emailValide && viewModel.password.isNotBlank()

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { viewModel.signIn() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !viewModel.isSubmitting && formValide,
        ) {
            if (viewModel.isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Se connecter")
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { viewModel.signUp() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !viewModel.isSubmitting && formValide,
        ) {
            Text("Créer un compte")
        }
    }
}
