package com.example.asistente

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.asistente.ui.theme.AsistenteTheme
import com.example.asistente.viewmodel.MainViewModel
import java.io.File

data class MainUiState(
    val isModelLoaded: Boolean = false,
    val isRecording: Boolean = false,
    val audioLevel: Float = 0f,
    val currentTranscript: String = "",
    val isProcessing: Boolean = false,
    val modelInfo: String = "",
    val savedFiles: List<File> = emptyList(),
    val hasAudioPermission: Boolean = false,
    val errorMessage: String? = null
)

class MainActivity : ComponentActivity() {
    
    private val viewModel: MainViewModel by viewModels()
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.updatePermissionStatus()
        if (isGranted) {
            viewModel.initializeModel()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Solicitar permisos si es necesario
        checkAndRequestPermissions()
        
        setContent {
            AsistenteTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }
    
    private fun checkAndRequestPermissions() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                viewModel.updatePermissionStatus()
                viewModel.initializeModel()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        if (!uiState.isModelLoaded && uiState.hasAudioPermission) {
            viewModel.initializeModel()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Asistente IA Local",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Powered by Gemma 3n",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Estado del sistema
        SystemStatusCard(uiState = uiState)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Controles principales
        ControlsCard(
            uiState = uiState,
            onStartRecording = { viewModel.startRecording() },
            onStopRecording = { viewModel.stopRecording() },
            onInitializeModel = { viewModel.initializeModel() }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Transcripción en vivo
        if (uiState.currentTranscript.isNotBlank()) {
            TranscriptionCard(
                transcript = uiState.currentTranscript,
                isProcessing = uiState.isProcessing,
                onGenerateSummary = { viewModel.generateSummary() }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Lista de archivos guardados
        SavedFilesCard(
            files = uiState.savedFiles,
            onFileClick = { file -> 
                // Aquí podrías abrir un diálogo para mostrar el contenido
            },
            onDeleteFile = { file -> viewModel.deleteFile(file) }
        )
        
        // Mensaje de error
        uiState.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.clearError() }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SystemStatusCard(uiState: MainUiState) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Estado del Sistema",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            StatusRow(
                label = "Permisos de Audio",
                isActive = uiState.hasAudioPermission,
                icon = if (uiState.hasAudioPermission) Icons.Default.VolumeUp else Icons.Default.VolumeOff
            )
            
            StatusRow(
                label = "Modelo Gemma 3n",
                isActive = uiState.isModelLoaded,
                icon = if (uiState.isModelLoaded) Icons.Default.SmartToy else Icons.Default.CloudOff
            )
            
            StatusRow(
                label = "Grabación Activa",
                isActive = uiState.isRecording,
                icon = if (uiState.isRecording) Icons.Default.RadioButtonChecked else Icons.Default.Stop
            )
            
            // Nivel de audio visual
            if (uiState.isRecording) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Nivel:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    LinearProgressIndicator(
                        progress = uiState.audioLevel.coerceIn(0f, 1f),
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp),
                        color = when {
                            uiState.audioLevel > 0.7f -> Color.Red
                            uiState.audioLevel > 0.3f -> Color.Yellow
                            else -> Color.Green
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StatusRow(
    label: String,
    isActive: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) Color.Green else Color.Gray,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isActive) Color.Green else Color.Gray,
            modifier = Modifier.size(16.dp)
        )
    }
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
fun ControlsCard(
    uiState: MainUiState,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onInitializeModel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Controles",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Botón de inicializar modelo
                ElevatedButton(
                    onClick = onInitializeModel,
                    enabled = uiState.hasAudioPermission && !uiState.isRecording,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.SmartToy, contentDescription = null)
                        Text("Cargar\nModelo", textAlign = TextAlign.Center)
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Botón de grabación
                if (!uiState.isRecording) {
                    Button(
                        onClick = onStartRecording,
                        enabled = uiState.isModelLoaded && uiState.hasAudioPermission,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Text("Iniciar\nGrabación", textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    Button(
                        onClick = onStopRecording,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Text("Detener\nGrabación", textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TranscriptionCard(
    transcript: String,
    isProcessing: Boolean,
    onGenerateSummary: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transcripción en Vivo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                }
                
                IconButton(onClick = onGenerateSummary) {
                    Icon(Icons.Default.Assignment, contentDescription = "Generar resumen")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                LazyColumn(
                    modifier = Modifier.padding(12.dp)
                ) {
                    item {
                        Text(
                            text = transcript.ifBlank { "Esperando transcripción..." },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SavedFilesCard(
    files: List<File>,
    onFileClick: (File) -> Unit,
    onDeleteFile: (File) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Archivos Guardados (${files.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (files.isEmpty()) {
                Text(
                    text = "No hay archivos guardados",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.height(200.dp)
                ) {
                    items(files.take(10)) { file ->
                        FileItem(
                            file = file,
                            onClick = { onFileClick(file) },
                            onDelete = { onDeleteFile(file) }
                        )
                        if (file != files.last()) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FileItem(
    file: File,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when {
                file.name.startsWith("transcript_") -> Icons.Default.Description
                file.name.startsWith("summary_") -> Icons.Default.Assignment
                else -> Icons.Default.InsertDriveFile
            },
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
            Text(
                text = "${file.length() / 1024} KB",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Eliminar",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}