#!/usr/bin/env bash
# ==============================================================================
# SIGELAB - Script de inicio para Linux (Ubuntu, Debian, Mint, Arch, Fedora)
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 1. Búsqueda y validación de Java 21
detectar_java21() {
    local candidato=""
    
    # Prioridad 1: JAVA_HOME ya definido en el entorno
    if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then
        if "$JAVA_HOME/bin/java" -version 2>&1 | grep -qE '(openjdk|java) (version )?"?21'; then
            echo "$JAVA_HOME/bin/java"
            return 0
        fi
    fi

    # Prioridad 2: Comando java en PATH
    if command -v java >/dev/null 2>&1; then
        if java -version 2>&1 | grep -qE '(openjdk|java) (version )?"?21'; then
            echo "$(command -v java)"
            return 0
        fi
    fi

    # Prioridad 3: Rutas habituales de instalación en Linux
    local rutas_comunes=(
        "$HOME/bin/jdk-21/bin/java"
        "/usr/lib/jvm/java-21-openjdk-amd64/bin/java"
        "/usr/lib/jvm/temurin-21-jdk-amd64/bin/java"
        "/usr/lib/jvm/jdk-21/bin/java"
        "/opt/jdk-21/bin/java"
    )

    for candidato in "${rutas_comunes[@]}"; do
        if [[ -x "$candidato" ]]; then
            if "$candidato" -version 2>&1 | grep -qE '(openjdk|java) (version )?"?21'; then
                echo "$candidato"
                return 0
            fi
        fi
    done

    return 1
}

JAVA_BIN=""
if ! JAVA_BIN="$(detectar_java21)"; then
    echo "==========================================================================" >&2
    echo "Error: No se encontró Java 21 (JDK 21) en el sistema." >&2
    echo "SIGELAB requiere Java 21 LTS." >&2
    echo "" >&2
    echo "Opciones para solucionarlo:" >&2
    echo "  1. Si ya lo tienes descargado, exporta su ruta antes de ejecutar:" >&2
    echo "     export JAVA_HOME=/ruta/a/tu/jdk-21" >&2
    echo "     export PATH=\$JAVA_HOME/bin:\$PATH" >&2
    echo "  2. O instálalo con tu gestor de paquetes:" >&2
    echo "     sudo apt install openjdk-21-jdk" >&2
    echo "==========================================================================" >&2
    exit 1
fi

JAVA_HOME_DIR="$(cd -- "$(dirname -- "$JAVA_BIN")/.." && pwd)"
export JAVA_HOME="$JAVA_HOME_DIR"
export PATH="$JAVA_HOME/bin:$PATH"

# 2. Validación de archivo de configuración .env
if [[ ! -f ".env" ]]; then
    if [[ -f ".env.example" ]]; then
        echo "Aviso: No se encontró .env. Copiando desde .env.example..."
        cp .env.example .env
        echo "Por favor, configure las credenciales de la base de datos en .env."
    fi
fi

# 3. Permiso de ejecución en Maven Wrapper
if [[ -f "./mvnw" && ! -x "./mvnw" ]]; then
    chmod +x ./mvnw
fi

# 4. Construcción incremental y generación de classpath de ejecución
CLASSPATH_FILE="target/runtime-classpath-linux.txt"
if [[ ! -f "$CLASSPATH_FILE" || ! -d "target/classes" ]]; then
    echo "Preparando dependencias y compilando SIGELAB..."
    ./mvnw -DskipTests compile dependency:build-classpath \
        -Dmdep.includeScope=runtime \
        -Dmdep.outputFile="$CLASSPATH_FILE"
fi

RUNTIME_CLASSPATH="$(cat "$CLASSPATH_FILE")"
FULL_CLASSPATH="target/classes:${RUNTIME_CLASSPATH}"

# 5. Ejecutar la aplicación principal
echo "Iniciando SIGELAB con Java 21..."
exec "$JAVA_BIN" -cp "$FULL_CLASSPATH" sigelab.Main "$@"
