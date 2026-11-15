# Sourced by the cfarm run scripts (aix.sh, solaris-sparc.sh, s390x.sh) from the repository root.
# Defines cfarm_build, which runs a build command, retrying it once if it fails, within one
# wall-clock budget for the whole run script.
#
# The budget counts from the start of the run script ($SECONDS, which ksh and bash both keep), so
# the fetch and `git clean` before the build count against it. On a loaded shared box those alone
# have taken five minutes. The budget stays under remote.sh's 25-minute ssh limit, so the box stops
# the build itself and the run script still prints its crash reports, rather than ssh cutting the
# session off with the build still running.
#
# A retry absorbs a one-off failure, such as a flaky test or a JVM crash. It is not attempted after
# a timeout: a build that has used up the budget once won't finish a second time in what is left.
# It is also skipped when too little time remains for a full build.

CFARM_BUDGET=1380    # 23 minutes, the whole run script
CFARM_MIN_BUILD=300  # 5 minutes, the least a build can finish in (a normal one takes 5 to 11)

# Whole seconds since the run script started (ksh93 keeps a fractional $SECONDS)
cfarm_elapsed() {
  echo "${SECONDS%.*}"
}

# Runs "$@" under what is left of the budget; fails with 124 if too little is left to start
cfarm_attempt() {
  left=$((CFARM_BUDGET - $(cfarm_elapsed)))
  if [ "$left" -lt "$CFARM_MIN_BUILD" ]; then
    echo "Only ${left}s of the budget remain; not starting the build."
    return 124
  fi
  timeout --kill-after=30s "${left}s" "$@"
}

cfarm_build() {
  cfarm_attempt "$@"
  rc=$?
  case $rc in
    0) return 0 ;;
    # 124: the budget ran out. 137 (265 in ksh93): the build ignored SIGTERM and timeout
    # sent SIGKILL to its process group, itself included.
    124 | 137 | 265)
      echo "::error title=Build timed out::The build used up the ${CFARM_BUDGET}s budget after $(cfarm_elapsed)s; not retrying."
      return 124
      ;;
  esac
  if [ $((CFARM_BUDGET - $(cfarm_elapsed) - 15)) -lt "$CFARM_MIN_BUILD" ]; then
    echo "Build failed with exit code $rc after $(cfarm_elapsed)s; too little of the budget is left to retry."
    return $rc
  fi
  echo "Build failed with exit code $rc after $(cfarm_elapsed)s; retrying once."
  sleep 15
  cfarm_attempt "$@"
}
