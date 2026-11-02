# Runs on cfarm191.cfarm.net (Debian, s390x, big-endian) through its login shell, with REPO_URL and BRANCH
# exported, via .github/cfarm/remote.sh from cfarm.yaml.
set -e
# Backup reaper: kill stale JVMs from a previously killed run (older
# than the build budget) so they can't hold the workspace hostage.
for pid in $(ps -u oshi -o pid=,etime=,comm= 2>/dev/null | awk '$3 ~ /java/ {
      et=$2; n=split(et,a,/[-:]/);
      m=(et ~ /-/)?999:(n==3?a[1]*60+a[2]:a[1]);
      if (m>=20) print $1 }'); do
  kill -9 "$pid" 2>/dev/null || true
done
[ -d ~/oshi ] || git clone -q "$REPO_URL" ~/oshi
cd ~/oshi
# Fetch + reset to the triggering ref regardless of local state.
# FETCH_HEAD avoids touching the cfarm clone's named branches.
git fetch "$REPO_URL" "$BRANCH"
git reset --hard FETCH_HEAD
git clean -xffd
# The distribution's OpenJDK, kept current by the box's own updates
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-s390x
# Reap the build's process group on exit (orphaned test JVMs) while
# shielding this shell so the build's real exit code is preserved.
trap 'ec=$?; trap "" TERM; kill 0 2>/dev/null; exit $ec' EXIT
set +e
# One wall-clock budget for both attempts, kept under the 25-minute ssh limit
# so the box self-terminates first and leaves no orphan.
# aggregate-coverage turns on the oshi.comparison tests, which check that the JNA,
# FFM and native-free implementations agree on a big-endian machine.
BUILD='./mvnw clean test -Paggregate-coverage -B || { sleep 15; ./mvnw clean test -Paggregate-coverage -B; }'
timeout --kill-after=30s 20m sh -c "$BUILD"
rc=$?
# The next run's git clean wipes any crash report, so print it now
[ "$rc" -eq 0 ] || sh .github/dump-crash-reports.sh
exit $rc
