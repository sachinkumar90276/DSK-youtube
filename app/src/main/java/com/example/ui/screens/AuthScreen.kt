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
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.ProfileEntity
import com.example.data.local.entity.UserEntity
import com.example.ui.components.*
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
        LiquidGlassTopBar(
            title = when (mode) {
                AuthMode.SIGN_IN -> "Sign In"
                AuthMode.REGISTER -> "Create Free Account"
                AuthMode.FORGOT_PASSWORD -> "Reset Password"
                AuthMode.EDIT_PROFILE -> "Edit Profile & Channel"
            },
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (mode != AuthMode.EDIT_PROFILE) {
                LiquidGlassCard(
                    shape = RoundedCornerShape(24.dp),
                    intensity = GlassIntensity.REGULAR,
                    accentTint = DskRed,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DskBrandLogo()
                        Text(
                            "Watch Everything. Pay Nothing. Always free creator & viewer accounts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            when (mode) {
                AuthMode.SIGN_IN -> {
                    var email by remember { mutableStateOf("") }
                    var password by remember { mutableStateOf("") }

                    LiquidGlassCard(
                        shape = RoundedCornerShape(24.dp),
                        intensity = GlassIntensity.REGULAR,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            LiquidGlassTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = "Email",
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_email_input")
                            )
                            LiquidGlassTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = "Password",
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_password_input")
                            )
                            LiquidGlassButton(
                                onClick = { onLogin(email, password) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_submit_button")
                            ) {
                                Text("Sign In", fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TextButton(onClick = { mode = AuthMode.FORGOT_PASSWORD }) {
                                    Text("Forgot password?")
                                }
                                TextButton(onClick = { mode = AuthMode.REGISTER }) {
                                    Text("Create account", color = DskRed, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                AuthMode.REGISTER -> {
                    var username by remember { mutableStateOf("") }
                    var email by remember { mutableStateOf("") }
                    var password by remember { mutableStateOf("") }

                    LiquidGlassCard(
                        shape = RoundedCornerShape(24.dp),
                        intensity = GlassIntensity.REGULAR,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            LiquidGlassTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = "Channel / Username",
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_username_input")
                            )
                            LiquidGlassTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = "Email",
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_email_input")
                            )
                            LiquidGlassTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = "Password (min 6 chars)",
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_password_input")
                            )
                            LiquidGlassButton(
                                onClick = { onRegister(email, username, password) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_submit_button")
                            ) {
                                Text("Create Free Account & Channel", fontWeight = FontWeight.Bold)
                            }
                            TextButton(onClick = { mode = AuthMode.SIGN_IN }) {
                                Text("Already have an account? Sign In")
                            }
                        }
                    }
                }

                AuthMode.FORGOT_PASSWORD -> {
                    var email by remember { mutableStateOf("") }
                    var newPassword by remember { mutableStateOf("") }

                    LiquidGlassCard(
                        shape = RoundedCornerShape(24.dp),
                        intensity = GlassIntensity.REGULAR,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            LiquidGlassTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = "Account Email",
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            LiquidGlassTextField(
                                value = newPassword,
                                onValueChange = { newPassword = it },
                                label = "New Password (min 6 chars)",
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            LiquidGlassButton(
                                onClick = { onResetPassword(email, newPassword) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Reset Password", fontWeight = FontWeight.Bold)
                            }
                            TextButton(onClick = { mode = AuthMode.SIGN_IN }) {
                                Text("Back to Sign In")
                            }
                        }
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

                    LiquidGlassCard(
                        shape = RoundedCornerShape(24.dp),
                        intensity = GlassIntensity.REGULAR,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                ChannelAvatar(
                                    name = displayName.ifBlank { "U" },
                                    avatarUri = pickedAvatarUri?.toString() ?: currentProfile?.avatarUri,
                                    size = 64.dp
                                )
                                LiquidGlassButton(
                                    onClick = {
                                        avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    },
                                    isPrimary = false
                                ) {
                                    Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Upload Profile Picture")
                                }
                            }

                            LiquidGlassTextField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                label = "Display / Channel Name",
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            LiquidGlassTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = "Username",
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            LiquidGlassTextField(
                                value = bio,
                                onValueChange = { bio = it },
                                label = "Channel Bio",
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                            LiquidGlassButton(
                                onClick = { onUpdateProfile(displayName, username, bio, pickedAvatarUri) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Save Profile & Channel", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    LiquidGlassCard(
                        shape = RoundedCornerShape(24.dp),
                        intensity = GlassIntensity.SUBTLE,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Change Password", style = MaterialTheme.typography.titleMedium)
                            LiquidGlassTextField(
                                value = currentPass,
                                onValueChange = { currentPass = it },
                                label = "Current Password",
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            LiquidGlassTextField(
                                value = newPass,
                                onValueChange = { newPass = it },
                                label = "New Password",
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            LiquidGlassButton(
                                onClick = {
                                    onChangePassword(currentPass, newPass)
                                    currentPass = ""
                                    newPass = ""
                                },
                                isPrimary = false,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Update Password")
                            }
                        }
                    }

                    LiquidGlassCard(
                        shape = RoundedCornerShape(18.dp),
                        intensity = GlassIntensity.SUBTLE,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(currentUser?.email ?: "", style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = onLogout) {
                                Text("Logout", color = DskRed, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
