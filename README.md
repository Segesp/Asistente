# Asistente IA Local con Gemma 3n

Una aplicación Android que funciona como un asistente de voz inteligente, capaz de escuchar continuamente el micrófono, transcribir el audio usando IA local y organizar la información en documentos estructurados.

## Características Principales

### 🎤 Grabación Continua
- Escucha continua del micrófono en segundo plano
- Detección automática de silencio
- Optimizado para batería con grabación inteligente

### 🧠 IA Local con Gemma 3n
- Modelo Gemma 3n E4B ejecutándose completamente en el dispositivo
- Sin dependencia de internet para procesamiento
- Privacidad total - los datos nunca salen del dispositivo

### 📝 Transcripción y Organización
- Transcripción automática del audio (simulada por limitaciones del SDK)
- Organización inteligente del texto usando Gemma 3n
- Generación de resúmenes automáticos
- Almacenamiento local de transcripciones

### 🔒 Privacidad y Seguridad
- Todo el procesamiento ocurre localmente
- Sin envío de datos a servidores externos
- Control total sobre la información

## Requisitos del Sistema

### Hardware Mínimo
- **RAM**: 3 GB mínimo (recomendado 4 GB+)
- **Almacenamiento**: 5 GB libres para el modelo
- **Android**: API 24+ (Android 7.0+)
- **GPU**: Opcional pero recomendada para mejor rendimiento

### Permisos Necesarios
- `RECORD_AUDIO`: Para grabación del micrófono
- `INTERNET`: Para descarga inicial del modelo
- `WRITE_EXTERNAL_STORAGE`: Para guardar transcripciones
- `FOREGROUND_SERVICE`: Para grabación en segundo plano
- `WAKE_LOCK`: Para mantener el dispositivo activo durante grabación

## Instalación y Configuración

### 1. Preparación del Modelo Gemma 3n

Dado que el modelo Gemma 3n E4B es demasiado grande para incluir en el APK (~4.4 GB), debe descargarse por separado:

#### Opción A: Descarga desde Hugging Face
```bash
# Usando git-lfs
git clone https://huggingface.co/google/gemma-3n-E4B-it-litert-preview
```

#### Opción B: Descarga desde Kaggle
1. Ve a la página del modelo en Kaggle
2. Descarga el archivo `gemma-3n-E4B-it.task`

### 2. Transferir el Modelo al Dispositivo

#### Para Desarrollo (usando ADB):
```bash
adb push gemma-3n-E4B-it.task /data/local/tmp/
adb shell "run-as com.example.asistente cp /data/local/tmp/gemma-3n-E4B-it.task /data/data/com.example.asistente/files/"
```

#### Para Producción:
La aplicación puede configurarse para descargar automáticamente el modelo desde un servidor en el primer inicio.

### 3. Compilación

```bash
# Clonar el repositorio
git clone [URL_DEL_REPOSITORIO]
cd Asistente

# Compilar la aplicación
./gradlew assembleDebug

# Instalar en dispositivo conectado
./gradlew installDebug
```

## Uso de la Aplicación

### Primera Configuración

1. **Permisos**: Al abrir la app por primera vez, concede permisos de micrófono
2. **Modelo**: Presiona "Cargar Modelo" para inicializar Gemma 3n
3. **Verificación**: Confirma que todos los estados muestren ✅

### Grabación Continua

1. **Iniciar**: Presiona "Iniciar Grabación"
2. **Monitoreo**: Observa el nivel de audio en tiempo real
3. **Procesamiento**: El audio se procesa automáticamente con IA
4. **Resultados**: Las transcripciones aparecen en tiempo real

### Gestión de Documentos

- **Transcripciones**: Se guardan automáticamente con timestamp
- **Resúmenes**: Genera resúmenes manuales o automáticos
- **Archivos**: Visualiza y gestiona todos los documentos guardados
- **Eliminación**: Elimina archivos innecesarios desde la app

## Arquitectura Técnica

### Componentes Principales

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   MainActivity  │────│  MainViewModel  │────│   GemmaManager  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │                       │                       │
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ AudioRecording  │────│   AudioManager  │────│   MediaPipe     │
│    Service      │    │                 │    │   LiteRT        │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### Flujo de Datos

1. **AudioManager** captura audio del micrófono
2. **AudioRecordingService** procesa en segundo plano
3. **GemmaManager** aplica IA para transcribir/organizar
4. **MainViewModel** coordina estado y UI
5. **MainActivity** muestra resultados al usuario

### Especificaciones de Audio

Según la documentación de Gemma 3n:
- **Frecuencia de muestreo**: 16 kHz
- **Canales**: Mono (1 canal)
- **Formato**: 32-bit float (PCM internamente para Android)
- **Duración máxima por chunk**: 30 segundos

## Limitaciones Actuales

### SDK de MediaPipe
- El soporte de audio directo para Gemma 3n aún no está disponible en el SDK público de Android
- Actualmente se simula la transcripción mientras se espera la actualización oficial
- La funcionalidad de visión (imágenes) sí está disponible

### Rendimiento
- Primera carga del modelo: 30-60 segundos
- Procesamiento en GPU: ~23 tokens/segundo (Pixel/Samsung high-end)
- Procesamiento en CPU: ~10 tokens/segundo

### Memoria
- Modelo E4B requiere ~3 GB RAM
- Recomendado cerrar otras apps durante uso intensivo

## Optimización y Configuración Avanzada

### Configuración del Modelo

En `GemmaManager.kt` puedes ajustar:

```kotlin
private const val MAX_TOKENS = 512      // Longitud máxima de respuesta
private const val TEMPERATURE = 0.8f    // Creatividad (0.0 - 1.0)
private const val TOP_K = 40            // Diversidad de tokens
```

### Configuración de Audio

En `AudioManager.kt`:

```kotlin
private const val CHUNK_DURATION_MS = 3000    // Duración de chunks
private const val SILENCE_THRESHOLD = 500     // Umbral de silencio
private const val SILENCE_DURATION_MS = 1000  // Duración mínima de silencio
```

## Roadmap y Futuras Mejoras

### Próximas Funcionalidades
- [ ] Soporte nativo de audio cuando esté disponible en MediaPipe
- [ ] Integración con modelos de STT especializados
- [ ] Exportación de documentos a formatos estándar
- [ ] Configuración avanzada de prompts
- [ ] Modo de baja latencia para conversaciones

### Optimizaciones Planificadas
- [ ] Cuantización dinámica según recursos disponibles
- [ ] Cache inteligente de respuestas frecuentes
- [ ] Modo de ahorro de batería
- [ ] Procesamiento en streaming mejorado

## Solución de Problemas

### Modelo No Carga
1. Verificar que el archivo `gemma-3n-E4B-it.task` esté en `/data/data/com.example.asistente/files/`
2. Confirmar permisos de lectura
3. Verificar espacio de almacenamiento suficiente
4. Reiniciar la aplicación

### Problemas de Audio
1. Verificar permisos de micrófono
2. Confirmar que el micrófono no esté siendo usado por otra app
3. Revisar configuración de audio del sistema
4. Probar con auriculares con micrófono

### Rendimiento Lento
1. Cerrar aplicaciones en segundo plano
2. Cambiar de GPU a CPU backend (o viceversa)
3. Reducir MAX_TOKENS en configuración
4. Considerar usar modelo E2B en lugar de E4B

## Contribuciones

Las contribuciones son bienvenidas. Areas de especial interés:

- **Optimización de rendimiento**
- **Mejoras en la UI/UX**
- **Integración con otros modelos de IA**
- **Pruebas en diferentes dispositivos**
- **Documentación y tutoriales**

## Licencia

Este proyecto utiliza:
- **Gemma 3n**: Bajo licencia de uso responsable de Google
- **MediaPipe**: Apache License 2.0
- **Código de la aplicación**: [Especificar licencia]

## Referencias y Documentación

- [Documentación oficial de Gemma 3n](https://ai.google.dev/gemma)
- [MediaPipe GenAI API](https://developers.google.com/mediapipe/solutions/genai)
- [Google AI Edge](https://ai.google.dev/edge)
- [Repositorio oficial de Gemma](https://github.com/google-deepmind/gemma)

## Contacto y Soporte

Para reportar bugs, solicitar funcionalidades o contribuir:
- **Issues**: [URL_ISSUES]
- **Discussions**: [URL_DISCUSSIONS]
- **Email**: [EMAIL_CONTACTO]

---

**Nota**: Esta aplicación está diseñada para propósitos educativos y de investigación. Para uso en producción, considera las implicaciones de privacidad y rendimiento según tu caso de uso específico.
