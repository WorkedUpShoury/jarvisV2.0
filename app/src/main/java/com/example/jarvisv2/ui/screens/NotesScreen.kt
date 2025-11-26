package com.example.jarvisv2.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.jarvisv2.ui.theme.DarkOnSurface
import com.example.jarvisv2.ui.theme.DarkPrimary
import com.example.jarvisv2.ui.theme.DarkSurface
import com.example.jarvisv2.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

// Command format for the server: create note [TITLE] --- [CONTENT]
// We use the current date as the title to align with the server's NotesDaily.md default logic.
private fun generateNoteCommand(content: String): String {
    val title = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    // This command format is understood by the server to append to NotesDaily.md
    return "create note $title --- $content"
}

@Composable
fun NotesDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var noteValue by remember { mutableStateOf(TextFieldValue("")) }
    var isSending by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = DarkSurface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- Header ---
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Append to NotesDaily.md",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close", tint = DarkOnSurface)
                    }
                }

                // *** NO TITLE FIELD ***

                // --- Formatting Toolbar ---
                FormattingToolbar(noteValue) { newText ->
                    noteValue = newText
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- Main Content Field ---
                OutlinedTextField(
                    value = noteValue,
                    onValueChange = { noteValue = it },
                    label = { Text("Note Content (Markdown supported)", color = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // --- Save Button ---
                Button(
                    onClick = {
                        isSending = true
                        val command = generateNoteCommand(noteValue.text)
                        viewModel.sendButtonCommand(command)
                        noteValue = TextFieldValue("")
                        isSending = false
                        onDismiss()
                    },
                    enabled = noteValue.text.isNotBlank() && !isSending,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkPrimary),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text(if (isSending) "Saving..." else "Append to NotesDaily.md", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// --- Formatting Toolbar Composables (Unchanged) ---
data class FormatAction(val icon: ImageVector, val name: String, val pattern: String)

private val formatActions = listOf(
    FormatAction(Icons.Default.Title, "H1", "# TEXT"),
    FormatAction(Icons.Default.FormatBold, "Bold", "**TEXT**"),
    FormatAction(Icons.Default.FormatItalic, "Italic", "*TEXT*"),
    FormatAction(Icons.Default.FormatListBulleted, "List", "\n- TEXT"),
    FormatAction(Icons.Default.Link, "Link", "[TEXT](URL)"),
    FormatAction(Icons.Default.Code, "Code Block", "\n```\nCODE\n```\n"),
    FormatAction(Icons.Default.TableChart, "Table", "\n\n| H1 | H2 |\n|---|---|\n| C1 | C2 |\n\n"),
)

@Composable
fun FormattingToolbar(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(formatActions) { action ->
            OutlinedButton(
                onClick = { applyFormat(action, value, onValueChange) },
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, DarkPrimary.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.3f),
                    contentColor = DarkPrimary
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(action.icon, contentDescription = action.name, modifier = Modifier.size(20.dp))
            }
        }
    }
}

private fun applyFormat(
    action: FormatAction,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit
) {
    val selection = value.selection
    val start = selection.min
    val end = selection.max
    val text = value.text

    val pattern = action.pattern

    val (prefix, suffix, placeholder) = when {
        pattern.contains("TEXT") -> {
            val parts = pattern.split("TEXT")
            Triple(parts.getOrElse(0) { "" }, parts.getOrElse(1) { "" }, "TEXT")
        }
        pattern.contains("CODE") -> {
            val parts = pattern.split("CODE")
            Triple(parts.getOrElse(0) { "" }, parts.getOrElse(1) { "" }, "CODE")
        }
        else -> Triple(pattern, "", "")
    }

    val newText = if (start != end) {
        // Apply format around selection
        text.substring(0, start) + prefix + text.substring(start, end) + suffix + text.substring(end)
    } else {
        // Insert format at cursor
        text.substring(0, start) + prefix + placeholder + suffix + text.substring(end)
    }

    // Calculate new cursor position
    val newCursorPosition = if (start != end) {
        // Position cursor at the end of the newly wrapped selection
        end + prefix.length + suffix.length
    } else {
        // Position cursor right after the prefix (or placeholder if non-empty)
        start + prefix.length
    }

    val finalSelection = TextRange(newCursorPosition)

    onValueChange(value.copy(text = newText, selection = finalSelection))
}