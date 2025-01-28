package com.example.projectcm.ui.auth

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projectcm.database.entities.User
import com.example.projectcm.database.repositories.UserRepository
import com.example.projectcm.utils.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(private val userRepository: UserRepository) : ViewModel() {
    private val _loginResult = MutableStateFlow<Result<User?>>(Result.Start)
    val loginResult: StateFlow<Result<User?>> = _loginResult

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _loginResult.value = Result.Loading
            try {
                val user = userRepository.getUser(username, password)
                if (user != null) {
                    _loginResult.value = Result.Success(user)
                } else {
                    _loginResult.value = Result.Error("Invalid credentials")
                }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Login failed", e)
                _loginResult.value = Result.Error("Login failed")
            }
        }
    }

    fun logout() {
        _loginResult.value = Result.Start
    }
}

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: (User) -> Unit,
    onRegisterClick: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val loginResult by viewModel.loginResult.collectAsState()

    Log.d("LoginScreen", "Recomposed with loginResult: $loginResult")

    LaunchedEffect(loginResult) {
        if (loginResult is Result.Success) {
            (loginResult as Result.Success<User?>).data?.let { user ->
                onLoginSuccess(user)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp) 
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally, 
            verticalArrangement = Arrangement.spacedBy(16.dp) 
        ) {
            
            Text(
                text = "Login",
                fontSize = 24.sp,
                modifier = Modifier.padding(bottom = 32.dp) 
            )

            
            Spacer(modifier = Modifier.height(16.dp))

            
            TextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth()
            )

            
            Spacer(modifier = Modifier.height(16.dp))

            
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            
            Spacer(modifier = Modifier.height(8.dp))

            
            when (val result = loginResult) {
                is Result.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally)) 
                is Result.Success -> null
                is Result.Error -> Text(result.message, color = MaterialTheme.colorScheme.error)
                Result.Start -> null
            }

            
            Spacer(modifier = Modifier.height(16.dp))

            
            Button(
                onClick = { viewModel.login(username, password) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Login")
            }

            
            Spacer(modifier = Modifier.height(16.dp))

            
            Button(
                onClick = { onRegisterClick() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Register")
            }
        }
    }
}
