#!/bin/bash

# Script para descargar y configurar el modelo Gemma 3n para la aplicación Asistente
# Asegúrate de tener git-lfs instalado y configurado

echo "=== Descarga del Modelo Gemma 3n E4B ==="
echo ""

# Verificar si git-lfs está instalado
if ! command -v git-lfs &> /dev/null; then
    echo "❌ git-lfs no está instalado. Instálalo primero:"
    echo "   Ubuntu/Debian: sudo apt install git-lfs"
    echo "   macOS: brew install git-lfs"
    echo "   Windows: Descarga desde https://git-lfs.github.io/"
    exit 1
fi

# Crear directorio temporal
TEMP_DIR="./temp_model_download"
mkdir -p "$TEMP_DIR"
cd "$TEMP_DIR"

echo "📥 Descargando modelo Gemma 3n E4B desde Hugging Face..."
echo "   Tamaño aproximado: 4.4 GB"
echo "   Esto puede tomar varios minutos dependiendo de tu conexión..."
echo ""

# Clonar el repositorio del modelo
git clone https://huggingface.co/google/gemma-3n-E4B-it-litert-preview

if [ $? -ne 0 ]; then
    echo "❌ Error al descargar el modelo"
    exit 1
fi

cd gemma-3n-E4B-it-litert-preview

# Verificar que el archivo .task existe
if [ ! -f "gemma-3n-E4B-it.task" ]; then
    echo "❌ Archivo del modelo no encontrado"
    exit 1
fi

echo "✅ Modelo descargado exitosamente"
echo ""

# Verificar si hay un dispositivo Android conectado
echo "🔍 Verificando dispositivos Android conectados..."
if ! command -v adb &> /dev/null; then
    echo "❌ ADB no está instalado. Instala Android SDK Platform Tools"
    exit 1
fi

DEVICE_COUNT=$(adb devices | grep -c "device$")
if [ "$DEVICE_COUNT" -eq 0 ]; then
    echo "❌ No hay dispositivos Android conectados"
    echo "   Conecta tu dispositivo Android y habilita la depuración USB"
    exit 1
fi

echo "✅ Dispositivo Android detectado"
echo ""

# Copiar modelo al dispositivo
echo "📱 Copiando modelo al dispositivo Android..."
echo "   Esto puede tomar varios minutos..."

# Primer paso: copiar a directorio temporal accesible
adb push gemma-3n-E4B-it.task /data/local/tmp/

if [ $? -ne 0 ]; then
    echo "❌ Error al copiar modelo al dispositivo"
    exit 1
fi

echo "✅ Modelo copiado a directorio temporal del dispositivo"
echo ""

# Verificar si la aplicación está instalada
PACKAGE_NAME="com.example.asistente"
if adb shell pm list packages | grep -q "$PACKAGE_NAME"; then
    echo "📱 Aplicación detectada. Copiando modelo al directorio de la app..."
    
    # Copiar al directorio privado de la aplicación
    adb shell "run-as $PACKAGE_NAME cp /data/local/tmp/gemma-3n-E4B-it.task /data/data/$PACKAGE_NAME/files/" 2>/dev/null
    
    if [ $? -eq 0 ]; then
        echo "✅ Modelo configurado exitosamente para la aplicación"
        
        # Limpiar archivo temporal
        adb shell "rm /data/local/tmp/gemma-3n-E4B-it.task"
        
    else
        echo "⚠️  No se pudo copiar directamente al directorio de la app"
        echo "   El archivo está en /data/local/tmp/gemma-3n-E4B-it.task"
        echo "   La aplicación intentará copiarlo automáticamente al iniciarse"
    fi
else
    echo "⚠️  Aplicación no está instalada en el dispositivo"
    echo "   El archivo está en /data/local/tmp/gemma-3n-E4B-it.task"
    echo "   Instala la aplicación primero, luego ejecútala para configurar el modelo"
fi

echo ""
echo "🧹 Limpiando archivos temporales..."
cd ../../
rm -rf "$TEMP_DIR"

echo ""
echo "🎉 Configuracion completada!"
echo ""
echo "Proximos pasos:"
echo "1. Si no lo has hecho, compila e instala la aplicación:"
echo "   ./gradlew installDebug"
echo ""
echo "2. Abre la aplicación en tu dispositivo"
echo ""
echo "3. Presiona 'Cargar Modelo' para inicializar Gemma 3n"
echo ""
echo "4. Una vez cargado, puedes comenzar a usar el asistente"
echo ""
echo "Notas importantes:"
echo "• El modelo requiere ~3 GB de RAM libre"
echo "• La primera carga puede tomar 30-60 segundos"
echo "• Cierra otras aplicaciones para mejor rendimiento"
echo ""
echo "Si tienes problemas, consulta el README.md para solución de problemas."
