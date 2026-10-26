#!/bin/sh
set -eu

MAVEN_VERSION=3.9.11
MAVEN_USER_HOME=${MAVEN_USER_HOME:-"${HOME}/.m2"}
MAVEN_HOME="${MAVEN_USER_HOME}/wrapper/dists/apache-maven-${MAVEN_VERSION}"
MAVEN_DIST="${MAVEN_HOME}/apache-maven-${MAVEN_VERSION}"
MAVEN_ZIP="${MAVEN_HOME}/apache-maven-${MAVEN_VERSION}-bin.zip"

if [ ! -x "${MAVEN_DIST}/bin/mvn" ]; then
  mkdir -p "${MAVEN_HOME}"
  if [ ! -f "${MAVEN_ZIP}" ]; then
    echo "Downloading Apache Maven ${MAVEN_VERSION}..."
    URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/${MAVEN_VERSION}/apache-maven-${MAVEN_VERSION}-bin.zip"
    if command -v curl >/dev/null 2>&1; then
      curl -fsSL "${URL}" -o "${MAVEN_ZIP}"
    elif command -v wget >/dev/null 2>&1; then
      wget -q "${URL}" -O "${MAVEN_ZIP}"
    else
      echo "curl or wget is required to download Maven." >&2
      exit 1
    fi
  fi
  if command -v unzip >/dev/null 2>&1; then
    unzip -oq "${MAVEN_ZIP}" -d "${MAVEN_HOME}"
  else
    echo "unzip is required to extract Maven." >&2
    exit 1
  fi
fi

exec "${MAVEN_DIST}/bin/mvn" "$@"
