@echo off
REM Script para Windows - Descargar modelo Gemma 3n E4B

echo === Descarga del Modelo Gemma 3n E4B ===
echo.

REM Verificar si git está instalado
git --version >nul 2>&1
if errorlevel 1 (
    echo ❌ Git no está instalado. Descárgalo desde: https://git-scm.com/
    pause
    exit /b 1
)

REM Crear directorio temporal
set TEMP_DIR=temp_model_download
if not exist "%TEMP_DIR%" mkdir "%TEMP_DIR%"
cd "%TEMP_DIR%"

echo 📥 Descargando modelo Gemma 3n E4B desde Hugging Face...
echo    Tamaño aproximado: 4.4 GB
echo    Esto puede tomar varios minutos...
echo.

REM Clonar el repositorio del modelo
git clone https://huggingface.co/google/gemma-3n-E4B-it

if errorlevel 1 (
    echo ❌ Error al descargar el modelo
    pause
    exit /b 1
)

echo.
echo ✅ Modelo descargado exitosamente
echo 📁 Ubicación: %cd%\google\gemma-3n-E4B-it

REM Encontrar el archivo .task
for /r "google\gemma-3n-E4B-it" %%f in (*.task) do (
    echo 📄 Archivo del modelo encontrado: %%f
    echo.
    echo 📋 Próximos pasos:
    echo 1. Copia este archivo a la carpeta de tu app Android
    echo 2. O usa adb push para enviarlo al dispositivo
    echo.
)

echo 🎉 ¡Descarga completada!
pause
