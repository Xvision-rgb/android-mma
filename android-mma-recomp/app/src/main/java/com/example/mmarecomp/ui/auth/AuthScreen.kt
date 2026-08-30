package com.example.mmarecomp.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.mmarecomp.R
import androidx.compose.runtime.remember
import com.example.mmarecomp.ui.theme.Dimens
import com.example.mmarecomp.viewmodel.AuthViewModel

@Composable
fun AuthScreen(viewModel: AuthViewModel) {
    val emailFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { emailFocusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.spaceLg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.auth_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(Dimens.spaceSm))
        Text(
            stringResource(R.string.auth_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.spaceLg))

        val emailTouched = viewModel.email.isNotBlank()
        val emailValide = android.util.Patterns.EMAIL_ADDRESS.matcher(viewModel.email).matches()
        OutlinedTextField(
            value = viewModel.email,
            onValueChange = { viewModel.email = it },
            label = { Text(stringResource(R.string.auth_email)) },
            isError = emailTouched && !emailValide,
            supportingText = if (emailTouched && !emailValide) {
                { Text(stringResource(R.string.auth_email_invalid)) }
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
        val formValide = emailValide && viewModel.password.isNotBlank()
        OutlinedTextField(
            value = viewModel.password,
            onValueChange = { viewModel.password = it },
            label = { Text(stringResource(R.string.auth_password)) },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (passwordVisible) {
                            stringResource(R.string.auth_hide_password)
                        } else {
                            stringResource(R.string.auth_show_password)
                        },
                    )
                }
            },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = androidx.compose.ui.text.input.ImeAction.Done,
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    if (formValide && !viewModel.isSubmitting) viewModel.signIn()
                },
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        viewModel.errorMessage?.let {
            Spacer(Modifier.height(Dimens.spaceSm))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        viewModel.signUpSuccessMessage?.let {
            Spacer(Modifier.height(Dimens.spaceSm))
            Text(it, color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { viewModel.signIn() },
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = Dimens.minTouchTarget),
            enabled = !viewModel.isSubmitting && formValide,
        ) {
            if (viewModel.isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(Dimens.iconSm), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(stringResource(R.string.auth_sign_in))
            }
        }
        Spacer(Modifier.height(Dimens.spaceSm))
        OutlinedButton(
            onClick = { viewModel.signUp() },
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = Dimens.minTouchTarget),
            enabled = !viewModel.isSubmitting && formValide,
        ) {
            Text(stringResource(R.string.auth_sign_up))
        }
    }
}
