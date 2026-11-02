# Prints the swap totals and every JVM crash report (hs_err_pid*.log) under the current directory.
# The Unix jobs call it, from the repository root, after a failed build: a VM guest is discarded the
# moment its run script fails, before `copyback` or any later step can retrieve a file, and a cfarm
# workspace is wiped by `git clean` at the start of the next run. Printing the report into the job
# log is the only way to keep it. A report holds the failure reason at the top and the rlimits and
# the physical and swap totals at the bottom, so it is printed whole.
#
# Plain POSIX sh, since it runs on every Unix CI host, including AIX's ksh and OpenBSD's ksh.
echo "===== swap ====="
case "$(uname -s)" in
  FreeBSD | DragonFly) swapinfo -h ;;
  OpenBSD | NetBSD) swapctl -l ;;
  SunOS) swap -s ;;
  AIX) lsps -s ;;
  Linux) free -h ;;
  *) echo "no swap command known for $(uname -s)" ;;
esac || true
find . -name 'hs_err_pid*.log' -type f | while read -r f; do
  echo "===== $f ====="
  cat "$f"
done
exit 0
