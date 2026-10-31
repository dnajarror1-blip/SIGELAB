# SIGELAB en Windows

## Requisitos

- Windows 10/11 x64 y Git.
- JDK 21 (no solo JRE), `java` en PATH; si se define `JAVA_HOME`, debe apuntar al mismo JDK 21.
- Python 3.12 x64 recomendado, con Python Launcher (`py`) y venv. Se aceptan 3.12–3.14.
- Microsoft ODBC Driver 18 for SQL Server x64, instalado explícitamente por el usuario.
- SQL Server 2022; Docker Desktop iniciado si se usará el contenedor del proyecto.
- Webcam libre, permisos de cámara para aplicaciones de escritorio y sesión gráfica.
- DBeaver o SSMS para preparar SQL. IntelliJ es opcional.
- Internet para Maven Wrapper, dependencias Python y modelos faltantes. Coordinar los modelos/liveness con el responsable facial; ver [integración facial](FACIAL_INTEGRACION.md).

Los scripts no instalan Python, Java, Docker ni ODBC. No se necesita Maven global ni activar el venv. Los comandos siguientes parten de la raíz del proyecto. Desde otro directorio, invoque la ruta completa entre comillas con `&`, por ejemplo `& 'C:\Mis proyectos\SIGELAB\run-sigelab-windows.ps1'`.

## Primera ejecución

1. Clonar el repositorio o actualizarlo mediante Git/IntelliJ y abrir PowerShell en su raíz.
2. Iniciar Docker Desktop. Para una instalación nueva, copiar el ejemplo y editar **privadamente** `.env.docker`, definiendo una contraseña fuerte en `MSSQL_SA_PASSWORD` (no sobrescribir un archivo existente):

   ```powershell
   Copy-Item .env.docker.example .env.docker
   notepad .env.docker
   docker compose --env-file .env.docker up -d
   docker ps
   ```

   Esperar a que SQL acepte conexiones. El volumen conserva los datos; cambiar `.env.docker` no cambia la contraseña de una instancia ya creada. No borrar volúmenes para solucionar un fallo de conexión.
3. Conectar DBeaver/SSMS a host `127.0.0.1`, puerto `1433`, usuario configurado (por ejemplo `sa`), contraseña definida y base inicial **master**. En la instancia local con certificado autofirmado puede ser necesario confiar explícitamente en el certificado. Ejecutar **COMO SCRIPT**, con soporte para separadores `GO`, en este orden:

   1. `database/sigelab.sql`
   2. `database/seed/001_roles.sql`
   3. `database/seed/002_estudiante.sql`
   4. `database/seed/003_datos_demo.sql`

   Usar una instancia destinada a la demo. En una base existente, revisar estado/migraciones con el responsable; estos pasos no son una autorización para restablecer datos.
4. Preparar Python; reutiliza `.venv`, prefiere `py -3.12`, luego 3.13 y 3.14, e instala requirements solo si el diagnóstico lo requiere:

   ```powershell
   .\facial-service\setup-facial-windows.ps1
   ```

5. Configurar ambos `.env` interactivamente (contraseña SQL oculta). Usar las mismas credenciales SQL. Para el contenedor local autofirmado, confirmar `true` al confiar en su certificado, si corresponde:

   ```powershell
   .\facial-service\configure-local-windows.ps1
   ```

   Conserva claves existentes. Solo confirmar `SIN PERFILES` si la base realmente no tiene perfiles. Si existen perfiles, recuperar su clave AES original. Windows usa las ACL del directorio: el configurador no aplica permisos POSIX ni modifica ACL. Guardar el proyecto en un directorio privado. Las variables del entorno tienen prioridad sobre los archivos; evitar valores antiguos o duplicados.
6. Comprobar dependencias, configuración, ODBC, SQL/esquema (solo SELECT), modelo y cámara:

   ```powershell
   .\facial-service\prepare-demo-windows.ps1
   ```

   Debe terminar exactamente con `READY FOR DEMO`. Puede descargar modelos faltantes; no registra rostros ni acredita identidad/calibración.
7. Iniciar FastAPI en primer plano y mantener la terminal abierta:

   ```powershell
   .\facial-service\start-facial-windows.ps1
   ```

   Usa `127.0.0.1:8765`, un worker y sin reload. Ctrl+C detiene el servicio.
8. En **otra terminal**, solo si el ADMIN activo existente no tiene perfil, ejecutar una vez y completar captura/liveness:

   ```powershell
   .\facial-service\enroll-user-windows.ps1 -Username admin.demo
   ```

   También admite `-Id 123` como alternativa a `-Username` (usar un ID real). No crea usuarios, no reemplaza perfiles ni abre sesión. No repetir automáticamente ante un error.
9. Ejecutar Java desde esa segunda terminal:

   ```powershell
   .\run-sigelab-windows.ps1
   ```

   Valida Java 21, usa `mvnw.cmd`, compila incrementalmente sin pruebas, construye el classpath runtime con `;` y mantiene la raíz como directorio de trabajo.
10. Login con `admin.demo`, la contraseña de esa cuenta y el rostro registrado. En una base nueva el seed documenta las credenciales demo en el README; no restablece la contraseña de cuentas existentes. FaceID sigue siendo obligatorio.

Cada script tiene un wrapper `.cmd` con el mismo nombre; por ejemplo `facial-service\enroll-user-windows.cmd -Username admin.demo`. Solo invoca PowerShell y devuelve su código de salida. Para ver mensajes, ejecutarlos desde una terminal abierta.

## Problemas comunes

| Problema | Qué revisar |
|---|---|
| ODBC Driver 18 no encontrado | Instalar manualmente Microsoft ODBC Driver 18 for SQL Server x64 y repetir `prepare-demo-windows.ps1`. Doctor lo detecta con pyodbc. |
| Cámara ocupada | Cerrar Teams, Zoom, Camera y otros procesos que usen la webcam. No ejecutar prepare mientras otra captura está activa. |
| Permiso de cámara | En Configuración → Privacidad (y seguridad) → Cámara, permitir cámara y acceso a aplicaciones de escritorio, respetando las políticas del equipo. |
| Cámara 0 incorrecta | Ajustar privadamente `FACIAL_CAMERA_INDEX` en `facial-service/.env`. Mantener `FACIAL_CAMERA_BACKEND=AUTO`; Windows intenta DirectShow y después CAP_ANY. Doctor muestra el backend abierto. |
| Puerto 1433 ocupado | Revisar si hay otra instancia SQL/contenedor. Coordinar el puerto y actualizar la configuración correspondiente; no detener procesos ajenos ni borrar datos. |
| Docker apagado | Abrir Docker Desktop, esperar al motor y repetir Compose y `docker ps`. |
| `FACIAL_ENCRYPTION_KEY` perdida | Recuperar la clave original del respaldo privado. Generar otra no permite descifrar perfiles existentes. |
| Perfil ya existente | No repetir enrollment ni reemplazarlo automáticamente. Iniciar sesión con el perfil existente; coordinar cualquier recuperación con el administrador. |
| FastAPI 8765 ocupado | Revisar la terminal del servicio previo y cerrarlo con Ctrl+C si corresponde. No iniciar dos servidores ni usar reload. |
| Java no encuentra `.env` | Usar `run-sigelab-windows.ps1`; en IntelliJ configurar working directory a la raíz del proyecto. Revisar también variables de entorno antiguas. |
| Python incorrecto | Comprobar `py --list`. Instalar 3.12 x64 con Launcher. Un `.venv` incompatible o creado en Linux no se sobrescribe: revisarlo y recrearlo manualmente para Windows. |
| Fallo de pip/modelos | Revisar acceso a Internet y disponibilidad de las versiones de requirements para ese Python; preferir 3.12. No cambiar versiones, thresholds ni modelos para omitir diagnósticos. |

Si PowerShell bloquea scripts, revisar `Get-ExecutionPolicy -List` y respetar las políticas del equipo. Si están permitidos los scripts locales, puede habilitarlos **solo para la sesión actual** antes de ejecutarlos:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy RemoteSigned
```

Se pierde al cerrar la terminal, no cambia la política permanente y no prevalece sobre políticas de organización. Los `.cmd` tampoco eluden estas políticas. Si un archivo está marcado como descargado o existe una política administrada, coordinar con el responsable del equipo.

## Seguridad

Nunca subir `.env`, `.env.docker`, `facial-service/.env`, `.venv`, claves, contraseñas ni embeddings/exportaciones privadas. No pegar secretos en comandos, tickets o capturas. Los scripts no imprimen las variables privadas ni ejecutan SQL de inicialización automáticamente.

El arranque Windows requiere validación real en Windows: PowerShell 5.1/7, rutas con espacios, instalación de wheels, ODBC/SQL, DirectShow/fallback, captura/liveness y login Swing. Las pruebas aisladas en Linux no sustituyen esa validación.
