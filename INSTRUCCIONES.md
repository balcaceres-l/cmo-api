# 🏥 Guía de Despliegue e Instrucciones de Uso: Backend & Android

Este documento detalla paso a paso las instrucciones necesarias para levantar el backend en **PHP / Laravel** y posteriormente compilar, probar y ejecutar la aplicación móvil en **Android (Kotlin / Jetpack Compose)**.

---

## 📋 Requisitos Previos

Asegúrate de contar con las siguientes herramientas instaladas en tu sistema:

1. **Backend**:
   - **PHP >= 8.2** (con extensiones `pdo_mysql`, `mbstring`, `openssl`, `bcmath`, `curl`).
   - **Composer** (gestor de dependencias de PHP).
   - **MariaDB** o **MySQL** (ejecutándose en el puerto estándar `3306`).

2. **Android**:
   - **JDK 21 LTS** (por ejemplo, *Eclipse Adoptium Temurin JDK 21* o el OpenJDK incluido en Android Studio).
   - **Android SDK** (API Level 26 a 34).
   - **Android Studio** (versión Ladybug / Iguana o superior) o terminal con Android Build Tools.

---

## 🚀 Parte 1: Configuración y Ejecución del Backend (Laravel)

### 1. Entrar a la carpeta del backend
Abre una terminal y dirígete al directorio `backend`:

```bash
cd backend
```

### 2. Instalar dependencias de PHP
Ejecuta Composer para instalar las librerías del proyecto:

```bash
composer install
```

### 3. Configurar variables de entorno (`.env`)
Copia la plantilla de entorno y genera la clave de cifrado de la aplicación:

```bash
# En Windows (PowerShell / CMD):
copy .env.example .env

# En Linux / macOS / Git Bash:
cp .env.example .env
```

Genera la llave de la aplicación:

```bash
php artisan key:generate
```

### 4. Configurar la Base de Datos
Abre el archivo `.env` en tu editor de texto y verifica o ajusta las credenciales de MariaDB/MySQL:

```env
DB_CONNECTION=mysql
DB_HOST=127.0.0.1
DB_PORT=3306
DB_DATABASE=hospital_universitario
DB_USERNAME=root
DB_PASSWORD=
```

> **Nota:** Crea la base de datos `hospital_universitario` en tu gestor de base de datos (por ejemplo, en phpMyAdmin, DBeaver o en consola MySQL con `CREATE DATABASE hospital_universitario;`).

### 5. Ejecutar migraciones y seeders
Crea las tablas y carga los datos de prueba (usuarios médicos, enfermeros, consultorios, camas, catálogo de laboratorios y ambulancia):

```bash
php artisan migrate:fresh --seed
```

### 6. Levantar el servidor de desarrollo
Inicia el servidor local de Laravel. Se recomienda enlazarlo a `0.0.0.0` para que los emuladores Android (`10.0.2.2`) o dispositivos móviles físicos en tu red Wi-Fi puedan acceder:

```bash
php artisan serve --host=0.0.0.0 --port=8000
```

El servidor estará accesible en:
- Desde tu PC: `http://localhost:8000/api/v1`
- Desde el Emulador Android oficial: `http://10.0.2.2:8000/api/v1`
- Desde un dispositivo móvil físico en la misma red Wi-Fi: `http://<IP_LOCAL_DE_TU_PC>:8000/api/v1`

*(Opcional)* Si deseas habilitar WebSockets con Laravel Reverb en una segunda terminal:
```bash
php artisan reverb:start
```

---

### 🔑 Credenciales Demo del Backend

El seeder inicial genera usuarios con contraseña compartida: **`Hospital2026!`**

| Usuario | Contraseña | Rol | Cargo | Acceso a Android |
| :--- | :--- | :--- | :--- | :---: |
| `medico.general` | `Hospital2026!` | `MEDICO` | Medicina general | ✅ Permitido |
| `medico.emergencia` | `Hospital2026!` | `MEDICO` | Emergencia | ✅ Permitido |
| `medico.alergista` | `Hospital2026!` | `MEDICO` | Alergista | ✅ Permitido |
| `recepcion` | `Hospital2026!` | `RECEPCIONISTA` | Recepcionista | ✅ Permitido |
| `enfermeria` | `Hospital2026!` | `RECEPCIONISTA` | Enfermera | ✅ Permitido |
| `admin` | `Hospital2026!` | `ADMINISTRADOR` | Administración | ⛔ Bloqueado (solo Web) |
| `paciente.uno` | `Hospital2026!` | `PACIENTE` | Paciente | ⛔ Bloqueado (solo iOS/Web) |

---

## 📱 Parte 2: Comandos y Ejecución de la App Android (Kotlin)

### 1. Entrar a la carpeta del proyecto Android
Desde la raíz del repositorio, navega a la carpeta `android`:

```bash
cd android
```

### 2. Configurar el SDK de Android (`local.properties`)
Si no existe el archivo `local.properties` en la carpeta `android/`, créalo indicando la ruta hacia tu Android SDK:

**En Windows:**
```properties
sdk.dir=C:\\Users\\<TU_USUARIO>\\AppData\\Local\\Android\\Sdk
```

**En macOS:**
```properties
sdk.dir=/Users/<TU_USUARIO>/Library/Android/sdk
```

**En Linux:**
```properties
sdk.dir=/home/<TU_USUARIO>/Android/Sdk
```

### 3. Verificar que JAVA_HOME apunte a JDK 21
Para evitar errores de compatibilidad con Kotlin 1.9+ y Gradle 8.9+, confirma que tu entorno utilice Java 21:

```bash
# Windows PowerShell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.x-hotspot"

# Linux / macOS
export JAVA_HOME=/path/to/jdk-21
```

### 4. Ejecutar las Pruebas Unitarias de Lógica de Negocio
Ejecuta la suite completa de pruebas unitarias (validación de semáforo de 30m, formato de expediente `LB-2026-001`, catálogo de especialidades en cascada, bloqueo de solapamiento horario y triaje flexible):

**En Windows (PowerShell / CMD):**
```powershell
.\gradlew.bat testDebugUnitTest
```
*(O si ejecutas directamente con el jar del wrapper y JDK 21):*
```powershell
& "$env:JAVA_HOME\bin\java.exe" -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain testDebugUnitTest
```

**En Linux / macOS / Git Bash:**
```bash
./gradlew testDebugUnitTest
```

> **Resultado Esperado:** `BUILD SUCCESSFUL (11 tests completed, 0 failed)`

### 5. Compilar el APK de Depuración (Debug APK)
Para compilar y empaquetar la aplicación Android:

**En Windows:**
```powershell
.\gradlew.bat assembleDebug
```

**En Linux / macOS:**
```bash
./gradlew assembleDebug
```

El binario ejecutable generado se encontrará en:
```
android/app/build/outputs/apk/debug/app-debug.apk
```

### 6. Instalar y Ejecutar en Emulador o Dispositivo Físico Conectado
Asegúrate de tener un emulador Android abierto o un teléfono conectado por USB con Depuración activada (`adb devices`), y ejecuta:

```bash
# Windows
.\gradlew.bat installDebug

# Linux / macOS
./gradlew installDebug
```

O abre la aplicación en el dispositivo con `adb`:
```bash
adb shell am start -n com.cmo.hospital/.MainActivity
```

---

## 💻 Opción Recomendada con Interfaz Gráfica: Android Studio

1. Abre **Android Studio**.
2. Haz clic en **Open** (o **File > Open**) y selecciona directamente la carpeta `cmo-api/android`.
3. Espera a que termine la sincronización inicial de Gradle (**Gradle Sync**).
4. Selecciona tu emulador o dispositivo físico en la barra de herramientas superior.
5. Haz clic en el botón verde **Run 'app'** (o presiona `Shift + F10`).

---

## 🧪 Pruebas de Flujo Clínico en la App

1. **Inicio de Sesión y Control de Acceso por ROL**:
   - Inicia sesión con `medico.general` para entrar al portal médico.
   - Cierra sesión y entra con `recepcion` o `enfermeria` para acceder al portal de admisiones y triaje.
   - Prueba ingresar con `admin` o `paciente.uno` y observa cómo el sistema deniega el acceso indicando que la app móvil está reservada para personal en punto de atención.
2. **Agendamiento en Cascada (Recepción)**:
   - Selecciona una especialidad (ej. *Cardiología*).
   - Observa cómo la lista de doctores se actualiza mostrando únicamente especialistas de esa rama.
   - Selecciona fecha y bloque horario de 30 minutos (los horarios ocupados se bloquean automáticamente).
3. **Triaje y Pase a Consulta**:
   - En la lista de citas, pulsa `🩺 Triaje / Signos` para registrar opcionalmente los signos vitales del paciente con cálculo automático de IMC.
   - Pulsa el botón destacado **`🚀 Pasar a Consulta`**: el paciente pasará a estado `EN_ATENCION` y aparecerá inmediatamente en la pantalla del doctor con el cronómetro de 30 minutos iniciado en verde.
4. **Laboratorio LIS y Auto-Agendamiento (Doctor)**:
   - El doctor puede indicar exámenes de laboratorio (Troponina, Hemograma, Glucosa, etc.) y cambiar interactivamente su estado (*En Espera*, *Agendado*, *En Proceso*, *Realizado*, *Cancelado*).
   - El doctor puede agendar citas de control para sí mismo verificando su propia disponibilidad sin permitir empalmes de horario.
