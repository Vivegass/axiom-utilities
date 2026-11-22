#!/usr/bin/env bash
# Builds Axiom Utilities with plain javac (Minecraft 26.3 is unobfuscated, so no Loom/remapping is needed).
#
# It compiles against jars a Prism Launcher install already has. Override any of these if yours differ:
#   JAVA_HOME  JDK 25+                     (default: C:/Program Files/Java/jdk-25.0.2)
#   PRISM      Prism Launcher data folder  (default: %APPDATA%/PrismLauncher)
#   AXIOM      Axiom 6.1.x jar for MC 26.3 (default: the first libs/Axiom-*.jar; Axiom is not redistributable,
#              so download it yourself and drop it into libs/)
set -euo pipefail
cd "$(dirname "$0")"

prop() { grep -E "^$1=" mod.properties | head -1 | cut -d= -f2- | tr -d '\r'; }
VERSION="$(prop mod_version)"
MC="$(prop minecraft_version)"
AUTHOR="$(prop author)"
HOMEPAGE="$(prop homepage)"
SOURCES="$(prop sources)"
ISSUES="$(prop issues)"
FULL_VERSION="$VERSION+mc$MC"
JAR="build/libs/axiom-utilities-$FULL_VERSION.jar"

JDK="${JAVA_HOME:-/c/Program Files/Java/jdk-25.0.2}"
if command -v cygpath >/dev/null 2>&1; then
    PRISM="${PRISM:-$(cygpath -m "$APPDATA")/PrismLauncher}"
else
    PRISM="${PRISM:-$HOME/.local/share/PrismLauncher}"
fi
LIBS="$PRISM/libraries"
if [ -z "${AXIOM:-}" ]; then
    AXIOM="$(ls libs/Axiom-*.jar 2>/dev/null | head -1 || true)"
fi

SEP=":"; case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=";";; esac
CP="$AXIOM"
for lib in \
    "com/mojang/minecraft/26.3/minecraft-26.3-client.jar" \
    "net/fabricmc/fabric-loader/0.20.1/fabric-loader-0.20.1.jar" \
    "net/fabricmc/sponge-mixin/0.17.5+mixin.0.8.7/sponge-mixin-0.17.5+mixin.0.8.7.jar" \
    "org/joml/joml/1.10.9/joml-1.10.9.jar" \
    "com/google/code/gson/gson/2.14.0/gson-2.14.0.jar" \
    "org/slf4j/slf4j-api/2.0.17/slf4j-api-2.0.17.jar"; do
    if [ ! -f "$LIBS/$lib" ]; then echo "Missing library: $LIBS/$lib" >&2; exit 1; fi
    CP="$CP$SEP$LIBS/$lib"
done
if [ -z "$AXIOM" ] || [ ! -f "$AXIOM" ]; then echo "Axiom jar not found: put Axiom-6.1.x-for-MC26.3.jar into libs/ or set AXIOM=..." >&2; exit 1; fi
if [ "$AUTHOR" = "CHANGE_ME" ]; then echo "warning: author in mod.properties is still CHANGE_ME" >&2; fi

json_escape() { printf '%s' "$1" | sed -e 's/\\/\\\\/g' -e 's/"/\\"/g'; }
CONTACT=""
add_contact() { if [ -n "$2" ]; then CONTACT="$CONTACT${CONTACT:+,}
    \"$1\": \"$(json_escape "$2")\""; fi; }
add_contact homepage "$HOMEPAGE"
add_contact sources "$SOURCES"
add_contact issues "$ISSUES"

rm -rf build && mkdir -p build/classes build/libs
"$JDK/bin/javac" --release 25 -proc:none -encoding UTF-8 -cp "$CP" -d build/classes $(find src/main/java -name '*.java')
cp -r src/main/resources/. build/classes/
cp LICENSE build/classes/LICENSE_axiom_utilities

cat > build/classes/fabric.mod.json <<EOF
{
  "schemaVersion": 1,
  "id": "axiom_utilities",
  "version": "$FULL_VERSION",
  "name": "Axiom Utilities",
  "description": "Unofficial quality-of-life addon for the Axiom editor: offset target point for every tool, painting into the air next to blocks, global presets (brush + placement + mask + block) and a block pipette.",
  "authors": ["$(json_escape "$AUTHOR")"],
  "contact": {$CONTACT
  },
  "license": "MIT",
  "icon": "assets/axiom_utilities/icon.png",
  "environment": "client",
  "mixins": ["axiom_utilities.mixins.json"],
  "depends": {
    "fabricloader": ">=0.16.0",
    "minecraft": "~$MC",
    "java": ">=25",
    "axiom": ">=6.1.5 <6.2"
  }
}
EOF

"$JDK/bin/jar" --create --file "$JAR" -C build/classes .
echo "Built $JAR"
