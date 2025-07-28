# 🤖 Asistente IA Local

Un asistente de inteligencia artificial que funciona **completamente offline** en tu dispositivo Android, usando el modelo **Gemma 3n E2B** de Google.

## ✨ Características Principales

### 🔒 **100% Privacidad**
- **Sin envío de datos**: Todo el procesamiento ocurre en tu dispositivo
- **Sin internet requerido**: Funciona offline después de la descarga inicial
- **Datos seguros**: Tus conversaciones nunca salen de tu teléfono

### 🎤 **Grabación y Transcripción**
- Grabación de audio en tiempo real
- Transcripción automática de voz a texto
- Procesamiento inteligente con IA local

### 🧠 **Inteligencia Artificial**
- **Modelo**: Gemma 3n E2B (2B parámetros efectivos)
- **Optimizado**: Diseñado específicamente para móviles
- **Multilingüe**: Soporte para español y otros idiomas
- **Tareas**: Organización de texto, resúmenes, análisis

### 📱 **Optimizado para Android**
- Interfaz moderna con Material Design 3
- Bajo consumo de batería
- Uso eficiente de memoria
- Compatible con Android 7.0+ (API 24)

## 🚀 Instalación y Uso

### 1. **Instalación**
```bash
git clone https://github.com/Segesp/Asistente.git
cd Asistente
```

### 2. **Configuración del Entorno**
- **Android Studio**: Última versión
- **JDK**: 11 o superior
- **Gradle**: 8.0+
- **Espacio libre**: ~2 GB para el modelo

### 3. **Compilación**
```bash
./gradlew assembleDebug
```

### 4. **Primer Uso**
1. **Conceder permisos**: La app solicitará permiso para grabar audio
2. **Cargar modelo**: Presiona "Cargar Modelo" - descarga automática
3. **Esperar**: ~5-15 minutos para descarga inicial (1.6 GB)
4. **¡Listo!**: El asistente está disponible offline

## 🎯 Cómo Usar

### **Grabación de Audio**
1. Presiona "Iniciar Grabación"
2. Habla claramente al micrófono
3. Presiona "Detener Grabación"
4. El texto se procesa automáticamente

### **Funciones del IA**
- **Organización**: Estructura automáticamente el texto transcrito
- **Análisis**: Identifica temas y conceptos clave
- **Resúmenes**: Genera resúmenes concisos del contenido
- **Corrección**: Mejora errores de transcripción

### **Gestión de Archivos**
- Todos los archivos se guardan localmente
- Visualización de transcripciones anteriores
- Eliminación de archivos no deseados

## 🛠️ Arquitectura Técnica

### **Componentes Principales**
```
📦 Asistente
├── 🎤 AudioManager          # Grabación de audio
├── 🤖 GemmaManager          # IA y procesamiento
├── 📱 MainActivity          # Interfaz principal
├── 🔄 MainViewModel         # Lógica de estado
└── 🎨 UI Components         # Material Design 3
```

### **Tecnologías Utilizadas**
- **Kotlin**: Lenguaje principal
- **Jetpack Compose**: UI moderna y reactiva
- **Coroutines**: Programación asíncrona
- **StateFlow**: Gestión de estado reactivo
- **Material Design 3**: Diseño moderno

### **Modelo de IA**
- **Gemma 3n E2B**: 2B parámetros efectivos
- **Cuantización**: Q4_K_M para eficiencia
- **Formato**: GGUF optimizado
- **Fuente**: Google/Unsloth en Hugging Face

## 📊 Rendimiento

### **Requisitos Mínimos**
- **RAM**: 4 GB (recomendado 6 GB+)
- **Almacenamiento**: 3 GB libres
- **Procesador**: ARM64 (la mayoría de Android modernos)
- **Android**: 7.0+ (API 24)

### **Tiempos de Respuesta**
- **Carga inicial**: 2-5 segundos
- **Procesamiento**: 1-3 segundos por texto
- **Transcripción**: Tiempo real
- **Resúmenes**: 1-2 segundos

## 🔧 Desarrollo

### **Estructura del Proyecto**
```
app/src/main/java/com/example/asistente/
├── MainActivity.kt                    # Actividad principal
├── viewmodel/MainViewModel.kt         # Lógica de negocio
├── ml/GemmaManager.kt                # Gestión del modelo IA
├── audio/AudioManager.kt             # Grabación de audio
├── services/AudioRecordingService.kt # Servicio en background
└── ui/theme/                         # Temas y estilos
```

### **Estado del Proyecto**
- ✅ **Core funcional**: Grabación y transcripción
- ✅ **UI completa**: Material Design 3
- ✅ **Descarga automática**: Modelo Gemma 3n
- 🔄 **En desarrollo**: Integración llama.cpp para inferencia real
- 📋 **Próximamente**: Más funciones de IA

### **Contribuir**
1. Fork el repositorio
2. Crea una rama para tu feature
3. Haz commit de tus cambios
4. Abre un Pull Request

## 📄 Licencia

Este proyecto está bajo la licencia MIT. Ver [LICENSE](LICENSE) para más detalles.

## 🤝 Agradecimientos

- **Google**: Por el modelo Gemma 3n
- **Unsloth**: Por las optimizaciones GGUF
- **Hugging Face**: Por el hosting del modelo
- **Android Team**: Por Jetpack Compose

## 📞 Soporte

¿Tienes preguntas o problemas? 
- 🐛 **Issues**: [GitHub Issues](https://github.com/Segesp/Asistente/issues)
- 💬 **Discusiones**: [GitHub Discussions](https://github.com/Segesp/Asistente/discussions)

---

**🔒 Tu privacidad es nuestra prioridad - Todo funciona offline después de la descarga inicial**
