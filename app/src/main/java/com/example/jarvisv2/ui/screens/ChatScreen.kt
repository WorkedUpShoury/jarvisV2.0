package com.example.jarvisv2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvisv2.ui.components.ChatBubble
import com.example.jarvisv2.ui.components.CommandInputBar
import com.example.jarvisv2.ui.theme.DarkError
import com.example.jarvisv2.ui.theme.DarkOnSurface
import com.example.jarvisv2.ui.theme.DarkSurface
import com.example.jarvisv2.viewmodel.ChatSender
import com.example.jarvisv2.viewmodel.MainViewModel

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap

@Composable
fun ImagePreview(uri: Uri, onRemove: () -> Unit) {
    val context = LocalContext.current
    val bitmap = remember(uri) {
        try {
            val stream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(stream).asImageBitmap()
        } catch (e: Exception) { null }
    }

    if (bitmap != null) {
        Box(modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)) {
            Image(
                bitmap = bitmap,
                contentDescription = "Preview",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(20.dp)
                    .background(Color.Black.copy(alpha=0.6f), CircleShape)
            ) {
                Icon(Icons.Default.Close, "Remove", tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
fun ChatScreen(viewModel: MainViewModel) {
    val commandText by viewModel.commandText.collectAsState()
    val chatHistory by viewModel.chatHistory.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val selectedImage by viewModel.selectedImageUri.collectAsState()

    val listState = rememberLazyListState()

    // State for Dialogs and Menus
    var showClearAllDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    // --- Image Pickers Setup ---
    val context = LocalContext.current
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        viewModel.onImageSelected(uri)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoUri != null) {
            viewModel.onImageSelected(tempPhotoUri)
        }
    }

    fun launchCamera() {
        try {
            val file = File.createTempFile("camera_img_", ".jpg", context.cacheDir)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            tempPhotoUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    var showAttachDialog by remember { mutableStateOf(false) }

    if (showAttachDialog) {
        AlertDialog(
            onDismissRequest = { showAttachDialog = false },
            title = { Text("Attach Image") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    TextButton(onClick = {
                        showAttachDialog = false
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Text("Choose from Gallery", color = DarkOnSurface, fontSize = 16.sp)
                    }

                    TextButton(onClick = {
                        showAttachDialog = false
                        launchCamera()
                    }) {
                        Text("Take Photo", color = DarkOnSurface, fontSize = 16.sp)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAttachDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = DarkSurface
        )
    }

    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("Clear Chat?") },
            text = { Text("This will delete all message history from the server's history file. Your app will then resync.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.sendButtonCommand("clear chat history")
                    showClearAllDialog = false
                }) {
                    Text("Clear All", color = DarkError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Cancel", color = DarkOnSurface)
                }
            },
            containerColor = DarkSurface,
            titleContentColor = DarkOnSurface,
            textContentColor = DarkOnSurface
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Chat History",
                color = Color.White,
                fontSize = 18.sp
            )

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color.White
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Clear History", color = DarkError) },
                        onClick = {
                            showMenu = false
                            showClearAllDialog = true
                        }
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(chatHistory) { chat ->
                ChatBubble(
                    chat = chat,
                    onDelete = { viewModel.sendChatDeleteCommand(chat.message) },
                    onRepeat = if (chat.sender == ChatSender.User) {
                        { viewModel.sendButtonCommand(chat.message) }
                    } else null
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (selectedImage != null) {
            ImagePreview(uri = selectedImage!!) {
                viewModel.onImageSelected(null)
            }
        }

        CommandInputBar(
            text = commandText,
            onTextChanged = viewModel::onCommandTextChanged,
            // --- ENABLE CONVERSATION MODE HERE ---
            onSend = { viewModel.sendCurrentCommand(isConversation = true) },
            suggestions = suggestions,
            onSuggestionClick = {
                viewModel.onCommandTextChanged(it)
            },
            onAttachClick = { showAttachDialog = true }
        )
    }
}