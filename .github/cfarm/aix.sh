# Runs on cfarm119.cfarm.net (AIX) through its login shell, with REPO_URL and BRANCH
# exported, via .github/cfarm/remote.sh from cfarm.yaml.
# Backup reaper: kill stale JVMs from a previously killed run (older
# than the build budget) so their open handles don't block git clean /
# mvn clean on the shared (possibly NFS) workspace.
for pid in $(ps -u oshi -o pid=,etime=,comm= 2>/dev/null | awk '$3 ~ /java/ {
      et=$2; n=split(et,a,/[-:]/);
      m=(et ~ /-/)?999:(n==3?a[1]*60+a[2]:a[1]);
      if (m>=21) print $1 }'); do
  kill -9 "$pid" 2>/dev/null || true
done
rm -fR ~/javashared*
cd ~/git/oshi
export JAVA_HOME=~/java/jdk-25.0.3+9
export PATH=$PATH:/opt/freeware/bin
export MAVEN_OPTS="-Xshareclasses:none"
# Fetch + reset to the triggering ref regardless of local state.
# FETCH_HEAD avoids touching the cfarm clone's named branches.
git fetch "$REPO_URL" "$BRANCH"
git reset --hard FETCH_HEAD
# Force-remove leftover build output (incl. NFS .nfs* sillyrenames).
git clean -xffd
# mvnw hash_string() overflows AIX ksh arithmetic; use mvn directly
MAVEN_VERSION=3.9.15
MAVEN_DIR=/home/oshi/software/apache-maven-$MAVEN_VERSION
if [[ ! -d "$MAVEN_DIR" ]]; then
  mkdir -p /home/oshi/software
  wget https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MAVEN_VERSION/apache-maven-$MAVEN_VERSION-bin.tar.gz -nv --max-redirect=0 -O /tmp/maven.tar.gz
  gunzip -f /tmp/maven.tar.gz
  cd /home/oshi/software && tar xf /tmp/maven.tar && rm /tmp/maven.tar
  cd ~/git/oshi
fi
export PATH=$MAVEN_DIR/bin:$PATH
# Pre-extract libjnidispatch.a to a stable path so the test JVM can
# System.load() it directly. JNA's default behavior is to unpack the
# library from the jar with a fresh per-PID filename on every JVM
# start, which is what flakes on OpenJ9. Use 'jar' (always shipped
# with the JDK) to avoid depending on AIX-side 'unzip' or 'sort -V'.
JNA_CACHE=/home/oshi/.cache/jna
JNA_LIB=$JNA_CACHE/libjnidispatch.a
mkdir -p "$JNA_CACHE"
JNA_VERSION=$(mvn -q -N help:evaluate -Dexpression=jna.version -DforceStdout 2>/dev/null)
JNA_JAR=$HOME/.m2/repository/net/java/dev/jna/jna/$JNA_VERSION/jna-$JNA_VERSION.jar
if [[ ! -f "$JNA_JAR" ]]; then
  mvn -q -N dependency:get -Dartifact=net.java.dev.jna:jna:$JNA_VERSION -Dmaven.gitcommitid.skip=true
fi
if [[ ! -f "$JNA_LIB" || "$JNA_JAR" -nt "$JNA_LIB" ]]; then
  # Extract into a per-run scratch dir on the SAME filesystem as the
  # cache so the final install is an atomic rename -- concurrency-safe
  # even if two runs populate at once (no shared /tmp scratch to race).
  SCRATCH=$(mktemp -d "$JNA_CACHE/.extract.XXXXXX")
  (cd "$SCRATCH" && jar xf "$JNA_JAR" com/sun/jna/aix-ppc64/libjnidispatch.a)
  mv "$SCRATCH/com/sun/jna/aix-ppc64/libjnidispatch.a" "$JNA_LIB"
  # Expose under both names in case the JVM's mapLibraryName picks .so
  ln -sf libjnidispatch.a "$JNA_CACHE/libjnidispatch.so"
  rm -rf "$SCRATCH"
fi
# jna.boot.library.path tells JNA to System.load() the pre-extracted
# file first; we deliberately do NOT set jna.nounpack so JNA can still
# fall back to JAR extraction if the boot-path load ever fails.
export _JAVA_OPTIONS="-Djna.boot.library.path=$JNA_CACHE"
# Reap the build's process group on exit (orphaned test JVMs) while
# shielding this shell so the build's real exit code is preserved.
trap 'ec=$?; trap "" TERM; kill 0 2>/dev/null; exit $ec' EXIT
# One wall-clock budget for both attempts, kept under the 25-minute ssh limit
# so the box self-terminates first and leaves no orphan.
BUILD='mvn clean test -Paggregate-coverage -B || { sleep 15; mvn clean test -Paggregate-coverage -B; }'
if command -v timeout >/dev/null 2>&1; then
  timeout --kill-after=30s 20m sh -c "$BUILD"
else
  sh -c "$BUILD"
fi
rc=$?
# The next run's git clean wipes any crash report, so print it now
[ "$rc" -eq 0 ] || sh .github/dump-crash-reports.sh
exit $rc
