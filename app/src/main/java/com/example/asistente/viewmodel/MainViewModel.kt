package com.example.asistente.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.asistente.audio.AudioManager
import com.example.asistente.ml.GemmaManager
import com.example.asistente.services.AudioRecordingService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * ViewModel principal para la aplicacion del asistente
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val context: Context = application.applicationContext
    private val gemmaManager = GemmaManager(context)
    private val audioManager = AudioManager(context)
    
    private val _uiState = MutableStateFlow(com.example.asistente.MainUiState())
    val uiState: StateFlow<com.example.asistente.MainUiState> = _uiState.asStateFlow()
    
    companion object {
        private const val TAG = "MainViewModel"
    }
    
    init {
        observeComponents()
        checkInitialState()
    }
    
    private fun observeComponents() {
        viewModelScope.launch {
            // Observar estado del modelo Gemma incluyendo descarga
            combine(
                gemmaManager.isModelLoaded,
                gemmaManager.transcriptionResult,
                gemmaManager.isProcessing,
                gemmaManager.isDownloading,
                gemmaManager.downloadProgress,
                gemmaManager.downloadStatus,
                audioManager.isRecording,
                audioManager.audioLevel
            ) { isModelLoaded, transcription, isProcessing, isDownloading, downloadProgress, downloadStatus, isRecording, audioLevel ->
                _uiState.value = _uiState.value.copy(
                    isModelLoaded = isModelLoaded,
                    currentTranscript = transcription,
                    isDownloading = isDownloading,
                    downloadProgress = downloadProgress,
                    downloadStatus = downloadStatus,
                    isProcessing = isProcessing,
                    isRecording = isRecording,
                    audioLevel = audioLevel
                )
            }.collect()
        }
    }
    
    private fun checkInitialState() {
        _uiState.value = _uiState.value.copy(
            hasAudioPermission = audioManager.hasAudioPermission(),
            modelInfo = gemmaManager.getModelInfo()
        )
        loadSavedFiles()
    }
    
    /**
     * Inicializa el modelo Gemma 3n
     */
    fun initializeModel() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(errorMessage = null)
                
                if (!gemmaManager.isModelFileAvailable()) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Archivo del modelo no encontrado. Descarga el modelo Gemma 3n E4B."
                    )
                    return@launch
                }
                
                Log.d(TAG, "Inicializando modelo Gemma 3n...")
                val success = gemmaManager.initializeModel()
                
                if (success) {
                    Log.i(TAG, "Modelo inicializado correctamente")
                    _uiState.value = _uiState.value.copy(
                        modelInfo = gemmaManager.getModelInfo()
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Error al cargar el modelo Gemma 3n"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error durante la inicializacion", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error inesperado: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Inicia la grabacion continua usando el servicio
     */
    fun startRecording() {
        if (!audioManager.hasAudioPermission()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Se requieren permisos de audio"
            )
            return
        }
        
        if (!_uiState.value.isModelLoaded) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "El modelo debe estar cargado antes de iniciar la grabacion"
            )
            return
        }
        
        try {
            AudioRecordingService.startRecording(context)
            Log.i(TAG, "Servicio de grabacion iniciado")
        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar grabacion", e)
            _uiState.value = _uiState.value.copy(
                errorMessage = "Error al iniciar grabacion: ${e.message}"
            )
        }
    }
    
    /**
     * Detiene la grabacion continua
     */
    fun stopRecording() {
        try {
            AudioRecordingService.stopRecording(context)
            Log.i(TAG, "Servicio de grabacion detenido")
            loadSavedFiles() // Recargar archivos guardados
        } catch (e: Exception) {
            Log.e(TAG, "Error al detener grabacion", e)
            _uiState.value = _uiState.value.copy(
                errorMessage = "Error al detener grabacion: ${e.message}"
            )
        }
    }
    
    /**
     * Procesa texto manualmente con Gemma 3n
     */
    fun processText(text: String) {
        if (!_uiState.value.isModelLoaded) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "El modelo debe estar cargado"
            )
            return
        }
        
        viewModelScope.launch {
            try {
                gemmaManager.processAudioText(text)
            } catch (e: Exception) {
                Log.e(TAG, "Error al procesar texto", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error al procesar texto: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Genera resumen del texto actual
     */
    fun generateSummary() {
        val currentText = _uiState.value.currentTranscript
        if (currentText.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "No hay texto para resumir"
            )
            return
        }
        
        viewModelScope.launch {
            try {
                val summary = gemmaManager.generateSummary(currentText)
                
                // Guardar resumen
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val file = File(context.filesDir, "summary_manual_$timestamp.txt")
                file.writeText(summary)
                
                Log.i(TAG, "Resumen generado y guardado")
                loadSavedFiles()
            } catch (e: Exception) {
                Log.e(TAG, "Error al generar resumen", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error al generar resumen: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Carga la lista de archivos guardados
     */
    private fun loadSavedFiles() {
        viewModelScope.launch {
            try {
                val files = context.filesDir.listFiles()?.filter { 
                    it.name.endsWith(".txt") 
                }?.sortedByDescending { 
                    it.lastModified() 
                } ?: emptyList()
                
                _uiState.value = _uiState.value.copy(savedFiles = files)
            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar archivos", e)
            }
        }
    }
    
    /**
     * Lee el contenido de un archivo
     */
    fun readFile(file: File): String {
        return try {
            file.readText()
        } catch (e: Exception) {
            Log.e(TAG, "Error al leer archivo", e)
            "Error al leer el archivo: ${e.message}"
        }
    }
    
    /**
     * Elimina un archivo
     */
    fun deleteFile(file: File) {
        try {
            if (file.delete()) {
                Log.d(TAG, "Archivo eliminado: ${file.name}")
                loadSavedFiles()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al eliminar archivo", e)
            _uiState.value = _uiState.value.copy(
                errorMessage = "Error al eliminar archivo: ${e.message}"
            )
        }
    }
    
    /**
     * Limpia el mensaje de error
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    /**
     * Actualiza el estado de permisos
     */
    fun updatePermissionStatus() {
        _uiState.value = _uiState.value.copy(
            hasAudioPermission = audioManager.hasAudioPermission()
        )
    }
    
    /**
     * Obtiene estadisticas de uso
     */
    fun getUsageStats(): String {
        val files = _uiState.value.savedFiles
        val totalFiles = files.size
        val totalSize = files.sumOf { it.length() }
        val transcripts = files.filter { it.name.startsWith("transcript_") }.size
        val summaries = files.filter { it.name.startsWith("summary_") }.size
        
        return """
            Estadisticas de uso:
            • Total de archivos: $totalFiles
            • Transcripciones: $transcripts
            • Resumenes: $summaries
            • Espacio total: ${totalSize / 1024} KB
            • Modelo cargado: ${if (_uiState.value.isModelLoaded) "Si" else "No"}
        """.trimIndent()
    }
    
    override fun onCleared() {
        super.onCleared()
        gemmaManager.release()
        audioManager.release()
    }
}
