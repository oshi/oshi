#!/usr/bin/env bash
#
# Upload a central-bundle.zip to Maven Central Portal.
#
# Use this when mvn deploy / release:perform builds the bundle successfully
# but the automatic upload fails (e.g. broken pipe).
#
# The bundle is typically at: target/central-publishing/central-bundle.zip
# or after release:perform:  target/checkout/target/central-publishing/central-bundle.zip
#
# Credentials are read from the "sonatype-central" server in ~/.m2/settings.xml.
#
# Usage:
#   ./scripts/upload-to-central.sh [path/to/central-bundle.zip]

set -euo pipefail

CENTRAL_API="https://central.sonatype.com/api/v1/publisher"
SERVER_ID="sonatype-central"

# --- Locate bundle -----------------------------------------------------------
if [[ $# -ge 1 ]]; then
    BUNDLE="$1"
else
    for candidate in \
        target/checkout/target/central-publishing/central-bundle.zip \
        target/central-publishing/central-bundle.zip; do
        if [[ -f "$candidate" ]]; then
            BUNDLE="$candidate"
            break
        fi
    done
fi

if [[ -z "${BUNDLE:-}" || ! -f "$BUNDLE" ]]; then
    echo "ERROR: central-bundle.zip not found." >&2
    echo "       Pass the path as an argument, or run from the project root after a failed deploy." >&2
    exit 1
fi
echo "Bundle: $BUNDLE ($(du -h "$BUNDLE" | cut -f1))"

# --- Credentials from settings.xml -------------------------------------------
SETTINGS="${HOME}/.m2/settings.xml"
if [[ ! -f "$SETTINGS" ]]; then
    echo "ERROR: $SETTINGS not found." >&2
    exit 1
fi

if command -v xmllint &>/dev/null; then
    USERNAME=$(xmllint --xpath "string(//server[id='$SERVER_ID']/username)" "$SETTINGS" 2>/dev/null)
    PASSWORD=$(xmllint --xpath "string(//server[id='$SERVER_ID']/password)" "$SETTINGS" 2>/dev/null)
else
    SERVER_BLOCK=$(sed -n "/<server>/,/<\/server>/p" "$SETTINGS" | tr '\n' ' ')
    USERNAME=$(echo "$SERVER_BLOCK" | grep -oP "<id>$SERVER_ID</id>.*?<username>\K[^<]+" || true)
    PASSWORD=$(echo "$SERVER_BLOCK" | grep -oP "<id>$SERVER_ID</id>.*?<password>\K[^<]+" || true)
fi

if [[ -z "$USERNAME" || -z "$PASSWORD" ]]; then
    echo "ERROR: Could not find credentials for server '$SERVER_ID' in $SETTINGS" >&2
    exit 1
fi
TOKEN=$(printf '%s:%s' "$USERNAME" "$PASSWORD" | base64)

# --- Upload -------------------------------------------------------------------
echo "Uploading to Central Portal..."
DEPLOYMENT_ID=$(curl --fail --silent --show-error \
    --connect-timeout 30 \
    --max-time 600 \
    --retry 3 \
    --retry-delay 10 \
    --request POST \
    --header "Authorization: Bearer $TOKEN" \
    --form "bundle=@$BUNDLE" \
    "$CENTRAL_API/upload?publishingType=USER_MANAGED&name=oshi")

echo "Upload successful!"
echo "Deployment ID: $DEPLOYMENT_ID"
echo ""
echo "Next steps:"
echo "  1. Check status:  https://central.sonatype.com/publishing"
echo "  2. Once validated, publish from the Portal UI"
