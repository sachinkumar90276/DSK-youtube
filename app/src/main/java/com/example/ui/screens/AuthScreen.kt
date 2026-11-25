package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.ProfileEntity
import com.example.data.local.entity.UserEntity
import com.example.ui.components.ChannelAvatar
import com.example.ui.components.DskBrandLogo
import com.example.ui.theme.DskRed

enum class AuthMode {
    SIGN_IN, REGISTER, FORGOT_PASSWORD, EDIT_PROFILE
}

@Composable
fun AuthScreen(
    initialMode: AuthMode,
    currentUser: UserEntity?,
    currentProfile: ProfileEntity?,
    onBack: () -> Unit,
    onLogin: (email: String, password: String) -> Unit,
    onRegister: (email: String, username: String, password: String) -> Unit,
    onResetPassword: (email: String, newPassword: String) -> Unit,
    onUpdateProfile: (displayName: String, username: String, bio: String, avatarUri: Uri?) -> Unit,
    onChangePassword: (currentPass: String, newPass: String) -> Unit,
    onLogout: () -> Unit
) {
    var mode by remember(initialMode, currentUser) {
        mutableStateOf(if (currentUser != null) AuthMode.EDIT_PROFILE else initialMode)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = when (mode) {
                    AuthMode.SIGN_IN -> "Sign In"
                    AuthMode.REGISTER -> "Create Free Account"
                    AuthMode.FORGOT_PASSWORD -> "Reset Password"
                    AuthMode.EDIT_PROFILE -> "Edit Profile & Channel"
                },
                style = MaterialTheme.typography.titleLarge
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (mode != AuthMode.EDIT_PROFILE) {
                DskBrandLogo()
                Text(
                    "Watch Everything. Pay Nothing. Always free creator & viewer accounts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            when (mode) {
                AuthMode.SIGN_IN -> {
                    var email by remember { mutableStateOf("") }
                    var password by remember { mutableStateOf("") }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_email_input")
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_password_input")
                    )
                    Button(
                        onClick = { onLogin(email, password) },
                        colors = ButtonDefaults.buttonColors(containerColor = DskRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_submit_button")
                    ) {
                        Text("Sign In")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { mode = AuthMode.FORGOT_PASSWORD }) {
                            Text("Forgot password?")
                        }
                        TextButton(onClick = { mode = AuthMode.REGISTER }) {
                            Text("Create account", color = DskRed)
                        }
                    }
                }

                AuthMode.REGISTER -> {
                    var username by remember { mutableStateOf("") }
                    var email by remember { mutableStateOf("") }
                    var password by remember { mutableStateOf("") }

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Channel / Username") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_username_input")
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_email_input")
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password (min 6 chars)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_password_input")
                    )
                    Button(
                        onClick = { onRegister(email, username, password) },
                        colors = ButtonDefaults.buttonColors(containerColor = DskRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("register_submit_button")
                    ) {
                        Text("Create Free Account & Channel")
                    }
                    TextButton(onClick = { mode = AuthMode.SIGN_IN }) {
                        Text("Already have an account? Sign In")
                    }
                }

                AuthMode.FORGOT_PASSWORD -> {
                    var email by remember { mutableStateOf("") }
                    var newPassword by remember { mutableStateOf("") }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Account Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password (min 6 chars)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { onResetPassword(email, newPassword) },
                        colors = ButtonDefaults.buttonColors(containerColor = DskRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Reset Password")
                    }
                    TextButton(onClick = { mode = AuthMode.SIGN_IN }) {
                        Text("Back to Sign In")
                    }
                }

                AuthMode.EDIT_PROFILE -> {
                    var displayName by remember(currentProfile) { mutableStateOf(currentProfile?.displayName ?: "") }
                    var username by remember(currentProfile) { mutableStateOf(currentProfile?.username ?: "") }
                    var bio by remember(currentProfile) { mutableStateOf(currentProfile?.bio ?: "") }
                    var pickedAvatarUri by remember { mutableStateOf<Uri?>(null) }

                    var currentPass by remember { mutableStateOf("") }
                    var newPass by remember { mutableStateOf("") }

                    val avatarPicker = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.PickVisualMedia()
                    ) { uri -> if (uri != null) pickedAvatarUri = uri }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        ChannelAvatar(
                            name = displayName.ifBlank { "U" },
                            avatarUri = pickedAvatarUri?.toString() ?: currentProfile?.avatarUri,
                            size = 64.dp
                        )
                        OutlinedButton(onClick = {
                            avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) {
                            Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Upload Profile Picture")
                        }
                    }

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Display / Channel Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("Channel Bio") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { onUpdateProfile(displayName, username, bio, pickedAvatarUri) },
                        colors = ButtonDefaults.buttonColors(containerColor = DskRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Profile & Channel")
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Change Password", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = currentPass,
                        onValueChange = { currentPass = it },
                        label = { Text("Current Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = { Text("New Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = {
                            onChangePassword(currentPass, newPass)
                            currentPass = ""
                            newPass = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Update Password")
                    }

                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(currentUser?.email ?: "", style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = onLogout) {
                                Text("Logout", color = DskRed)
                            }
                        }
                    }
                }
            }
        }
    }
}
