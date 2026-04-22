#!/usr/bin/env bash
# Run OSHI JMH benchmarks.
# Can be run from anywhere; the script locates the repository root automatically.
#
# To run a specific benchmark class:
#   ./oshi-benchmark/scripts/run-benchmarks.sh ProcessesBenchmark

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

# If not already at the repo root, cd there automatically
if [ ! -f "${REPO_ROOT}/pom.xml" ]; then
    echo "ERROR: Could not locate repository root (expected pom.xml at ${REPO_ROOT})" >&2
    echo "Please run this script from within the oshi repository." >&2
    exit 1
fi
cd "${REPO_ROOT}"
JAR="${REPO_ROOT}/oshi-benchmark/target/benchmarks.jar"

# Require Java 25+
JAVA_MAJOR=$(java -version 2>&1 | head -1 | sed 's/.*version "\([0-9]*\).*/\1/')
if [ "${JAVA_MAJOR}" -lt 25 ] 2>/dev/null; then
    echo "ERROR (run-benchmarks.sh): Java 25+ is required, found Java ${JAVA_MAJOR}." >&2
    exit 1
fi

if [ ! -f "${JAR}" ]; then
    echo "benchmarks.jar not found — building first..."
    ./mvnw install -pl oshi-common,oshi-core,oshi-core-ffm -DskipTests -q
    ./mvnw package -pl oshi-benchmark -DskipTests -Dshade.phase=package -q
fi

exec java -jar "${JAR}" "$@"
