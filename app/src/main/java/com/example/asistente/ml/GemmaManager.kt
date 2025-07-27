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

/**
 * Manager para manejar la inferencia con Gemma 3n
 * Basado en la documentacion oficial de Google AI Edge y MediaPipe
 */
class GemmaManager(private val context: Context) {
    
    private var llmInference: LlmInference? = null
    private val _isModelLoaded = MutableStateFlow(false)
    val isModelLoaded: StateFlow<Boolean> = _isModelLoaded.asStateFlow()
    
    private val _transcriptionResult = MutableStateFlow("")
    val transcriptionResult: StateFlow<String> = _transcriptionResult.asStateFlow()
    
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()
    
    companion object {
        private const val TAG = "GemmaManager"
        private const val MODEL_FILE_NAME = "gemma-3n-E4B-it.task"
        
        // Configuracion del modelo segun la documentacion
        private const val MAX_TOKENS = 512
    }
    
    /**
     * Inicializa el modelo Gemma 3n
     * El archivo .task debe estar en el directorio de archivos de la app
     */
    suspend fun initializeModel(): Boolean = withContext(Dispatchers.IO) {
        try {
            val modelFile = File(context.filesDir, MODEL_FILE_NAME)
            
            if (!modelFile.exists()) {
                Log.e(TAG, "Archivo del modelo no encontrado: ${modelFile.absolutePath}")
                return@withContext false
            }
            
            Log.d(TAG, "Inicializando modelo Gemma 3n...")
            
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(MAX_TOKENS)
                // Usar GPU si esta disponible para mejor rendimiento
                .setPreferredBackend(LlmInference.Backend.GPU)
                .build()
            
            llmInference = LlmInference.createFromOptions(context, options)
            _isModelLoaded.value = true
            
            Log.i(TAG, "Modelo Gemma 3n cargado exitosamente")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar el modelo Gemma 3n", e)
            _isModelLoaded.value = false
            false
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
            
            // Usar generateResponse para obtener respuesta síncrona
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
            "Modelo no encontrado"
        }
    }
}
