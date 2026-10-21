# Runs on cfarm152.cfarm.net (DragonFly BSD) through its login shell, with REPO_URL and BRANCH
# exported, via .github/cfarm/remote.sh from cfarm.yaml.
# Backup reaper: DragonFly's ps has no etime keyword, so instead of an age
# test, kill any JVM still running from this checkout. The concurrency
# group serializes runs, so one left over here is from a killed run.
pkill -9 -U oshi -f 'java.*/home/oshi/oshi/' 2>/dev/null || true
cd ~/oshi
export JAVA_HOME=~/java/jdk-25.0.4-dragonfly
git fetch "$REPO_URL" "$BRANCH"
git reset --hard FETCH_HEAD
git clean -xffd
trap 'ec=$?; trap "" TERM; kill 0 2>/dev/null; exit $ec' EXIT
BUILD='./mvnw clean test -Paggregate-coverage -B || { sleep 15; ./mvnw clean test -Paggregate-coverage -B; }'
timeout --kill-after=30s 20m sh -c "$BUILD"
exit $?
