# 📥 INSTRUCCIONES PARA DESCARGAR EL MODELO GEMMA 3N

## Error: "Archivo del modelo no encontrado"

Si ves este error en la app, necesitas descargar el modelo Gemma 3n E4B primero.

## 🚀 Opción 1: Script Automático (Recomendado)

### Para Linux/Mac:
```bash
chmod +x setup_model.sh
./setup_model.sh
```

### Para Windows:
```cmd
download_model.bat
```

## 🔧 Opción 2: Descarga Manual

1. **Ve a Hugging Face:**
   https://huggingface.co/google/gemma-3n-E4B-it

2. **Descarga el modelo:**
   - Busca el archivo `.task` (aproximadamente 4.4 GB)
   - Descárgalo a tu computadora

3. **Coloca el archivo en tu dispositivo:**
   
   ### Usando Android Studio:
   ```bash
   adb push gemma-3n-E4B-it.task /data/data/com.example.asistente/files/
   ```
   
   ### Usando dispositivo físico:
   - Copia el archivo a la carpeta Downloads del dispositivo
   - Usa un explorador de archivos para moverlo a la ubicación correcta

## ⚠️ Requisitos

- **Espacio libre:** Al menos 5 GB
- **Conexión a internet:** Estable para la descarga
- **Tiempo estimado:** 10-30 minutos dependiendo de tu conexión

## 🔍 Verificar la instalación

Una vez descargado, al presionar "Cargar Modelo" en la app debería cargar correctamente y mostrarte:
- ✅ Modelo Gemma 3n: Cargado
- Información del modelo con el tamaño en MB

## 🆘 Problemas comunes

### Error de espacio insuficiente:
- Libera espacio en el dispositivo (al menos 5 GB)

### Error de permisos:
- Asegúrate de que la app tenga permisos de almacenamiento

### Error de descarga:
- Verifica tu conexión a internet
- Intenta nuevamente después de unos minutos
