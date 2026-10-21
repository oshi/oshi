#!/usr/bin/env bash
#
# Publish a release's javadoc on javadoc.io.
#
# javadoc.io only builds docs for a version after someone syncs the artifact's version list from
# Maven Central and uploads the version. This does both for each documented artifact, through the
# same forms its versions page submits. It needs no credentials, but the version must already be on
# Maven Central; pass --wait to poll for it first. The docs appear about 10 minutes after the upload.
#
# Usage:
#   ./scripts/update-javadoc-io.sh [--wait] [version]
#
# The version defaults to the newest oshi-parent-* tag.

set -euo pipefail

GROUP="com.github.oshi"
ARTIFACTS="oshi-common oshi-core oshi-core-ffm oshi-metrics"
JAVADOC_IO="https://javadoc.io"
CENTRAL="https://repo1.maven.org/maven2/com/github/oshi"
WAIT_MINUTES=120

WAIT=false
if [[ "${1:-}" == "--wait" ]]; then
    WAIT=true
    shift
fi
VERSION="${1:-$(git describe --tags --abbrev=0 --match 'oshi-parent-*' | sed 's/^oshi-parent-//')}"
if [[ ! "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "ERROR: '$VERSION' is not a release version." >&2
    exit 1
fi
echo "Version: $VERSION"

WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT
JAR="$WORK/cookies"
PAGE="$WORK/page.html"

# --- Wait for Maven Central ---------------------------------------------------
on_central() {
    curl -s --fail -o /dev/null "$CENTRAL/$1/$VERSION/$1-$VERSION.pom"
}

for a in $ARTIFACTS; do
    deadline=$((SECONDS + WAIT_MINUTES * 60))
    until on_central "$a"; do
        if [[ "$WAIT" != true ]]; then
            echo "ERROR: $a $VERSION is not on Maven Central yet. Retry later, or pass --wait." >&2
            exit 1
        fi
        if ((SECONDS > deadline)); then
            echo "ERROR: $a $VERSION did not reach Maven Central within $WAIT_MINUTES minutes." >&2
            exit 1
        fi
        echo "Waiting for $a $VERSION on Maven Central..."
        sleep 60
    done
done
echo "All artifacts are on Maven Central."

# --- javadoc.io -----------------------------------------------------------------
# Fetch an artifact's versions page into $PAGE, keeping the session cookie its CSRF token is tied to
fetch_page() {
    curl -s --fail --retry 3 -b "$JAR" -c "$JAR" -o "$PAGE" "$JAVADOC_IO/versions/$GROUP/$1"
}

csrf_token() {
    grep -o 'name="csrfToken" value="[^"]*"' "$PAGE" | head -1 | sed 's/.*value="//; s/"$//'
}

# The status badge's title in the version's row: UPLOADED once docs exist, DISCOVERED before upload
version_status() {
    grep -A4 "<td>$VERSION</td>" "$PAGE" | grep -o 'title="[A-Z_]*"' | head -1 | sed 's/title="//; s/"$//'
}

post_form() {
    local artifact=$1 action=$2
    shift 2
    curl -s --fail --retry 3 -b "$JAR" -c "$JAR" -o /dev/null -L \
        --data-urlencode "csrfToken=$(csrf_token)" "$@" "$JAVADOC_IO/versions/$GROUP/$artifact/$action"
}

failed=0
for a in $ARTIFACTS; do
    rm -f "$JAR"
    fetch_page "$a"
    if [[ -z "$(version_status)" ]]; then
        post_form "$a" sync
        for _ in 1 2 3 4 5 6; do
            fetch_page "$a"
            [[ -n "$(version_status)" ]] && break
            sleep 10
        done
    fi
    status=$(version_status)
    case "$status" in
        UPLOADED)
            echo "$a: $VERSION already available" ;;
        DISCOVERED)
            post_form "$a" upload --data-urlencode "versionId=$VERSION"
            fetch_page "$a"
            echo "$a: uploaded $VERSION (now $(version_status))" ;;
        "")
            echo "ERROR: $a: javadoc.io does not list $VERSION after syncing." >&2
            failed=1 ;;
        *)
            echo "$a: $VERSION is $status; leaving it alone" ;;
    esac
done

echo ""
echo "Docs appear about 10 minutes after upload, at $JAVADOC_IO/doc/$GROUP"
exit $failed
