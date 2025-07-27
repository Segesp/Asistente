package com.example.asistente.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.asistente.MainActivity
import com.example.asistente.R
import com.example.asistente.audio.AudioManager
import com.example.asistente.ml.GemmaManager
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Servicio en primer plano para grabacion continua de audio
 * Permite grabar audio incluso cuando la app esta en segundo plano
 */
class AudioRecordingService : Service() {
    
    private lateinit var audioManager: AudioManager
    private lateinit var gemmaManager: GemmaManager
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private val transcriptBuilder = StringBuilder()
    private var isServiceRunning = false
    
    companion object {
        private const val TAG = "AudioRecordingService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "audio_recording_channel"
        
        const val ACTION_START_RECORDING = "START_RECORDING"
        const val ACTION_STOP_RECORDING = "STOP_RECORDING"
        const val ACTION_GET_TRANSCRIPT = "GET_TRANSCRIPT"
        
        fun startRecording(context: Context) {
            val intent = Intent(context, AudioRecordingService::class.java).apply {
                action = ACTION_START_RECORDING
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopRecording(context: Context) {
            val intent = Intent(context, AudioRecordingService::class.java).apply {
                action = ACTION_STOP_RECORDING
            }
            context.startService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Servicio creado")
        
        audioManager = AudioManager(this)
        gemmaManager = GemmaManager(this)
        
        createNotificationChannel()
        
        // Inicializar Gemma 3n
        serviceScope.launch {
            val success = gemmaManager.initializeModel()
            if (success) {
                Log.i(TAG, "Modelo Gemma 3n cargado en el servicio")
            } else {
                Log.e(TAG, "Error al cargar Gemma 3n en el servicio")
            }
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> startRecordingProcess()
            ACTION_STOP_RECORDING -> stopRecordingProcess()
        }
        return START_STICKY // Reiniciar el servicio si es terminado por el sistema
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun startRecordingProcess() {
        if (isServiceRunning) {
            Log.w(TAG, "El servicio ya esta grabando")
            return
        }
        
        Log.i(TAG, "Iniciando grabacion continua")
        isServiceRunning = true
        
        // Crear notificacion persistente
        val notification = createNotification("Grabacion activa", "El asistente esta escuchando...")
        startForeground(NOTIFICATION_ID, notification)
        
        // Iniciar grabacion de audio
        audioManager.startRecording { audioChunk ->
            processAudioChunk(audioChunk)
        }
        
        // Agregar timestamp inicial al documento
        val timestamp = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        transcriptBuilder.append("=== Sesion iniciada: $timestamp ===\n\n")
    }
    
    private fun stopRecordingProcess() {
        if (!isServiceRunning) {
            Log.w(TAG, "El servicio no esta grabando")
            return
        }
        
        Log.i(TAG, "Deteniendo grabacion continua")
        isServiceRunning = false
        
        audioManager.stopRecording()
        
        // Agregar timestamp final y guardar documento
        val timestamp = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        transcriptBuilder.append("\n=== Sesion finalizada: $timestamp ===")
        
        saveTranscriptToFile()
        
        stopForeground(true)
        stopSelf()
    }
    
    private fun processAudioChunk(audioData: FloatArray) {
        serviceScope.launch {
            try {
                // Por ahora simulamos la transcripcion ya que el soporte de audio
                // directo en Gemma 3n no esta disponible en el SDK publico
                val simulatedText = audioManager.simulateTranscription(audioData)
                
                if (simulatedText.isNotBlank() && !simulatedText.contains("Silencio")) {
                    Log.d(TAG, "Audio procesado: $simulatedText")
                    
                    // Procesar con Gemma 3n para organizar y mejorar el texto
                    val processedText = gemmaManager.processAudioText(simulatedText)
                    
                    // Agregar al documento con timestamp
                    val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    transcriptBuilder.append("[$timestamp] $processedText\n")
                    
                    // Actualizar notificacion
                    updateNotification("Grabacion activa", "Ultimo: $simulatedText")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al procesar audio", e)
            }
        }
    }
    
    private fun saveTranscriptToFile() {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "transcript_$timestamp.txt"
            val file = java.io.File(filesDir, fileName)
            
            file.writeText(transcriptBuilder.toString())
            Log.i(TAG, "Transcripcion guardada en: ${file.absolutePath}")
            
            // Generar resumen con Gemma 3n
            serviceScope.launch {
                try {
                    val summary = gemmaManager.generateSummary(transcriptBuilder.toString())
                    val summaryFile = java.io.File(filesDir, "summary_$timestamp.txt")
                    summaryFile.writeText(summary)
                    Log.i(TAG, "Resumen guardado en: ${summaryFile.absolutePath}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error al generar resumen", e)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar transcripcion", e)
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Grabacion de Audio",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificaciones para el servicio de grabacion continua"
                setSound(null, null)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(title: String, content: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
    
    private fun updateNotification(title: String, content: String) {
        val notification = createNotification(title, content)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Servicio destruido")
        
        isServiceRunning = false
        audioManager.release()
        gemmaManager.release()
        serviceScope.cancel()
    }
    
    fun getCurrentTranscript(): String = transcriptBuilder.toString()
}
