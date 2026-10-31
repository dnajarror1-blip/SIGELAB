# SIGELAB en Linux

Guía oficial de configuración, verificación de dependencias, diagnóstico de estados y ejecución en entornos Linux (Ubuntu, Debian, Linux Mint, Arch Linux y Fedora).

---

## 1. Requisitos del Sistema y Dependencias

Para ejecutar SIGELAB con todos sus módulos (Java Swing, Base de Datos SQL Server 2022 y Reconocimiento Facial Python con OpenCV/InsightFace), el sistema requiere los siguientes componentes:

| Componente | Versión requerida | Paquete del sistema (Debian/Ubuntu/Mint) | Paquete (Arch Linux) | Paquete (Fedora) |
|---|:---:|---|---|---|
| **Java JDK** | **Java 21 LTS** | `openjdk-21-jdk` (o Temurin 21) | `jdk21-openjdk` | `java-21-openjdk-devel` |
| **Docker Engine** | 20.10+ | `docker.io` + `docker-compose-v2` | `docker` + `docker-compose` | `moby-engine` + `docker-compose` |
| **Python** | 3.12 – 3.14 | `python3` + `python3-venv` + `python3-pip` | `python` | `python3` + `python3-pip` |
| **Driver ODBC** | Driver 18 for SQL Server | `msodbcsql18` + `unixodbc-dev` | `msodbcsql18` (AUR) | `msodbcsql18` + `unixODBC-devel` |
| **Librerías C/Gráficas** | OpenCV / OpenGL | `libgl1` + `libglib2.0-0` | `libglvnd` | `libglvnd-glx` |
| **Webcam / V4L2** | Video4Linux2 | `v4l-utils` | `v4l-utils` | `v4l-utils` |

---

## 2. Verificación de Dependencias (Comprobación por nombres)

Ejecute los siguientes comandos en su terminal para identificar qué componentes están instalados y cuáles faltan:

### A. Verificar Java 21
```bash
# Comprobar versión de Java
java -version 2>&1 | head -n 1
```
- **Esperado:** `openjdk version "21.x.x"` o `java version "21.x.x"`.
- **Si falta o no está en PATH:**
  - Instalar: `sudo apt install -y openjdk-21-jdk`
  - O si tiene JDK portable en `$HOME/bin/jdk-21`:
    ```bash
    export JAVA_HOME="$HOME/bin/jdk-21"
    export PATH="$JAVA_HOME/bin:$PATH"
    ```

### B. Verificar Docker y Contenedores
```bash
# Verificar Docker CLI
docker --version
docker compose version

# Verificar si el usuario tiene permiso para Docker sin sudo
docker ps
```
- **Si dice "Permission denied":**
  ```bash
  sudo usermod -aG docker $USER
  # Aplicar grupo a la sesión actual:
  newgrp docker
  ```

### C. Verificar Driver ODBC 18 (para Python y SQL Server)
```bash
odbcinst -j
odbcinst -q -d -n "ODBC Driver 18 for SQL Server"
```
- **Si no encuentra el driver:** Instale el repositorio oficial de Microsoft:
  ```bash
  curl https://packages.microsoft.com/keys/microsoft.asc | sudo gpg --dearmor -o /etc/apt/trusted.gpg.d/microsoft.gpg
  echo "deb [arch=amd64] https://packages.microsoft.com/ubuntu/$(lsb_release -rs)/prod $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/mssql-release.list
  sudo apt update
  sudo ACCEPT_EULA=Y apt install -y msodbcsql18 unixodbc-dev
  ```

### D. Verificar Cámara Web (V4L2)
```bash
# Listar cámaras disponibles
ls -l /dev/video*
# O detallado con v4l-utils:
v4l2-ctl --list-devices
```
- **Permisos de cámara:** Si `/dev/video0` pertenece al grupo `video`, asegúrese de que su usuario sea miembro:
  ```bash
  sudo usermod -aG video $USER
  ```

### E. Verificar Librerías del Sistema para OpenCV
Python/OpenCV requiere `libgl1` y `libglib2.0-0` para renderizar ventanas de captura y procesar imágenes:
```bash
dpkg -l | grep -E "libgl1|libglib2.0-0"
# Si faltan:
sudo apt install -y libgl1 libglib2.0-0
```

---

## 3. Diagnóstico de Estados del Sistema

Antes de iniciar la interfaz, compruebe el estado de los servicios:

| Servicio | Comando de verificación | Estado esperado | Acción si está detenido |
|---|---|---|---|
| **SQL Server** | `docker ps -f name=sigelab-sqlserver` | `Up ... (healthy)` | `docker start sigelab-sqlserver` |
| **Puerto SQL** | `nc -zv 127.0.0.1 1433` | `Connection to 127.0.0.1 1433 port [tcp] succeeded!` | Esperar 5-10 s a que inicialice |
| **FastAPI Facial** | `curl -s http://127.0.0.1:8765/health` | `{"status":"ok",...}` | `cd facial-service && bash start-facial.sh` |
| **Perfil Admin** | Consulta SQL en tabla `PerfilFacial` | 1 fila activa | Ejecutar `bash facial-service/enroll-user.sh --username admin.demo` |

### Comprobar conexión y datos demo en SQL Server:
```bash
docker exec sigelab-sqlserver /opt/mssql-tools18/bin/sqlcmd \
  -S localhost -U sa -P 'Dev2025!' -C -d sigelab \
  -Q "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE='BASE TABLE';"
```
Debe listar las **14 tablas relacionales**: `Auditoria`, `AuditoriaFacial`, `DetallePrestamo`, `Devolucion`, `Equipo`, `Laboratorio`, `Mantenimiento`, `Notificacion`, `PerfilFacial`, `Prestamo`, `Reserva`, `Rol`, `TipoEquipo`, `Usuario`.

---

## 4. Ejecución de SIGELAB mediante Script (`run-sigelab-linux.sh`)

El proyecto incluye el script ejecutable `run-sigelab-linux.sh` en la raíz del repositorio, equivalente a `run-sigelab-windows.ps1`.

### Características del script:
1. **Detección inteligente de Java 21:** Revisa `$JAVA_HOME`, el comando `java` en `$PATH`, `$HOME/bin/jdk-21` y rutas estándar del sistema `/usr/lib/jvm/`.
2. **Generación automática de Classpath:** Si no existe `target/runtime-classpath-linux.txt`, lo compila y construye automáticamente con Maven Wrapper.
3. **Validación de entorno:** Verifica la presencia de `.env` y los permisos del ejecutable.
4. **Inicio seguro:** Ejecuta `sigelab.Main` con el separador nativo de Linux (`:`).

### Modo de uso:
```bash
# 1. Dar permisos de ejecución (solo la primera vez)
chmod +x run-sigelab-linux.sh

# 2. Ejecutar
./run-sigelab-linux.sh
```

---

## 5. Guía Completa de Primera Ejecución (Paso a Paso)

Siga este orden para poner el sistema en marcha desde cero en Linux:

### Paso 1: Iniciar la Base de Datos con Docker
```bash
# Si es la primera vez, crear el archivo de variables para Docker:
cp .env.docker.example .env.docker
# Iniciar contenedor
docker compose --env-file .env.docker up -d
# Verificar que esté corriendo
docker ps
```

### Paso 2: Ejecutar los Scripts SQL (solo si la BD es nueva)
En DBeaver, SSMS o por terminal mediante `sqlcmd`, ejecute en orden con delimitador `GO`:
1. `database/sigelab.sql` (Estructura de tablas, constraints y triggers)
2. `database/seed/001_roles.sql` (Roles ADMIN, ENCARGADO)
3. `database/seed/002_estudiante.sql` (Rol ESTUDIANTE)
4. `database/seed/003_datos_demo.sql` (Usuarios demo, laboratorios y equipos demo)

### Paso 3: Configurar el Entorno Local
```bash
# Copiar variables de Java si no existen
cp -n .env.example .env
```
Verifique en `.env` que `DB_PASSWORD` coincida con la contraseña definida para `sa`.

### Paso 4: Preparar e Iniciar el Microservicio Facial
```bash
cd facial-service

# 1. Configurar claves privadas interactivamente (solo primera vez)
python3 scripts/configure_local.py

# 2. Preparar entorno virtual y modelos
bash prepare-demo-linux.sh

# 3. Iniciar el servicio FastAPI (dejar esta terminal abierta)
bash start-facial.sh
```

### Paso 5: Enrolar el Rostro del Administrador Demo
En **otra terminal**, ejecute una sola vez ante la cámara:
```bash
cd /home/ale/SIGELAB/facial-service
bash enroll-user.sh --username admin.demo
```
Siga las instrucciones en pantalla para registrar las muestras biométricas y la prueba de vida (*liveness*).

### Paso 6: Iniciar SIGELAB (Java Swing)
En una terminal en la raíz del proyecto:
```bash
cd /home/ale/SIGELAB
./run-sigelab-linux.sh
```

### Paso 7: Iniciar Sesión en la Interfaz
- **Usuario:** `admin.demo`
- **Contraseña:** `SigelabDemo2026!`
- Mire a la cámara para la confirmación de FaceID. El sistema validará credenciales, abrirá el menú según el rol de Administrador y registrará el evento de auditoría `LOGIN_EXITOSO`.

---

## 6. Tabla de Problemas Comunes en Linux y Soluciones

| Síntoma / Mensaje de Error | Causa Raíz | Solución Inmediata |
|---|---|---|
| **"Error: No se encontró Java 21 en el sistema"** | JDK 21 no está en PATH ni en JAVA_HOME. | Instalar `sudo apt install openjdk-21-jdk` o exportar `export JAVA_HOME=$HOME/bin/jdk-21; export PATH=$JAVA_HOME/bin:$PATH`. |
| **"La verificación facial no está disponible. Acceso denegado."** | El microservicio FastAPI en el puerto 8765 está apagado o no responde. | Iniciar el servicio en otra terminal: `cd facial-service && bash start-facial.sh`. |
| **"Debe registrar un rostro activo con un administrador."** | El usuario no tiene un embedding registrado en `PerfilFacial`. | Registrar rostro con: `cd facial-service && bash enroll-user.sh --username <usuario>`. |
| **"No se pudo conectar con la base de datos"** | El contenedor de SQL Server está detenido o el puerto 1433 no está listo. | Ejecutar `docker start sigelab-sqlserver` y esperar 5 segundos. |
| **`Login timeout expired` / `TCP Provider: Error code 0x2749`** | SQL Server está arrancando o la IP/puerto en `.env` es incorrecto. | Comprobar que `DB_HOST=127.0.0.1` y `DB_PORT=1433` en `.env`. |
| **`Permission denied: '/dev/video0'`** | El usuario no tiene permisos sobre la cámara web. | Agregar usuario al grupo de video: `sudo usermod -aG video $USER` y reiniciar sesión. |
| **`ImportError: libGL.so.1: cannot open shared object file`** | Falta la biblioteca OpenGL nativa para OpenCV. | Instalar paquete del sistema: `sudo apt install -y libgl1 libglib2.0-0`. |
| **`Can't open lib 'ODBC Driver 18 for SQL Server'`** | unixODBC no tiene instalado o configurado el driver de Microsoft. | Instalar paquete `msodbcsql18` según la sección 2.C de esta guía. |
| **Ventana Swing con fuentes borrosas o pequeñas** | Escalado HiDPI de Linux / Wayland. | Ejecutar con bandera de escalado: `java -Dsun.java2d.uiScale=1.5 -cp ... sigelab.Main`. |
