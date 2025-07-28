package com.example.asistente.ml

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.net.HttpURLConnection

/**
 * Manager para manejar la inferencia con Gemma 3n
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.net.HttpURLConnection
    private val _transcriptionResult = MutableStateFlow("")
    val transcriptionResult: StateFlow<String> = _transcriptionResult.asStateFlow()
    
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()
    
    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()
    
    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()
    
    private val _downloadStatus = MutableStateFlow("")
    val downloadStatus: StateFlow<String> = _downloadStatus.asStateFlow()
    
    companion object {
        private const val TAG = "GemmaManager"
        private const val MODEL_FILE_NAME = "gemma-3n-E4B-it.task"
        private const val MODEL_DOWNLOAD_URL = "https://huggingface.co/google/gemma-3n-E4B-it/resolve/main/gemma-3n-E4B-it.task"
        
        // Configuracion del modelo segun la documentacion
        private const val MAX_TOKENS = 512
    }
    
    /**
     * Inicializa el modelo Gemma 3n
     * Si no existe, lo descarga automaticamente
     */
    suspend fun initializeModel(): Boolean = withContext(Dispatchers.IO) {
        try {
            val modelFile = File(context.filesDir, MODEL_FILE_NAME)
            
            // Si el modelo no existe, descargarlo automaticamente
            if (!modelFile.exists()) {
                Log.i(TAG, "Modelo no encontrado. Iniciando descarga automatica...")
                _downloadStatus.value = "Preparando descarga..."
                
                val downloadSuccess = downloadModel()
                if (!downloadSuccess) {
                    Log.e(TAG, "Error en la descarga automatica del modelo")
                    return@withContext false
                }
            }
            
            Log.d(TAG, "Inicializando modelo Gemma 3n...")
            _downloadStatus.value = "Cargando modelo..."
            
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(MAX_TOKENS)
                // Usar GPU si esta disponible para mejor rendimiento
                .setPreferredBackend(LlmInference.Backend.GPU)
                .build()
            
            llmInference = LlmInference.createFromOptions(context, options)
            _isModelLoaded.value = true
            _downloadStatus.value = "Modelo listo"
            
            Log.i(TAG, "Modelo Gemma 3n cargado exitosamente")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar el modelo Gemma 3n", e)
            _isModelLoaded.value = false
            _downloadStatus.value = "Error: ${e.message}"
        }
    }
    
    /**
     * Descarga el modelo automaticamente desde Hugging Face
     */
    private suspend fun downloadModel(): Boolean = withContext(Dispatchers.IO) {
        try {
            _isDownloading.value = true
            _downloadProgress.value = 0f
            _downloadStatus.value = "Conectando con Hugging Face..."
            
            val modelFile = File(context.filesDir, MODEL_FILE_NAME)
            val tempFile = File(context.filesDir, "$MODEL_FILE_NAME.tmp")
            
            Log.i(TAG, "Iniciando descarga desde: $MODEL_DOWNLOAD_URL")
            
            val url = URL(MODEL_DOWNLOAD_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.connect()
            
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Error de servidor: ${connection.responseCode}")
                _downloadStatus.value = "Error del servidor: ${connection.responseCode}"
                return@withContext false
            }
            
            val fileSize = connection.contentLength.toLong()
            Log.i(TAG, "Tamano del archivo: ${fileSize / 1024 / 1024} MB")
            
            connection.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8192)
                    var totalBytesRead = 0L
                    var bytesRead: Int
                    
                    _downloadStatus.value = "Descargando modelo (${fileSize / 1024 / 1024} MB)..."
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        
                        val progress = totalBytesRead.toFloat() / fileSize.toFloat()
                        _downloadProgress.value = progress
                        
                        val progressPercent = (progress * 100).toInt()
                        _downloadStatus.value = "Descargando: $progressPercent% (${totalBytesRead / 1024 / 1024}/${fileSize / 1024 / 1024} MB)"
                        
                        // Log cada 10%
                        if (progressPercent % 10 == 0 && progressPercent > 0) {
                            Log.d(TAG, "Progreso de descarga: $progressPercent%")
                        }
                    }
                }
            }
            
            // Verificar que la descarga este completa
            if (tempFile.length() != fileSize) {
                Log.e(TAG, "Descarga incompleta: ${tempFile.length()} != $fileSize")
                tempFile.delete()
                _downloadStatus.value = "Error: Descarga incompleta"
                return@withContext false
            }
            
            // Mover archivo temporal al definitivo
            if (tempFile.renameTo(modelFile)) {
                Log.i(TAG, "Modelo descargado exitosamente: ${modelFile.absolutePath}")
                _downloadStatus.value = "Descarga completada"
                _downloadProgress.value = 1f
                _isDownloading.value = false
                return@withContext true
            } else {
                Log.e(TAG, "Error al mover archivo temporal")
                tempFile.delete()
                _downloadStatus.value = "Error al guardar archivo"
                return@withContext false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error durante la descarga", e)
            _downloadStatus.value = "Error: ${e.message}"
            _isDownloading.value = false
            return@withContext false
        }
    }
    
    /**
     * Procesa texto de audio para transcripcion y organizacion
     * Nota: Segun la documentacion, el soporte de audio directo aun no esta
     * disponible en el SDK publico, por lo que trabajamos con texto procesado
     */
    suspend fun processAudioText(audioText: String): String = withContext(Dispatchers.IO) {
        return@withContext try {
            val inference = llmInference ?: throw IllegalStateException("Modelo no inicializado")
            
            _isProcessing.value = true
            
            // Prompt optimizado para organizar transcripciones de audio
            val prompt = """
            Eres un asistente que organiza transcripciones de audio. 
            Tu tarea es tomar el siguiente texto transcrito y organizarlo de manera clara y coherente.
            
            Texto transcrito: "$audioText"
            
            Por favor:
            1. Corrige errores de transcripcion obvios
            2. Organiza el texto en parrafos coherentes
            3. Identifica temas principales
            4. Manten el significado original
            
            Texto organizado:
            """.trimIndent()
            
            Log.d(TAG, "Procesando texto de audio con Gemma 3n...")
            
            // Usar generateResponse para obtener respuesta sincrona
            val result = inference.generateResponse(prompt)
            
            // Actualizar el resultado y estado
            _transcriptionResult.value = result
            _isProcessing.value = false
            
            Log.d(TAG, "Transcripcion completada")
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al procesar texto de audio", e)
            _isProcessing.value = false
            "Error al procesar el texto: ${e.message}"
        }
    }
    
    /**
     * Genera un resumen del texto transcrito
     */
    suspend fun generateSummary(text: String): String = withContext(Dispatchers.IO) {
        return@withContext try {
            val inference = llmInference ?: throw IllegalStateException("Modelo no inicializado")
            
            val prompt = """
            Genera un resumen conciso y estructurado del siguiente texto:
            
            "$text"
            
            Resumen:
            """.trimIndent()
            
            inference.generateResponse(prompt)
        } catch (e: Exception) {
            Log.e(TAG, "Error al generar resumen", e)
            "Error al generar resumen: ${e.message}"
        }
    }
    
    /**
     * Libera los recursos del modelo
     */
    fun release() {
        try {
            llmInference?.close()
            llmInference = null
            _isModelLoaded.value = false
            Log.d(TAG, "Recursos del modelo liberados")
        } catch (e: Exception) {
            Log.e(TAG, "Error al liberar recursos", e)
        }
    }
    
    /**
     * Verifica si el archivo del modelo existe
     */
    fun isModelFileAvailable(): Boolean {
        val modelFile = File(context.filesDir, MODEL_FILE_NAME)
        return modelFile.exists()
    }
    
    /**
     * Obtiene informacion del modelo
     */
    fun getModelInfo(): String {
        val modelFile = File(context.filesDir, MODEL_FILE_NAME)
        return if (modelFile.exists()) {
            "Modelo: Gemma 3n E4B\nTamano: ${modelFile.length() / (1024 * 1024)} MB\nUbicacion: ${modelFile.absolutePath}"
        } else {
            """
            Modelo no encontrado
            
            Para descargar el modelo:
            1. Abre una terminal en la raiz del proyecto
            2. Ejecuta: ./setup_model.sh
            3. Espera a que descargue (~4.4 GB)
            
            O descarga manualmente desde:
            https://huggingface.co/google/gemma-3n-E4B-it
            """.trimIndent()
        }
    }

    /**
     * Obtiene instrucciones de descarga del modelo
     */
    fun getDownloadInstructions(): String {
        return """
        📥 DESCARGAR MODELO GEMMA 3N
        
        Opcion 1 - Script automatico:
        1. Abre terminal en la raiz del proyecto
        2. Ejecuta: chmod +x setup_model.sh
        3. Ejecuta: ./setup_model.sh
        
        Opcion 2 - Descarga manual:
        1. Ve a: https://huggingface.co/google/gemma-3n-E4B-it
        2. Descarga el archivo .task
        3. Coloca en: ${context.filesDir}/$MODEL_FILE_NAME
        
        Tamano: ~4.4 GB
        Tiempo estimado: 10-30 minutos
        """.trimIndent()
    }
}
