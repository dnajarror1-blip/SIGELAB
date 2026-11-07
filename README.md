# SIGELAB

Sistema de Gestión y Control de Laboratorios Universitarios. Proyecto final de Programación II, Universidad Mariano Gálvez de Guatemala.

## Estado y funciones

Están implementados e integrados en Java: login con contraseña y FaceID obligatorio, usuarios y permisos, laboratorios, tipos y equipos, QR desde imágenes, préstamos, devoluciones parciales/completas, mantenimiento, dashboard, reportes, auditoría y administración facial. Reservas es una extensión presente, no un requisito oficial; su integración con Préstamos queda fuera de esta preparación.

La validación Java aislada no acredita SQL Server ni biometría real. Consultar [matriz de aceptación](docs/MATRIZ_PRUEBAS.md), [manual técnico](docs/MANUAL_TECNICO.md) y [manual de usuario](docs/MANUAL_USUARIO.md).

## Integrantes y responsabilidades

La información compartida identifica integrantes por número, sin nombres personales:

| Integrante | Responsabilidad |
|---|---|
| #1 | Fundación, integración, base de datos, modelos, enums y JDBC |
| #2 | Autenticación, usuarios y notificaciones |
| #3 | Laboratorios, tipos y equipos |
| #4 | Préstamos |
| #5 | Devoluciones y reservas |
| #6 | Mantenimiento y auditoría |
| #7 | QR, dashboard y reportes |
| #8 | Reconocimiento facial |

Detalle: [RESPONSABILIDADES](docs/RESPONSABILIDADES.md) y [contrato](docs/TEAM_CONTRACT.md).

## Requisitos y dependencias externas

- JDK 21, Maven Wrapper, entorno gráfico para Swing.
- SQL Server 2022 (instancia propia o Docker Compose).
- Python **3.12–3.14**, venv, Microsoft **ODBC Driver 18 for SQL Server** y unixODBC en Linux.
- Webcam disponible, sesión gráfica y permisos de cámara; modelos InsightFace/ArcFace (`buffalo_l`) y complemento liveness compatible, coordinados con #8.
- FastAPI/Uvicorn, OpenCV, InsightFace, ONNX Runtime, pyodbc y cryptography según `facial-service/requirements*.txt`.
- Java utiliza JDBC Microsoft, jBCrypt, ZXing, Gson y FlatLaf según `pom.xml`. La preparación necesita acceso a los repositorios de dependencias y modelos.


## Arranque rápido en Windows

Ver [guía Windows](docs/GUIA_WINDOWS.md) para requisitos, Docker y preparación SQL previa.

```powershell
.\facial-service\setup-facial-windows.ps1
.\facial-service\configure-local-windows.ps1
.\facial-service\prepare-demo-windows.ps1
.\facial-service\start-facial-windows.ps1
```

En otra terminal, enrollment solo si el ADMIN existente aún no tiene perfil; después ejecutar Java:

```powershell
.\facial-service\enroll-user-windows.ps1 -Username admin.demo
.\run-sigelab-windows.ps1
```

## Arranque rápido en Linux

Ver [guía Linux](docs/GUIA_LINUX.md) para comprobación de paquetes, permisos V4L2 y diagnósticos.

```bash
docker start sigelab-sqlserver
./run-sigelab-linux.sh
```


## Primera ejecución

Sobre esta base SIGELAB existente, sin recrearla:

1. Si faltan datos demo, ejecutar `database/seed/003_datos_demo.sql` en DBeaver/SSMS; antes ejecutar 001/002 solo si faltan los roles. No restablece estados ni contraseñas existentes.
2. Desde la raíz: `python3 facial-service/scripts/configure_local.py`. Pide contraseña oculta; conserva claves existentes. Solo genera AES nueva si se confirma que no hay perfiles. Si hay perfiles y falta la clave, recuperar la original.
3. `bash facial-service/prepare-demo-linux.sh`: prepara venv y dependencias; verifica ODBC, configuración, SQL con SELECT, modelo y cámara. Puede descargar modelos al inicializar el motor. Termina `READY FOR DEMO` solo si esos diagnósticos pasan; no hace enrollment ni acredita biometría.
4. `bash facial-service/start-facial.sh` (mantener terminal abierta).
5. En otra terminal: `bash facial-service/enroll-user.sh --username admin.demo`. Solo si aún no tiene perfil. El bootstrap no crea usuarios y no abre una sesión.
6. En IntelliJ, JDK 21, directorio de ejecución raíz del proyecto, ejecutar `sigelab.Main`. No copiar variables a IntelliJ; `.env` raíz se carga automáticamente. Para consola ver el comando de classpath más abajo.
7. Login `admin.demo` / `SigelabDemo2026!` (solo si el seed creó esa cuenta con su contraseña demo) y rostro registrado. Sin perfil se solicita registrar rostro; servicio apagado o rostro inválido deniega acceso.

Python acepta 3.12, 3.13 y 3.14; `PYTHON_BIN=python3.14 bash facial-service/prepare-demo-linux.sh` selecciona un intérprete al crear venv. Un venv existente se conserva. El configurador genera archivos 0600, no muestra claves y no consulta ni modifica la BD. Entorno real > `.env` raíz > `.env` facial; evitar duplicar variables compartidas. No ejecutar estos archivos con `source`.

## Arranque desde una base limpia (orden obligatorio)

### 1. Crear la BD y ejecutar seeds

Para SQL Server local con Docker, copiar `.env.docker.example` a `.env.docker`, definir la contraseña local e iniciar:

```sh
docker compose --env-file .env.docker up -d
```

Conectar DBeaver/SSMS a `127.0.0.1:1433`, inicialmente a `master`, con las credenciales de esa instancia. Ejecutar **como scripts**, admitiendo los separadores `GO`, exactamente:

1. `database/sigelab.sql` (crea SIGELAB y las 14 tablas).
2. `database/seed/001_roles.sql`.
3. `database/seed/002_estudiante.sql`.
4. `database/seed/003_datos_demo.sql`.

En una instalación existente, revisar las migraciones aplicables; el esquema inicial solo crea tablas faltantes, no actualiza tablas existentes automáticamente. El seed demo inserta faltantes y no restablece contraseñas, roles ni estados previamente modificados.

### 2. Comprobar usuarios DEMO existentes

| Username | Contraseña exclusivamente DEMO | Rol | Estado inicial |
|---|---|---|---|
| `admin.demo` | `SigelabDemo2026!` | ADMIN | ACTIVO |
| `solicitante.demo` | `SigelabDemo2026!` | ESTUDIANTE | ACTIVO |

La BD guarda únicamente un hash BCrypt válido (coste 10). Estas credenciales públicas son para una base de demostración. El solicitante sirve para registrar préstamos desde la sesión ADMIN; su propio login requiere enrollment posterior desde Usuarios.

Se crean `LAB-DEMO`, tipo `Portátil demo` y `DEMO-EQ-001` a `DEMO-EQ-004`, inicialmente DISPONIBLES. Utilizar los primeros tres para CP-14. No se inserta ningún PerfilFacial. Los IDs se consultan, nunca se suponen.

### 3. Configurar Python 3.12–3.14 y FaceID

Instalar el driver ODBC 18, dependencias nativas y preparar el entorno desde la raíz:

```sh
python3 facial-service/scripts/configure_local.py
bash facial-service/prepare-demo-linux.sh
```

Usar `python3 facial-service/scripts/configure_local.py`: guarda las variables compartidas en `.env` raíz y las exclusivas de Python en `facial-service/.env`. Las tablas siguientes describen ambas fuentes:

| Variables | Uso |
|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME=SIGELAB`, `DB_USER`, `DB_PASSWORD` | Misma base Java/Python |
| `DB_DRIVER=ODBC Driver 18 for SQL Server` | Driver instalado |
| `DB_ENCRYPT`, `DB_TRUST_SERVER_CERTIFICATE` | Política TLS de la instancia |
| `FACIAL_API_KEY` | Clave HTTP aleatoria de al menos 32 caracteres, compartida con Java |
| `FACIAL_ENCRYPTION_KEY` | Base64 de 32 bytes aleatorios para AES-GCM; conservar tras enrollment |
| `FACIAL_MATCH_THRESHOLD` | Inicial 0.50 solo al faltar; requiere calibración por #8, en (0,1] |
| `FACIAL_LIVENESS_THRESHOLD` | Inicial 0.80, validar con cámara real |
| `FACIAL_MODEL` | `buffalo_l` |
| `FACIAL_CAMERA_INDEX`, `FACIAL_CAMERA_BACKEND` | Iniciales 0 y AUTO |
| `FACIAL_CAPTURE_TIMEOUT` | Inicial 120 segundos |

No compartir la clave AES con Java. Las claves HTTP y AES son distintas. Preparar modelos locales según [integración facial](docs/FACIAL_INTEGRACION.md). El setup acepta Python 3.12–3.14 y comprueba las dependencias instaladas sin cambiar sus versiones.

### 4. Iniciar FastAPI

```sh
bash facial-service/start-facial.sh
```

Equivale a ejecutar, desde `facial-service`, `.venv/bin/python -m uvicorn app.main:app --host 127.0.0.1 --port 8765 --workers 1`. Mantenerlo abierto, sin `--reload` y con un solo worker.

Windows: desde `facial-service`, crear `.venv` con `py -3.12 -m venv .venv`, instalar `requirements-dev.txt` con `.venv\Scripts\python -m pip install -r requirements-dev.txt` e iniciar Uvicorn con ese intérprete y los mismos argumentos.

### 5. Enrollment del ADMIN demo ya existente

En otra terminal, desde la raíz (Linux):

```sh
cd facial-service
.venv/bin/python scripts/enroll_user.py --username admin.demo
```

En Windows usar `.venv\Scripts\python scripts\enroll_user.py --username admin.demo`. Completar la captura y liveness. **El bootstrap no crea usuarios**: resuelve un ADMIN ACTIVO ya creado por SQL, y solicita su perfil mediante la API. No emplear scripts de TEST_FACE para SIGELAB ni repetir enrollment en cada login.

### 6. Comprobar `/face/status/{id}`

Usar el ID informado por enrollment o consultarlo con `SELECT id_usuario FROM SIGELAB.dbo.Usuario WHERE username=N'admin.demo';`. Consultar `GET http://127.0.0.1:8765/face/status/{id}` con cabecera `X-Facial-Api-Key` configurada privadamente en el cliente HTTP. Debe indicar usuario existente/activo y perfil registrado/activo. No copiar claves en tickets o capturas. `/health` no acredita cámara ni liveness.

### 7. Configurar e iniciar Java

En la configuración de ejecución de IntelliJ: JDK 21, clase principal `sigelab.Main` y directorio raíz. El configurador ya deja estas variables en `.env`; solo definirlas en el proceso si se desea sobrescribirlas:

- `DB_HOST=127.0.0.1`, `DB_PORT=1433`, `DB_NAME=SIGELAB`.
- `DB_USER`, `DB_PASSWORD` de la instancia local.
- `DB_ENCRYPT=true`, `DB_TRUST_SERVER_CERTIFICATE` según TLS (true solo para desarrollo con certificado local no confiable).
- `FACIAL_API_URL=http://127.0.0.1:8765`, `FACIAL_API_KEY` compartida con Python.

Cargar Maven y ejecutar `sigelab.Main` desde IntelliJ. Java carga `.env` de la raíz si se ejecuta desde ella; las variables reales del proceso tienen prioridad. En consola se puede compilar y preparar el classpath con:

```sh
./mvnw -DskipTests compile dependency:build-classpath -Dmdep.outputFile=target/runtime-classpath.txt
java -cp "target/classes:$(cat target/runtime-classpath.txt)" sigelab.Main
```

En Windows el separador del classpath es `;`. Ejecutar desde la raíz para cargar `.env`; las variables exportadas tienen prioridad.

### 8. Probar login

Ingresar `admin.demo` y su contraseña DEMO, completar verificación facial. Se revalida usuario/rol y se persiste LOGIN_EXITOSO antes de abrir el menú. Fallos de credenciales o rostro deniegan acceso e intentan LOGIN_FALLIDO obligatorio; fallos de auditoría deniegan acceso con error controlado.

## Pruebas aisladas

```sh
./mvnw -Djava.awt.headless=true '-Dtest=*Test,!*ManualTest' test
```

Surefire conserva exclusión `**/*IT.java`. Esta selección no usa SQL Server, Python ni cámara. Dos clases simulan HTTP y necesitan sockets locales. `*IT` son integración real y solo deben ejecutarse de forma explícita en una BD de pruebas autorizada. No confundir la compilación de IT con su ejecución.

`.env`, `.env.docker`, claves, contraseñas reales y configuración privada **no se versionan**. Los ejemplos se completan localmente. Nunca sustituir la clave AES de perfiles existentes sin un procedimiento acordado.
