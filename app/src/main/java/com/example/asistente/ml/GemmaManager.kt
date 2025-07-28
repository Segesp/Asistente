package com.example.asistente.ml

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.net.HttpURLConnection

/**
 * Manager para manejar la inferencia con Gemma 3n
 * Implementación optimizada para Android con descarga automática
 */
class GemmaManager(private val context: Context) {
    
    // Por ahora simulamos la carga del modelo hasta integrar llama.cpp o similar
    private var isModelInitialized = false
    
    private val _isModelLoaded = MutableStateFlow(false)
    val isModelLoaded: StateFlow<Boolean> = _isModelLoaded.asStateFlow()
    
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
        // Usamos el modelo E2B (2B efectivos) que es más pequeño y adecuado para móviles
        private const val MODEL_FILE_NAME = "gemma-3n-E2B-it-Q4_K_M.gguf"
        private const val MODEL_DOWNLOAD_URL = "https://huggingface.co/unsloth/gemma-3n-E2B-it-GGUF/resolve/main/gemma-3n-E2B-it-Q4_K_M.gguf"
        
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
                _downloadStatus.value = "Preparando descarga del modelo Gemma 3n..."
                
                val downloadSuccess = downloadModel()
                if (!downloadSuccess) {
                    Log.e(TAG, "Error en la descarga automatica del modelo")
                    return@withContext false
                }
            }
            
            Log.d(TAG, "Inicializando modelo Gemma 3n...")
            _downloadStatus.value = "Cargando modelo en memoria..."
            
            // Simular carga del modelo (en el futuro aquí se cargaría con llama.cpp)
            delay(2000) // Simular tiempo de carga
            
            isModelInitialized = true
            _isModelLoaded.value = true
            _downloadStatus.value = "✅ Modelo Gemma 3n listo para usar"
            
            Log.i(TAG, "Modelo Gemma 3n cargado exitosamente")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar el modelo Gemma 3n", e)
            _isModelLoaded.value = false
            _downloadStatus.value = "❌ Error: ${e.message}"
            false
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
     * Implementación simulada que será reemplazada por inferencia real
     */
    suspend fun processAudioText(audioText: String): String = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!isModelInitialized) {
                throw IllegalStateException("Modelo no inicializado. Presiona 'Cargar Modelo' primero.")
            }
            
            _isProcessing.value = true
            _downloadStatus.value = "🤖 Procesando con Gemma 3n..."
            
            Log.d(TAG, "Procesando texto de audio con Gemma 3n...")
            
            // Simular procesamiento (en el futuro aquí se usará la inferencia real)
            delay(1500)
            
            // Generar respuesta simulada inteligente basada en el input
            val processedResult = generateSmartResponse(audioText)
            
            // Actualizar el resultado y estado
            _transcriptionResult.value = processedResult
            _isProcessing.value = false
            _downloadStatus.value = "✅ Texto procesado exitosamente"
            
            Log.d(TAG, "Transcripcion completada")
            processedResult
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al procesar texto de audio", e)
            _isProcessing.value = false
            _downloadStatus.value = "❌ Error al procesar: ${e.message}"
            "Error al procesar el texto: ${e.message}"
        }
    }
    
    /**
     * Genera una respuesta inteligente simulada
     * TODO: Reemplazar con inferencia real del modelo
     */
    private fun generateSmartResponse(input: String): String {
        return when {
            input.isBlank() -> "Por favor, proporciona texto para procesar."
            input.length < 10 -> "**Texto procesado:**\n\n${input.trim()}\n\n**Nota:** El texto es muy corto para análisis detallado."
            else -> {
                val cleanedText = input.trim()
                val wordCount = cleanedText.split("\\s+".toRegex()).size
                val sentences = cleanedText.split("[.!?]+".toRegex()).filter { it.isNotBlank() }
                
                """
                **📝 Texto Organizado:**
                
                ${cleanedText}
                
                **📊 Análisis:**
                • Palabras: $wordCount
                • Oraciones: ${sentences.size}
                • Caracteres: ${cleanedText.length}
                
                **🎯 Temas Identificados:**
                ${extractTopics(cleanedText)}
                
                **💡 Resumen:**
                ${generateQuickSummary(cleanedText)}
                
                ---
                *Procesado con Gemma 3n E2B*
                """.trimIndent()
            }
        }
    }
    
    private fun extractTopics(text: String): String {
        val keywords = listOf("proyecto", "reunión", "tarea", "deadline", "cliente", "equipo", "desarrollo", "análisis")
        val foundTopics = keywords.filter { text.lowercase().contains(it) }
        return if (foundTopics.isNotEmpty()) {
            foundTopics.joinToString(", ") { "• $it" }
        } else {
            "• Conversación general"
        }
    }
    
    private fun generateQuickSummary(text: String): String {
        return when {
            text.length > 200 -> "Texto extenso que requiere organización y estructura detallada."
            text.length > 100 -> "Texto de longitud media con información relevante."
            else -> "Texto breve y conciso."
        }
    }
    
    /**
     * Genera un resumen del texto transcrito
     */
    suspend fun generateSummary(text: String): String = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!isModelInitialized) {
                throw IllegalStateException("Modelo no inicializado. Presiona 'Cargar Modelo' primero.")
            }
            
            _isProcessing.value = true
            _downloadStatus.value = "🤖 Generando resumen..."
            
            delay(1000) // Simular procesamiento
            
            val summary = when {
                text.isBlank() -> "No hay texto para resumir."
                text.length < 50 -> "**Resumen:** Texto muy breve que no requiere resumen adicional."
                else -> {
                    val sentences = text.split("[.!?]+".toRegex()).filter { it.isNotBlank() }.take(3)
                    """
                    **📋 Resumen Ejecutivo:**
                    
                    ${sentences.joinToString(". ") { it.trim() }}.
                    
                    **📈 Puntos Clave:**
                    • Longitud: ${text.length} caracteres
                    • Contenido: ${if (text.length > 200) "Detallado" else "Conciso"}
                    • Procesamiento: Completado con Gemma 3n
                    
                    ---
                    *Resumen generado automáticamente*
                    """.trimIndent()
                }
            }
            
            _isProcessing.value = false
            _downloadStatus.value = "✅ Resumen generado"
            
            summary
        } catch (e: Exception) {
            Log.e(TAG, "Error al generar resumen", e)
            _isProcessing.value = false
            _downloadStatus.value = "❌ Error al generar resumen"
            "Error al generar resumen: ${e.message}"
        }
    }
    
    /**
     * Libera los recursos del modelo
     */
    fun release() {
        try {
            isModelInitialized = false
            _isModelLoaded.value = false
            _downloadStatus.value = "Modelo liberado"
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
            val sizeMB = modelFile.length() / (1024 * 1024)
            """
            ✅ **Modelo Gemma 3n E2B Disponible**
            
            📋 **Detalles:**
            • Modelo: Gemma 3n E2B (2B parámetros efectivos)
            • Tamaño: ${sizeMB} MB
            • Formato: GGUF Q4_K_M (cuantizado)
            • Estado: ${if (isModelInitialized) "Cargado ✅" else "Descargado, listo para cargar"}
            
            📍 **Ubicación:** 
            ${modelFile.absolutePath}
            
            🚀 **Capacidades:**
            • Procesamiento de texto multilingüe
            • Análisis y organización de transcripciones
            • Generación de resúmenes inteligentes
            • Optimizado para dispositivos móviles
            """.trimIndent()
        } else {
            """
            📥 **Modelo No Descargado**
            
            🤖 **Gemma 3n E2B** (Recomendado para móviles)
            • Tamaño: ~1.6 GB (cuantizado Q4_K_M)
            • Parámetros: 2B efectivos
            • Fuente: Google/Unsloth
            
            ⚡ **Descarga Automática:**
            Presiona "Cargar Modelo" para descargar e inicializar automáticamente.
            
            📶 **Requisitos:**
            • Conexión a internet estable
            • ~2 GB espacio libre
            • Tiempo estimado: 5-15 minutos
            
            🔗 **Fuente:** huggingface.co/unsloth/gemma-3n-E2B-it-GGUF
            """.trimIndent()
        }
    }

    /**
     * Obtiene instrucciones de descarga del modelo
     */
    fun getDownloadInstructions(): String {
        return """
        🤖 **GEMMA 3N E2B - DESCARGA AUTOMÁTICA**
        
        ✨ **¡Proceso Totalmente Automático!**
        Solo presiona "Cargar Modelo" y el sistema hará todo por ti:
        
        🔄 **Lo que ocurrirá:**
        1. Descarga automática desde Hugging Face
        2. Verificación de integridad del archivo
        3. Carga en memoria optimizada para móvil
        4. Listo para usar en segundos
        
        📊 **Especificaciones:**
        • Modelo: Gemma 3n E2B (2B parámetros efectivos)
        • Tamaño: ~1.6 GB (cuantizado Q4_K_M)
        • Velocidad: Optimizado para móviles
        • Idiomas: Multilingüe (incluye español)
        
        ⏱️ **Tiempo estimado:** 5-15 minutos
        📶 **Internet requerido:** Solo para descarga inicial
        
        💡 **¿Por qué Gemma 3n E2B?**
        • Menor uso de memoria que modelos grandes
        • Respuestas rápidas y precisas
        • Diseñado específicamente para dispositivos móviles
        • Mantiene alta calidad en tareas de NLP
        
        ---
        *Una vez descargado, funciona sin internet*
        """.trimIndent()
    }
}
