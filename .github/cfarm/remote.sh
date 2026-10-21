#!/usr/bin/env bash
#
# ssh helper for the cfarm.yaml jobs. Some cfarm hosts intermittently drop an ssh
# connection before authentication (cfarm216 refused about half of all connections
# in October 2026), so every connection here retries when ssh fails to connect.
#
#   remote.sh check HOST           exit 0 if HOST takes ssh; 2 if it never answers
#                                  on port 22; 1 if it answers but ssh keeps failing
#   remote.sh run HOST SCRIPT      run SCRIPT through HOST's login shell, with
#                                  REPO_URL and BRANCH exported, under a 25-minute limit
#   remote.sh fetch HOST REMOTE LOCAL
#                                  copy REMOTE (relative to the home directory) to LOCAL
#
# The private key is read from GCC_CI_KEY.

set -uo pipefail

ATTEMPTS=5
RETRY_DELAY=20

KEY=$(mktemp)
trap 'rm -f "$KEY"' EXIT
printf '%s\n' "$GCC_CI_KEY" > "$KEY"
chmod 600 "$KEY"
SSH_OPTS=(-i "$KEY" -o BatchMode=yes -o ConnectTimeout=20 -o ServerAliveInterval=60
    -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o LogLevel=ERROR)

cmd=$1
host=$2

# ssh exits 255 for its own failures. One that comes within a minute is a refused or
# dropped connection, before the remote command could have done any real work.
refused() {
    [ "$1" -eq 255 ] && [ $((SECONDS - $2)) -lt 60 ]
}

case "$cmd" in
    check)
        port_open=false
        for attempt in $(seq $ATTEMPTS); do
            if timeout 20 bash -c "</dev/tcp/$host/22" 2>/dev/null; then
                port_open=true
                ssh "${SSH_OPTS[@]}" "oshi@$host" true && exit 0
            fi
            [ "$attempt" = $ATTEMPTS ] || sleep $RETRY_DELAY
        done
        [ "$port_open" = true ] && exit 1
        exit 2
        ;;
    run)
        # Both values are spliced into the remote command, so accept only plain names
        if [[ ! "$REPO_URL" =~ ^https://github\.com/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$ ]] \
            || [[ ! "$BRANCH" =~ ^[A-Za-z0-9_./-]+$ ]]; then
            echo "Refusing to run with REPO_URL='$REPO_URL' BRANCH='$BRANCH'" >&2
            exit 1
        fi
        remote="export REPO_URL='$REPO_URL' BRANCH='$BRANCH'
$(cat "$3")"
        for attempt in $(seq $ATTEMPTS); do
            start=$SECONDS
            timeout 25m ssh "${SSH_OPTS[@]}" "oshi@$host" "$remote"
            rc=$?
            refused $rc $start || exit $rc
            echo "ssh to $host was refused (attempt $attempt of $ATTEMPTS)" >&2
            [ "$attempt" = $ATTEMPTS ] || sleep $RETRY_DELAY
        done
        exit $rc
        ;;
    fetch)
        mkdir -p "$(dirname "$4")"
        for attempt in $(seq $ATTEMPTS); do
            start=$SECONDS
            scp "${SSH_OPTS[@]}" "oshi@$host:$3" "$4"
            rc=$?
            [ $rc -eq 0 ] && exit 0
            # scp exits 1 both when refused and when the file is missing, so a
            # missing report costs the retries before it is given up on
            [ $((SECONDS - start)) -lt 60 ] || exit $rc
            [ "$attempt" = $ATTEMPTS ] || sleep $RETRY_DELAY
        done
        exit $rc
        ;;
    *)
        echo "Usage: remote.sh check|run|fetch HOST ..." >&2
        exit 1
        ;;
esac
