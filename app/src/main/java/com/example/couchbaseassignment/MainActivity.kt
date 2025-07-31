package com.example.couchbaseassignment

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.couchbaseassignment.manager.ConfigurationManager
import com.example.couchbaseassignment.manager.DatabaseManager
import com.example.couchbaseassignment.manager.ErrorManager
import com.example.couchbaseassignment.manager.NetworkMonitor
import com.example.couchbaseassignment.model.Note
import com.example.couchbaseassignment.ui.theme.CouchbaseAssignmentTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var databaseManager: DatabaseManager
    private lateinit var networkMonitor: NetworkMonitor
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        ConfigurationManager.initialize(this)
        
        databaseManager = DatabaseManager.getInstance(this)
        
        networkMonitor = NetworkMonitor.getInstance(this)
        
        val config = ConfigurationManager.getConfiguration()
        
        setContent {
            CouchbaseAssignmentTheme {
                NotesScreen(
                    databaseManager = databaseManager,
                    networkMonitor = networkMonitor,
                    onNoteClick = { note -> openNoteEditor(note) },
                    onNewNoteClick = { openNoteEditor(null) }
                )
            }
        }
    }
    
    private fun openNoteEditor(note: Note?) {
        val intent = Intent(this, NoteEditorActivity::class.java)
        note?.let {
            intent.putExtra("note_id", it.id)
            intent.putExtra("note_title", it.title)
            intent.putExtra("note_content", it.content)
            intent.putExtra("note_created_at", it.createdAt.epochSeconds)
        }
        startActivity(intent)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        databaseManager.cleanup()
        networkMonitor.cleanup()
    }
}

@Composable
fun NotesScreen(
    databaseManager: DatabaseManager,
    networkMonitor: NetworkMonitor,
    onNoteClick: (Note) -> Unit,
    onNewNoteClick: () -> Unit
) {
    val notes by databaseManager.notesFlow.collectAsState()
    val isConnected by networkMonitor.isConnected.collectAsState()
    val errors by ErrorManager.errors.collectAsState()
    val context = LocalContext.current
    
    errors.firstOrNull()?.let { error ->
        AlertDialog(
            onDismissRequest = { ErrorManager.dismissNext() },
            title = { Text(error.title) },
            text = { Text(error.message) },
            confirmButton = {
                TextButton(onClick = { ErrorManager.dismissNext() }) {
                    Text("OK")
                }
            }
        )
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
                .padding(horizontal = 20.dp)
                .padding(top = 40.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "My Notes",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF66331A)
                    )
                    Text(
                        text = "${notes.size} ${if (notes.size == 1) "note" else "notes"}",
                        fontSize = 14.sp,
                        color = Color(0xFF99664D)
                    )
                }
                
                Button(
                    onClick = onNewNoteClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF66CC66)
                    ),
                    shape = RoundedCornerShape(25.dp),
                    modifier = Modifier.shadow(5.dp, RoundedCornerShape(25.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add note",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "New",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Spacer(modifier = Modifier.height(20.dp))
            
            if (notes.isEmpty()) {
                EmptyNotesState()
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(notes) { note ->
                        NoteCard(
                            note = note,
                            onNoteClick = { onNoteClick(note) },
                            onDeleteClick = { 
                                (context as ComponentActivity).lifecycleScope.launch {
                                    databaseManager.deleteElement(note)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyNotesState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    Color.White.copy(alpha = 0.3f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "📝",
                fontSize = 50.sp
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "No notes yet",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF66331A)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Tap the 'New' button to create\nyour first amazing note",
            fontSize = 16.sp,
            color = Color(0xFF99664D),
            lineHeight = 20.sp
        )
    }
}

@Composable
fun NoteCard(
    note: Note,
    onNoteClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = spring(),
        label = "scale"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable { 
                isPressed = false
                onNoteClick() 
            },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = note.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF336633),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = note.content,
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Date",
                        tint = Color(0xFF999999),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = note.getFormattedDate(),
                        fontSize = 12.sp,
                        color = Color(0xFF999999)
                    )
                }
            }
            
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete note",
                    tint = Color.Red.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}