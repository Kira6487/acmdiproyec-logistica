#!/bin/sh
# Maven Wrapper bootstrap script. The official wrapper JAR is downloaded only
# when it is absent, then delegates all arguments to MavenWrapperMain.
set -eu

base_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
wrapper_dir="$base_dir/.mvn/wrapper"
wrapper_jar="$wrapper_dir/maven-wrapper.jar"
wrapper_properties="$wrapper_dir/maven-wrapper.properties"

if [ ! -r "$wrapper_jar" ]; then
  wrapper_url=$(sed -n 's/^wrapperUrl=//p' "$wrapper_properties")
  if [ -z "$wrapper_url" ]; then
    echo "No se encontró wrapperUrl en $wrapper_properties" >&2
    exit 1
  fi
  mkdir -p "$wrapper_dir"
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$wrapper_url" -o "$wrapper_jar"
  elif command -v wget >/dev/null 2>&1; then
    wget -q "$wrapper_url" -O "$wrapper_jar"
  else
    echo "Se requiere curl o wget para descargar Maven Wrapper." >&2
    exit 1
  fi
fi

exec java -Dmaven.multiModuleProjectDirectory="$base_dir" -classpath "$wrapper_jar" \
  org.apache.maven.wrapper.MavenWrapperMain "$@"
