package com.example.asistente.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Manager para la grabacion y procesamiento de audio
 * Optimizado para trabajar con Gemma 3n segun las especificaciones oficiales
 */
class AudioManager(private val context: Context) {
    
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var isRecordingActive = false
    
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    
    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()
    
    private val _recordedText = MutableStateFlow("")
    val recordedText: StateFlow<String> = _recordedText.asStateFlow()
    
    companion object {
        private const val TAG = "AudioManager"
        
        // Configuracion segun especificaciones de Gemma 3n
        private const val SAMPLE_RATE = 16000 // 16 kHz
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO // Un solo canal
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        
        // Configuracion de buffer
        private const val BUFFER_SIZE_MULTIPLIER = 2
        private const val CHUNK_DURATION_MS = 3000 // 3 segundos por chunk
        private const val CHUNK_SIZE = SAMPLE_RATE * CHUNK_DURATION_MS / 1000
        
        // Umbral de silencio para detectar pausas
        private const val SILENCE_THRESHOLD = 500
        private const val SILENCE_DURATION_MS = 1000 // 1 segundo de silencio
    }
    
    /**
     * Verifica si tiene permisos de audio
     */
    fun hasAudioPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Inicializa el AudioRecord
     */
    @SuppressLint("MissingPermission")
    private fun initializeAudioRecord(): Boolean {
        try {
            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            ) * BUFFER_SIZE_MULTIPLIER
            
            if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                Log.e(TAG, "Error al obtener el tamano del buffer")
                return false
            }
            
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord no se pudo inicializar")
                return false
            }
            
            Log.d(TAG, "AudioRecord inicializado correctamente")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error al inicializar AudioRecord", e)
            return false
        }
    }
    
    /**
     * Inicia la grabacion continua
     */
    fun startRecording(onAudioChunk: (FloatArray) -> Unit) {
        if (!hasAudioPermission()) {
            Log.e(TAG, "No se tienen permisos de audio")
            return
        }
        
        if (isRecordingActive) {
            Log.w(TAG, "La grabacion ya esta activa")
            return
        }
        
        if (!initializeAudioRecord()) {
            Log.e(TAG, "No se pudo inicializar AudioRecord")
            return
        }
        
        isRecordingActive = true
        _isRecording.value = true
        
        recordingJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                audioRecord?.startRecording()
                Log.d(TAG, "Grabacion iniciada")
                
                val audioBuffer = ShortArray(CHUNK_SIZE)
                val floatBuffer = FloatArray(CHUNK_SIZE)
                var silenceCounter = 0
                
                while (isRecordingActive && !currentCoroutineContext()[Job]?.isCancelled!!) {
                    val bytesRead = audioRecord?.read(audioBuffer, 0, CHUNK_SIZE) ?: 0
                    
                    if (bytesRead > 0) {
                        // Convertir a float y calcular nivel de audio
                        var maxAmplitude = 0
                        for (i in 0 until bytesRead) {
                            val sample = audioBuffer[i].toInt()
                            floatBuffer[i] = sample / 32768.0f // Normalizar a [-1, 1]
                            maxAmplitude = maxOf(maxAmplitude, kotlin.math.abs(sample))
                        }
                        
                        // Actualizar nivel de audio para UI
                        _audioLevel.value = maxAmplitude / 32768.0f
                        
                        // Detectar silencio
                        if (maxAmplitude < SILENCE_THRESHOLD) {
                            silenceCounter += CHUNK_DURATION_MS
                        } else {
                            silenceCounter = 0
                        }
                        
                        // Si hay audio significativo, procesarlo
                        if (maxAmplitude >= SILENCE_THRESHOLD) {
                            Log.d(TAG, "Procesando chunk de audio (${bytesRead} samples)")
                            onAudioChunk(floatBuffer.copyOf(bytesRead))
                        }
                        
                        // Pequena pausa para no saturar el CPU
                        delay(100)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error durante la grabacion", e)
            } finally {
                audioRecord?.stop()
                Log.d(TAG, "Grabacion detenida")
            }
        }
    }
    
    /**
     * Detiene la grabacion
     */
    fun stopRecording() {
        isRecordingActive = false
        _isRecording.value = false
        
        recordingJob?.cancel()
        recordingJob = null
        
        audioRecord?.apply {
            if (state == AudioRecord.STATE_INITIALIZED) {
                stop()
            }
            release()
        }
        audioRecord = null
        
        _audioLevel.value = 0f
        Log.d(TAG, "Grabacion detenida y recursos liberados")
    }
    
    /**
     * Convierte audio a formato compatible con Gemma 3n
     * Segun la documentacion: 32 bits float, 16kHz, mono
     */
    fun convertToGemmaFormat(audioData: FloatArray): ByteArray {
        val byteBuffer = ByteBuffer.allocate(audioData.size * 4) // 4 bytes por float
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        
        for (sample in audioData) {
            byteBuffer.putFloat(sample)
        }
        
        return byteBuffer.array()
    }
    
    /**
     * Guarda audio en archivo WAV
     */
    fun saveAudioToFile(audioData: FloatArray, fileName: String): File? {
        return try {
            val file = File(context.filesDir, fileName)
            val fos = FileOutputStream(file)
            
            // Header WAV simplificado
            val header = createWavHeader(audioData.size)
            fos.write(header)
            
            // Datos de audio
            val audioBytes = convertToGemmaFormat(audioData)
            fos.write(audioBytes)
            
            fos.close()
            Log.d(TAG, "Audio guardado en: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar audio", e)
            null
        }
    }
    
    /**
     * Crea header WAV basico
     */
    private fun createWavHeader(audioDataSize: Int): ByteArray {
        val header = ByteArray(44)
        val byteBuffer = ByteBuffer.wrap(header)
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        
        // RIFF header
        byteBuffer.put("RIFF".toByteArray())
        byteBuffer.putInt(36 + audioDataSize * 4)
        byteBuffer.put("WAVE".toByteArray())
        
        // Format chunk
        byteBuffer.put("fmt ".toByteArray())
        byteBuffer.putInt(16) // PCM format chunk size
        byteBuffer.putShort(3) // IEEE float format
        byteBuffer.putShort(1) // Mono
        byteBuffer.putInt(SAMPLE_RATE)
        byteBuffer.putInt(SAMPLE_RATE * 4) // Byte rate
        byteBuffer.putShort(4) // Block align
        byteBuffer.putShort(32) // Bits per sample
        
        // Data chunk
        byteBuffer.put("data".toByteArray())
        byteBuffer.putInt(audioDataSize * 4)
        
        return header
    }
    
    /**
     * Simula transcripcion de audio a texto
     * En una implementacion real, aqui iria el procesamiento con Gemma 3n
     * cuando el soporte de audio este disponible en el SDK
     */
    fun simulateTranscription(audioData: FloatArray): String {
        // Por ahora simulamos la transcripcion
        val avgAmplitude = audioData.map { kotlin.math.abs(it) }.average()
        return when {
            avgAmplitude > 0.1 -> "Audio detectado con alta intensidad"
            avgAmplitude > 0.05 -> "Audio detectado con intensidad media"
            avgAmplitude > 0.01 -> "Audio detectado con baja intensidad"
            else -> "Silencio o ruido muy bajo"
        }
    }
    
    /**
     * Libera todos los recursos
     */
    fun release() {
        stopRecording()
    }
}
