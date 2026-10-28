# Runs on cfarm216.cfarm.net (Solaris SPARC) through its login shell, with REPO_URL and BRANCH
# exported, via .github/cfarm/remote.sh from cfarm.yaml.
set -e
source .profile
# Backup reaper: kill stale JVMs from a previously killed run (older
# than the build budget) so they can't hold the workspace hostage.
for pid in $(ps -u oshi -o pid=,etime=,comm= 2>/dev/null | awk '$3 ~ /java/ {
      et=$2; n=split(et,a,/[-:]/);
      m=(et ~ /-/)?999:(n==3?a[1]*60+a[2]:a[1]);
      if (m>=20) print $1 }'); do
  kill -9 "$pid" 2>/dev/null || true
done
cd ~/oshi
# Fetch + reset to the triggering ref regardless of local state.
# FETCH_HEAD avoids touching the cfarm clone's named branches.
git fetch "$REPO_URL" "$BRANCH"
git reset --hard FETCH_HEAD
# Force-remove leftover build output (incl. NFS .nfs* sillyrenames).
git clean -xffd
# Reap the build's process group on exit (orphaned test JVMs) while
# shielding this shell so the build's real exit code is preserved.
trap 'ec=$?; trap "" TERM; kill 0 2>/dev/null; exit $ec' EXIT
set +e
# One wall-clock budget for both attempts, kept under the 25-minute ssh limit
# so the box self-terminates first and leaves no orphan.
# jna-aggregate: JDK 17 aggregate report (oshi-common + oshi-core, no FFM) via oshi-demo;
# the oshi-benchmark aggregator needs JDK 25, unavailable on this SPARC box.
BUILD='./mvnw clean test -Pjna-aggregate -B || { sleep 15; ./mvnw clean test -Pjna-aggregate -B; }'
if command -v timeout >/dev/null 2>&1; then
  timeout --kill-after=30s 20m sh -c "$BUILD"
else
  sh -c "$BUILD"
fi
rc=$?
# The next run's git clean wipes any crash report, so print it now
[ "$rc" -eq 0 ] || sh .github/dump-crash-reports.sh
exit $rc
