package com.example.couchbaseassignment

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.couchbaseassignment.manager.DatabaseManager
import com.example.couchbaseassignment.model.Note
import com.example.couchbaseassignment.ui.theme.CouchbaseAssignmentTheme
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlin.random.Random

class NoteEditorActivity : ComponentActivity() {
    private lateinit var databaseManager: DatabaseManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        databaseManager = DatabaseManager.getInstance(this)
        
        val noteId = intent.getIntExtra("note_id", -1)
        val noteTitle = intent.getStringExtra("note_title") ?: ""
        val noteContent = intent.getStringExtra("note_content") ?: ""
        val noteCreatedAt = intent.getLongExtra("note_created_at", 0L)
        
        val existingNote = if (noteId != -1) {
            Note(
                id = noteId,
                title = noteTitle,
                content = noteContent,
                createdAt = Instant.fromEpochSeconds(noteCreatedAt)
            )
        } else null
        
        setContent {
            CouchbaseAssignmentTheme {
                NoteEditorScreen(
                    existingNote = existingNote,
                    onSave = { title, content ->
                        saveNote(existingNote, title, content)
                    },
                    onCancel = { finish() }
                )
            }
        }
    }
    
    private fun saveNote(existingNote: Note?, title: String, content: String) {
        lifecycleScope.launch {
            val noteId = existingNote?.id ?: Random.nextInt(1, Int.MAX_VALUE)
            val note = Note(
                id = noteId,
                title = title,
                content = content,
                createdAt = existingNote?.createdAt ?: Instant.fromEpochMilliseconds(System.currentTimeMillis())
            )
            
            if (existingNote != null) {
                databaseManager.updateExistingElement(note)
            } else {
                databaseManager.addNewElement(note)
            }
            
            finish()
        }
    }
}

@Composable
fun NoteEditorScreen(
    existingNote: Note?,
    onSave: (String, String) -> Unit,
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf(existingNote?.title ?: "") }
    var content by remember { mutableStateOf(existingNote?.content ?: "") }
    val titleFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = remember { FocusRequester() }
    
    val isFormValid = title.isNotBlank() && content.isNotBlank()
    
    LaunchedEffect(Unit) {
        if (existingNote == null) {
            titleFocusRequester.requestFocus()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFADFCA),
                        Color(0xFFF3D1BA),
                        Color(0xFFEBC4AB)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .padding(top = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.7f),
                        contentColor = Color(0xFF996666)
                    ),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Cancel",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Text(
                    text = if (existingNote == null) "New Note" else "Edit Note",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF66331A)
                )
                
                Button(
                    onClick = { onSave(title, content) },
                    enabled = isFormValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFormValid) Color(0xFF66CC66) else Color.Gray,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Save",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Save",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Title",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF4D7F4D)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        BasicTextField(
                            value = title,
                            onValueChange = { title = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(titleFocusRequester)
                                .background(
                                    Color.White.copy(alpha = 0.9f),
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    2.dp,
                                    Color(0xFF66CC66).copy(alpha = 0.3f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(16.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF333333)
                            ),
                            cursorBrush = SolidColor(Color(0xFF66CC66)),
                            decorationBox = { innerTextField ->
                                if (title.isEmpty()) {
                                    Text(
                                        text = "Enter your note title...",
                                        fontSize = 20.sp,
                                        color = Color.Gray.copy(alpha = 0.6f)
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Content",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF4D7F4D)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        BasicTextField(
                            value = content,
                            onValueChange = { content = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .focusRequester(contentFocusRequester)
                                .background(
                                    Color.White.copy(alpha = 0.9f),
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    2.dp,
                                    Color(0xFF66CC66).copy(alpha = 0.3f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 16.sp,
                                color = Color(0xFF333333),
                                lineHeight = 24.sp
                            ),
                            cursorBrush = SolidColor(Color(0xFF66CC66)),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    if (content.isEmpty()) {
                                        Text(
                                            text = "Write your thoughts here...",
                                            fontSize = 16.sp,
                                            color = Color.Gray.copy(alpha = 0.6f),
                                            modifier = Modifier.padding(4.dp)
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
} 