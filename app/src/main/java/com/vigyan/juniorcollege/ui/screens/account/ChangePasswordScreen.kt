package com.vigyan.juniorcollege.ui.screens.account

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.vigyan.juniorcollege.VigyanApp
import com.vigyan.juniorcollege.data.repository.AuthRepository
import com.vigyan.juniorcollege.util.simpleFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ChangePasswordUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

class ChangePasswordViewModel(
    private val authRepository: AuthRepository,
    private val userId: Long?
) : ViewModel() {

    private val _state = MutableStateFlow(ChangePasswordUiState())
    val state: StateFlow<ChangePasswordUiState> = _state

    fun onCurrentChange(v: String) { _state.value = _state.value.copy(currentPassword = v, error = null) }
    fun onNewChange(v: String) { _state.value = _state.value.copy(newPassword = v, error = null) }
    fun onConfirmChange(v: String) { _state.value = _state.value.copy(confirmPassword = v, error = null) }

    fun submit() {
        val s = _state.value
        val id = userId
        if (id == null) { _state.value = s.copy(error = "No active session"); return }
        if (s.currentPassword.isBlank() || s.newPassword.isBlank()) {
            _state.value = s.copy(error = "Please fill in all fields"); return
        }
        if (s.newPassword.length < 4) {
            _state.value = s.copy(error = "New password must be at least 4 characters"); return
        }
        if (s.newPassword != s.confirmPassword) {
            _state.value = s.copy(error = "New password and confirmation do not match"); return
        }

        viewModelScope.launch {
            _state.value = s.copy(isSaving = true, error = null)
            val user = authRepository.getUserById(id)
            if (user == null) {
                _state.value = _state.value.copy(isSaving = false, error = "User not found")
                return@launch
            }
            val ok = authRepository.changePassword(user, s.currentPassword, s.newPassword)
            _state.value = if (ok) {
                _state.value.copy(isSaving = false, success = true)
            } else {
                _state.value.copy(isSaving = false, error = "Current password is incorrect")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(navController: NavController, userId: Long?) {
    val context = LocalContext.current
    val app = context.applicationContext as VigyanApp
    val viewModel: ChangePasswordViewModel = viewModel(
        factory = simpleFactory { ChangePasswordViewModel(app.authRepository, userId) }
    )
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.success) {
        if (state.success) {
            snackbarHostState.showSnackbar("Password changed successfully")
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Change Password") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text("Update your account password", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = state.currentPassword,
                onValueChange = viewModel::onCurrentChange,
                label = { Text("Current Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.newPassword,
                onValueChange = viewModel::onNewChange,
                label = { Text("New Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.confirmPassword,
                onValueChange = viewModel::onConfirmChange,
                label = { Text("Confirm New Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            if (state.error != null) {
                Spacer(Modifier.height(12.dp))
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = viewModel::submit,
                enabled = !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (state.isSaving) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                else Text("Update Password")
            }
        }
    }
}
