# SIGELAB

**Sistema de Gestión y Control de Laboratorios Universitarios**

Proyecto final de Programación II — Universidad Mariano Gálvez de Guatemala.

## 1. Tecnologías utilizadas

- Java 21 y Java Swing
- Maven
- JDBC
- Microsoft SQL Server 2022
- Docker y Docker Compose
- Python para el módulo de reconocimiento facial
- Git y GitHub

## 2. Requisitos de software

Para ejecutar el proyecto se necesita:

- JDK 21
- Git
- Docker con Docker Compose
- IntelliJ IDEA o un IDE compatible con Maven
- DBeaver o una herramienta equivalente para ejecutar los scripts SQL
- Python para trabajar con el módulo facial

El proyecto está diseñado para desarrollarse en Linux y Windows.

## 3. Obtener el proyecto

Clonar el repositorio desde GitHub y abrir la carpeta `SIGELAB` en IntelliJ IDEA.

IntelliJ debe reconocer el archivo `pom.xml` y cargar automáticamente las dependencias Maven.

## 4. Configurar SQL Server

Crear el archivo `.env.docker` a partir de `.env.docker.example`.

En Linux:

```sh
cp .env.docker.example .env.docker
```

En Windows (PowerShell):

```powershell
Copy-Item .env.docker.example .env.docker
```

Abrir `.env.docker` y configurar una contraseña segura para el usuario administrador de SQL Server.

**Importante:** `.env.docker` contiene información privada y no debe subirse a GitHub.

## 5. Iniciar la base de datos

Desde la raíz del proyecto, ejecutar:

```sh
docker compose --env-file .env.docker up -d
```

Verificar el estado del contenedor:

```sh
docker compose --env-file .env.docker ps
```

La configuración local utiliza `127.0.0.1:1433`.

## 6. Crear las tablas y los datos iniciales

Abrir DBeaver y establecer una conexión con SQL Server utilizando:

- Host: `127.0.0.1`
- Puerto: `1433`
- Usuario: `sa`
- Contraseña: la definida en `.env.docker`
- Base inicial: `master`

Ejecutar los scripts en el siguiente orden:

1. `database/sigelab.sql`
2. `database/seed/001_roles.sql`

Para comprobar las tablas:

```sql
SELECT COUNT(*) AS total_tablas
FROM SIGELAB.sys.tables;
```

Resultado esperado: **14 tablas**.

Los roles iniciales son `ADMIN` y `ENCARGADO`.

## 7. Compilar y verificar el proyecto

En Linux:

```sh
./mvnw clean test
```

En Windows (PowerShell):

```powershell
.\mvnw.cmd clean test
```

El resultado esperado es `BUILD SUCCESS`.

## 8. Configuración JDBC

La clase `sigelab.config.DatabaseConfig` centraliza la conexión a SQL Server.

La configuración se obtiene de variables de entorno:

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USER`
- `DB_PASSWORD`
- `DB_ENCRYPT`
- `DB_TRUST_SERVER_CERTIFICATE`

Consultar `.env.example` para conocer los valores de referencia.

El archivo `.env.example` no se carga automáticamente desde Java. Las variables deben establecerse en el entorno de ejecución.

Para el servidor SQL local de desarrollo se puede utilizar `DB_TRUST_SERVER_CERTIFICATE=true`. Esta configuración no está destinada a producción.

## 9. Prueba de integración JDBC

La clase `DatabaseConnectionIT` comprueba la conexión real entre Java y SQL Server.

Con SQL Server ejecutándose y las variables de entorno configuradas, utilizar:

```sh
./mvnw -Dtest=DatabaseConnectionIT test
```

En Windows se sustituye `./mvnw` por `.\mvnw.cmd`.

Esta prueba requiere una base de datos disponible y se ejecuta de forma explícita.

## 10. Arquitectura

El proyecto utiliza la siguiente separación de responsabilidades:

`VIEW → CONTROLLER → SERVICE → REPOSITORY → JDBC → SQL SERVER`

Las reglas de negocio deben implementarse en la capa de servicios, no directamente en los formularios Swing.

## 11. Organización del repositorio

- `main`: versión estable.
- `develop`: integración del trabajo de los integrantes.
- `feature/*`: ramas individuales de desarrollo.

Los integrantes deben crear sus ramas desde `develop` y enviar sus cambios mediante Pull Request.

Antes de desarrollar módulos, consultar:

- `docs/TEAM_CONTRACT.md`
- `docs/RESPONSABILIDADES.md`
- `docs/DECISIONES.md`

Documentación de la base de datos:

- [Modelo entidad-relación](docs/MODELO_ER.md)
- [Diccionario de datos](docs/DICCIONARIO_DATOS.md)

## 12. Distribución del equipo

| Integrante | Responsabilidad |
|---|---|
| 1 | Fundación, base de datos, modelos, Enums y JDBC |
| 2 | Autenticación, usuarios y notificaciones |
| 3 | Laboratorios, catálogos y equipos |
| 4 | Préstamos |
| 5 | Devoluciones y reservas |
| 6 | Mantenimientos y auditoría |
| 7 | Códigos QR, reportes y dashboard |
| 8 | Reconocimiento facial |

## 13. Estado del proyecto

Proyecto en desarrollo. La funcionalidad de los módulos se integrará progresivamente en la rama `develop`.
