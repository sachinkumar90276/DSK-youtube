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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.CategoryEntity
import com.example.ui.components.*
import com.example.ui.theme.DskRed
import com.example.ui.theme.DskSuccess
import com.example.viewmodel.UploadUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    isSignedIn: Boolean,
    categories: List<CategoryEntity>,
    uploadState: UploadUiState,
    onRequireSignIn: () -> Unit,
    onUpload: (
        isShort: Boolean,
        videoUri: Uri?,
        directUrl: String?,
        thumbnailUri: Uri?,
        title: String,
        description: String,
        category: String,
        tags: String,
        visibility: String,
        commentsEnabled: Boolean,
        musicInfo: String
    ) -> Unit,
    onResetUploadState: () -> Unit,
    onOpenUploadedVideo: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isSignedIn) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            LiquidGlassCard(
                shape = RoundedCornerShape(26.dp),
                intensity = GlassIntensity.REGULAR,
                accentTint = DskRed,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(56.dp), tint = DskRed)
                    Text("Sign in to Publish Videos & Shorts", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Uploading is 100% free on DSK YouTube. Sign in or create a free creator account to start publishing.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LiquidGlassButton(
                        onClick = onRequireSignIn,
                        modifier = Modifier.testTag("upload_signin_button")
                    ) {
                        Text("Sign In / Register", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        return
    }

    var isShort by remember { mutableStateOf(false) }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var directVideoUrl by remember { mutableStateOf("") }
    var selectedThumbUri by remember { mutableStateOf<Uri?>(null) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember(categories) { mutableStateOf(categories.firstOrNull()?.name ?: "Technology") }
    var tags by remember { mutableStateOf("") }
    var visibility by remember { mutableStateOf("PUBLIC") }
    var commentsEnabled by remember { mutableStateOf(true) }
    var musicInfo by remember { mutableStateOf("") }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) selectedVideoUri = uri }

    val thumbPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) selectedThumbUri = uri }

    fun triggerUpload() {
        onUpload(
            isShort,
            selectedVideoUri,
            directVideoUrl.takeIf { it.isNotBlank() },
            selectedThumbUri,
            title,
            description,
            category,
            tags,
            visibility,
            commentsEnabled,
            musicInfo
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        LiquidGlassCard(
            shape = RoundedCornerShape(22.dp),
            intensity = GlassIntensity.STRONG,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Create & Upload", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Free Creator Studio • Max 100 MB per local file or paste a direct MP4/WebM stream URL.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Format Toggle
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LiquidGlassChip(
                selected = !isShort,
                onClick = { isShort = false },
                leadingIcon = Icons.Outlined.VideoLibrary,
                label = "Standard Video"
            )
            LiquidGlassChip(
                selected = isShort,
                onClick = { isShort = true },
                leadingIcon = Icons.Outlined.Bolt,
                label = "Short (9:16)"
            )
        }

        // Media Source Pickers
        LiquidGlassCard(
            shape = RoundedCornerShape(20.dp),
            intensity = GlassIntensity.REGULAR,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LiquidGlassButton(
                        onClick = {
                            videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                        },
                        isPrimary = false,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_video_button")
                    ) {
                        Icon(Icons.Outlined.VideoFile, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (selectedVideoUri != null) "Video Selected ✓" else "Pick Video")
                    }
                    LiquidGlassButton(
                        onClick = {
                            thumbPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        isPrimary = false,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_thumb_button")
                    ) {
                        Icon(Icons.Outlined.Image, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (selectedThumbUri != null) "Thumb Ready ✓" else "Thumbnail")
                    }
                }
                LiquidGlassTextField(
                    value = directVideoUrl,
                    onValueChange = { directVideoUrl = it },
                    label = "Or Direct MP4 / WebM URL",
                    placeholder = "https://.../video.mp4",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        LiquidGlassTextField(
            value = title,
            onValueChange = { title = it },
            label = "Title *",
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("upload_title_input")
        )

        LiquidGlassTextField(
            value = description,
            onValueChange = { description = it },
            label = "Description",
            minLines = 3,
            maxLines = 5,
            modifier = Modifier.fillMaxWidth()
        )

        if (isShort) {
            LiquidGlassTextField(
                value = musicInfo,
                onValueChange = { musicInfo = it },
                label = "Music / Audio Track",
                placeholder = "Original Sound - Your Channel",
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            ExposedDropdownMenuBox(
                expanded = categoryDropdownExpanded,
                onExpandedChange = { categoryDropdownExpanded = it }
            ) {
                LiquidGlassTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = "Category",
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false },
                    containerColor = glassDropdownContainerColor()
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.name) },
                            onClick = {
                                category = cat.name
                                categoryDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        LiquidGlassTextField(
            value = tags,
            onValueChange = { tags = it },
            label = "Tags (comma separated)",
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Visibility
        Text("Visibility", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("PUBLIC", "UNLISTED", "PRIVATE").forEach { vis ->
                LiquidGlassChip(
                    selected = visibility == vis,
                    onClick = { visibility = vis },
                    label = vis
                )
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
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Allow Comments", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = commentsEnabled, onCheckedChange = { commentsEnabled = it })
            }
        }

        // Upload Progress / Status Glass Card
        when (uploadState) {
            is UploadUiState.Uploading -> {
                LiquidGlassCard(
                    shape = RoundedCornerShape(18.dp),
                    intensity = GlassIntensity.REGULAR,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(uploadState.status, style = MaterialTheme.typography.bodyMedium)
                            Text("${uploadState.progress}%", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = DskRed)
                        }
                        LinearProgressIndicator(
                            progress = { uploadState.progress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = DskRed
                        )
                    }
                }
            }
            is UploadUiState.Success -> {
                LiquidGlassCard(
                    shape = RoundedCornerShape(18.dp),
                    intensity = GlassIntensity.REGULAR,
                    accentTint = DskSuccess,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Published successfully!", style = MaterialTheme.typography.titleSmall, color = DskSuccess)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            LiquidGlassButton(
                                onClick = {
                                    onOpenUploadedVideo(uploadState.contentId, uploadState.isShort)
                                    onResetUploadState()
                                }
                            ) {
                                Text("Watch Now")
                            }
                            LiquidGlassButton(
                                onClick = {
                                    selectedVideoUri = null
                                    directVideoUrl = ""
                                    selectedThumbUri = null
                                    title = ""
                                    description = ""
                                    tags = ""
                                    onResetUploadState()
                                },
                                isPrimary = false
                            ) {
                                Text("Upload Another")
                            }
                        }
                    }
                }
            }
            is UploadUiState.Error -> {
                LiquidGlassCard(
                    shape = RoundedCornerShape(18.dp),
                    intensity = GlassIntensity.REGULAR,
                    accentTint = DskRed,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(uploadState.message, style = MaterialTheme.typography.bodyMedium, color = DskRed)
                        LiquidGlassButton(onClick = { triggerUpload() }, isPrimary = false) {
                            Text("Retry Upload")
                        }
                    }
                }
            }
            UploadUiState.Idle -> Unit
        }

        LiquidGlassButton(
            onClick = { triggerUpload() },
            enabled = uploadState !is UploadUiState.Uploading,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("publish_upload_button")
        ) {
            Icon(Icons.Outlined.CloudUpload, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (isShort) "Publish Short (Free)" else "Publish Video (Free)", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(24.dp))
    }
}
