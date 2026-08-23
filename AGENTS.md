# AGENTS.md

Instructions for AI coding agents working in the OSHI repository. Humans should start with
[README.md](README.md) and [CONTRIBUTING.md](CONTRIBUTING.md); this file covers what an agent
is likely to get wrong.

## What OSHI is

A cross-platform Java library that reads operating system and hardware information through native
APIs, with no bundled native libraries. It supports Windows, macOS, Linux/Android, and six UNIX
variants (AIX, DragonFly BSD, FreeBSD, NetBSD, OpenBSD, Solaris/illumos), through two independent
native-access implementations (JNA and the JDK Foreign Function & Memory API).

**The single most important consequence: you can only build and test the platform you are running
on, and only against the hardware you happen to have.** Most changes in this repository cannot be
verified by running them. Plan for that instead of assuming a green local build means the change is
correct.

## Repository map

| Path | Contents |
|---|---|
| `oshi-common` | Public API interfaces, abstract bases, POJOs, shared parsing/util code, and a pure-Java (no native access) implementation under `oshi.nativefree` for Linux and NetBSD. **Java 8; no JNA, no FFM.** |
| `oshi-core` | The JNA implementation. **Java 8.** |
| `oshi-core-ffm` | The FFM implementation. **Java 25+.** |
| `oshi-demo` | Proof-of-concept examples, jbang catalog entries. |
| `oshi-metrics` | Micrometer bindings. Java 17+. |
| `oshi-benchmark` | JMH benchmarks and the `oshi.comparison` cross-implementation tests. Java 25+. |
| `oshi-dist` | Distribution zip assembly (a single `.zip`, excluding `oshi-demo`). |
| `config/` | Checkstyle, import-control, forbidden-apis, license header, formatter, Sonar config. |
| `src/site/` | Maven site sources (`SampleOutput.md`, project listings). |

Within `oshi-core` and `oshi-core-ffm` the layout mirrors each other:

- `oshi/hardware/platform/<os>/` and `oshi/software/os/<os>/` — the per-OS implementations of the
  public interfaces.
- `oshi/driver/<os>/` — reusable helpers that fetch and parse one specific thing (a sysfs file, a
  WMI query, a `sysctl` call). Most parsing logic belongs here, not in the HAL classes.
- `oshi/jna/platform/<os>/` (JNA) and `oshi/ffm/platform/<os>/` (FFM) — the native mappings.
- `oshi/util/` — shared utilities. `ParseUtil`, `ExecutingCommand`, `FileUtil`, `GlobalConfig`.

### Where code belongs

`oshi-common` is not just the interfaces — it holds the shared *implementation*. Only code that
actually touches JNA or FFM types belongs in `oshi-core` / `oshi-core-ffm`.

Anything else — parsing, unit conversion, string handling, sorting and filtering, caching, sentinel
and range checks, the logic that turns raw native output into a returned value — belongs in
`oshi-common`, as an abstract base class the platform implementations extend, a `driver`/`util`
class both call, or a POJO they populate. If you are writing the same logic in both twins, it is in
the wrong place.

Reducing this duplication has been a sustained effort in the project, so **default to hoisting**
rather than copying. A change that adds parallel logic to both implementations will be asked to
justify why it is not shared.

**Before writing any parsing helper, check whether `ParseUtil` already has one.** It is the home for
general parsing primitives — numbers, sizes, frequencies, hex, delimited fields, and dates
(`parseDateToEpoch`, `parseCimDateTimeToOffset`, `parseYearlessDateToEpoch`). The duplication is
easy to miss because the two copies live in per-platform drivers that you would never read side by
side: the AIX and generic-UNIX `who` readers each grew their own copy of "parse a `MMM d HH:mm`
timestamp that carries no year, defaulting to the current year and subtracting one if that lands in
the future" before it was hoisted. If a command on one OS emits a format, a command on another
probably does too, so put the format parser in `ParseUtil` and leave only the
command-specific regex in the driver.

Runtime configuration lives in `oshi-common/src/main/resources/oshi.properties`, read through
`oshi.util.GlobalConfig` or overridden with Java system properties. Configuration is read at
startup, is not thread-safe, and is not re-read during operation. Add new settings there rather
than introducing a separate mechanism.

## The feedback loop

```sh
./mvnw clean install                 # full reactor build + tests
./mvnw -pl oshi-core -am test        # one module and its dependencies
./mvnw spotless:apply                # formatting, import order, license-header years
./mvnw checkstyle:check              # includes import-control
./mvnw forbiddenapis:check           # see "Banned APIs" below
./mvnw javadoc:javadoc               # javadoc errors fail the release build
```

⚠️ **`forbiddenapis:check` and `javadoc:javadoc` need compiled classes, so run them after `install`
in the same command, never after a bare `clean`.** `./mvnw clean javadoc:javadoc` fails with
`Creating an aggregated report for both named and unnamed modules is not possible` — `clean` deletes
`oshi-common/target/classes/module-info.class`, no standalone goal regenerates it, and the
aggregator then sees one unnamed module among named ones. The error names `oshi-common` and looks
like a module-descriptor problem in the project; it is not.

Print a full report for the machine you are on — the fastest way to sanity-check a change by hand:

```sh
./mvnw install -DskipTests
./mvnw exec:java -pl oshi-core -Dexec.mainClass="oshi.SystemInfoTest" -Dexec.classpathScope="test"
./mvnw exec:java -pl oshi-core-ffm -Dexec.mainClass="oshi.ffm.SystemInfoTest" -Dexec.classpathScope="test"
```

Which modules are in the reactor depends on the JDK you build with: `oshi-metrics` needs 17+,
and `oshi-core-ffm`, `oshi-benchmark`, and `oshi-dist` need 25+. A build on JDK 8–16 silently
skips them. **Use a JDK 25+ toolchain so FFM code is actually compiled.**

### Stale `oshi-common` is the most common self-inflicted failure

`oshi-common` is a dependency of both `oshi-core` and `oshi-core-ffm`. When it is not part of the
build you just ran, Maven resolves it from the copy installed in your local `~/.m2` repository by a
previous build — which may be older than your working tree. Because both the stale and the current
copy carry the same `-SNAPSHOT` version, nothing warns you.

Symptoms: a change to `oshi-common` appears to have no effect; a method you just added is "not
found"; a test fails against behavior you already fixed; `NoSuchMethodError`, `AbstractMethodError`,
or an unresolvable symbol in a module that clearly compiles.

Avoid it:

- Use `./mvnw clean install` from the repository root when you have touched `oshi-common`, and
  before any final verification run.
- Always pass `-am` with `-pl` (`./mvnw -pl oshi-core -am test`) so dependency modules are rebuilt
  from source in the same reactor rather than resolved from `~/.m2`.
- Run `./mvnw install -DskipTests` before `exec:java`, benchmarks, or anything else that runs
  outside the reactor.

If a result does not make sense, rebuild clean from the root before spending time debugging it.

Before opening a PR, run the whole gate as one command, and fold any spotless reformatting into the
same commit:

```sh
./mvnw clean spotless:apply sortpom:sort checkstyle:check install forbiddenapis:check javadoc:javadoc -Paggregate-coverage
```

Spotless first, because it shifts line numbers. `install` before `forbiddenapis:check` and
`javadoc:javadoc`, because both need compiled classes — see the warning above. `checkstyle:check`
must be named explicitly: it is only bound to a phase in the `checks` profile, which is not active
by default. Add `-pl <modules> -am` to narrow the run to the modules you touched.

⚠️ **`-Paggregate-coverage` is not optional if you renamed, moved or deleted anything the
`oshi.comparison` tests reference.** `oshi-benchmark` sets `<maven.test.skip>true</maven.test.skip>`
in its own pom, and `maven.test.skip` suppresses test *compilation*, not just execution — so a full
green reactor build compiles **zero** of those test sources and cannot tell you they no longer
build. Either `-Paggregate-coverage` or `-Pnative-comparison` turns them back on. This is how a PR
deleting six driver classes reached CI with the comparison tests still importing them.

### Tests

Tests run under `de.sormuras.junit:junit-platform-maven-plugin`, **not** Surefire. There is no
`Tests run: N` line to grep for and Surefire `includes`/`excludes` do nothing. Check the exit code,
and look for `tests failed` or `[X]` markers in the output. Test files are named `*Test.java`.

JUnit 5 + Hamcrest are the only test dependencies. **Mockito is deliberately absent** — do not add
it. To make code testable, extract the parsing from the acquisition: a method that takes a
`List<String>` or `String` and returns a parsed result, called by a thin method that does the
`readFile`/`runNative`. Test the parse method against fixture data.

#### A new test package needs an entry in `module-info.test`

Tests run on the **module path**, patched into the module under test. JUnit instantiates a test
class reflectively, so the package has to be open to it — and the `opens` for tests lives in
`src/test/java/module-info.test`, not in `src/main/java/module-info.java`. That file holds the raw
JVM options the plugin passes to the test run.

**Adding a test in a package that no existing test uses means adding a line to that module's
`module-info.test`:**

```text
--add-opens
  com.github.oshi/oshi.software.os.windows=org.junit.platform.commons
```

The module name is `com.github.oshi` for `oshi-core`, `com.github.oshi.ffm` for `oshi-core-ffm`, and
`com.github.oshi.common` for `oshi-common`. The same file is also where `--add-reads`,
`--add-modules`, and `--enable-native-access` for tests are declared.

⚠️ **A missing entry fails only on the platform that actually runs the test.** A test guarded by
`@EnabledOnOs(OS.WINDOWS)` is skipped at the container level everywhere else — before JUnit ever
tries to construct it — so a full green build on Linux or macOS proves nothing about the opens.
The failure surfaces in CI as:

```text
[X] Unable to make oshi.software.os.windows.SomeTest() accessible:
    module com.github.oshi does not "opens oshi.software.os.windows" to module org.junit.platform.commons
```

To check the wiring from any platform, drop an unconditional throwaway test in the new package,
confirm it runs, and delete it.

## Hard rules that CI enforces

**Language level.** `oshi-common` and `oshi-core` compile **main** sources with `release 8`. No
`var`, no `List.of`, no records, no switch expressions, no text blocks, no `Stream.toList()`. Only
`oshi-core-ffm` (25), `oshi-metrics` (17), and `oshi-benchmark` (25) get modern Java in main
sources. Agents get this wrong constantly.

**Their `src/test` compiles at `release 17`**, set by `maven.compiler.testRelease` in the root
`pom.xml`. Test classes are never published, so the Java 8 guarantee to consumers does not reach
them. Use text blocks for captured command or file output, `Files.writeString` over
`Files.write(path, s.getBytes(UTF_8))`, and `Stream.toList()`; `var` is fine and is already the
house style in `oshi-core-ffm`. Two cautions:

- **NullAway checks test sources too**, at ERROR, the same as main. Compiling tests at 17 put them
  on the module path, which brings `module-info.java`'s `@NullMarked` into scope for test packages.
  A stub override must repeat the parent's `@Nullable`; a `Map.get()` needs a local plus
  `assertNotNull`, which narrows where Hamcrest's `is(notNullValue())` does not.
- **17 is pinned from both directions.** JUnit 6 requires 17+, so it is also a floor. The ceiling is
  Solaris SPARC on the GCC compile farm, the lowest JDK running tests in CI: it runs Oracle's JDK 17
  for Solaris 11.4, a build made specifically for that platform, with no 21 or 25 successor. It does
  not move while that platform is tested. Every other job is 21 or newer.
- **Do not sweep `Arrays.asList` to `List.of`.** `List.of` rejects nulls and is immutable, and
  fixtures here model real-world output where a null is sometimes deliberate. Prefer `List.of` in
  new code; leave working call sites alone.

Text blocks strip incidental leading and trailing whitespace, and spotless applies
`trimTrailingWhitespace` to Java sources, so a fixture whose real output carries trailing spaces or
tab alignment needs explicit `\s` or `\t` escapes. Converting a fixture is a judgment call, never a
find-and-replace — and not every fixture wants one. `FileUtilTest.testReadProcIo` deliberately keeps
generating its file body from the map it asserts against, because a text block there would duplicate
the data.

**Banned APIs** (`config/forbidden-apis.txt` and friends, enforced by `forbiddenapis:check`):

- `Integer.parseInt`, `Long.parseLong`, `Double.parseDouble`, `Integer.decode`, and relatives are
  banned. Use `oshi.util.ParseUtil` — `parseIntOrDefault`, `parseLongOrDefault`,
  `parseHyphenatedIntList`, `parseDHMSOrDefault`, and so on. They fail soft on the malformed input
  that real system output regularly produces.
- `org.slf4j.event.**` is banned; use `oshi.util.LogLevel`. OSHI supports hosts pinning slf4j 1.7.
- `oshi-common` may not reference `com.sun.jna.**`, `java.lang.foreign.**`, or any
  `System.load`/`loadLibrary`. It must stay pure JDK.
- `oshi-core-ffm` may not reference `com.sun.jna.**`.

**Import control.** `config/import-control.xml` allowlists every non-OSHI package that may be
imported, and must stay consistent with each module's `module-info.java`
(`src/main/java/module-info.java`). A new external import means editing both, plus possibly the
module's `pom.xml`. Checkstyle fails otherwise.

**API compatibility.** The semver guarantee does not cover the whole codebase. It covers the
interfaces and classes in `oshi.hardware` and `oshi.software.os`, plus anything annotated
`@PublicApi` — see [FAQ.md](FAQ.md#is-the-api-backwards-compatible-between-versions) for the full
statement. Platform implementations in the lower-level packages may change between minor versions,
and `oshi.driver` and `oshi.util` may change rarely, so **`ParseUtil` and its neighbours are not
semver-locked**; in `oshi/util/` only `PlatformEnum` carries the annotation, because it is returned
by public API.

Everything under `oshi.jna` and `oshi.ffm` — the native mappings and the utilities supporting
them — sits further out still: explicitly temporary, and not something dependent projects are
meant to rely on. The sole exception is `oshi.ffm.SystemInfo`, the FFM entry point, which is
`@PublicApi`.

Stability there is still valued — don't churn a util signature gratuitously — but a change to one is
a judgment call rather than an automatic breaking change. Check the annotation before calling
something a breaking change: `japicmp:cmp` reports on everything it can see, and the annotation is
what decides whether a report implies a version bump.

**Formatting and headers.** Spotless owns formatting and the import block, so you do not need to
remove unused imports — but you do need to *add* the ones you use. New files get a current-year-only
header (`Copyright 2026 The OSHI Project Contributors` / `SPDX-License-Identifier: MIT`), even when
copied from an older file; spotless maintains end years on existing files.

**Javadoc.** Every public and protected type and method needs javadoc. Do not use fully-qualified class names in
code or in javadoc — including inside `{@link ...}`. Import the type and link the simple name.

Two tools split the job. **Checkstyle owns presence** (`MissingJavadocType`, `MissingJavadocMethod` at
`scope=protected`); **doclint owns correctness** — the javadoc plugin runs `-Xdoclint:all,-missing`, so bad tags,
broken `{@link}` and malformed HTML still fail the build. `missing` is off there because it also demands a comment
on every protected field of the C-struct mirrors and on every implicit constructor: ~580 sites of no value to a
reader. Fields are deliberately not checked (no `MissingJavadocVariable`), and the `jna`/`ffm` binding packages and
the `*JNA.java`/`*FFM.java` backend twins are suppressed — a twin's only undocumented member is a public
constructor that forwards to the abstract parent's, which *is* documented.

⚠️ **Checkstyle caches results per file in `target/checkstyle-cachefile`, so a second run only re-checks what
changed.** Measuring a rule change without `mvn clean` first will report a fraction of the real violations and look
like a fix. The same trap applies to `javadoc:javadoc`, which silently skips when `target/reports` is already
populated. Always `clean` before believing either number — but `clean` alone breaks `javadoc:javadoc`, so pair it
with something that recompiles: `./mvnw clean compile javadoc:javadoc`, or the full gate command above.

**Suppressing a warning.** Both tools have one house style. Never add a line range to
`config/checkstyle-suppressions.xml` — a range breaks the moment anything above it shifts, including a
single added import, and the failure looks unrelated to the change that caused it.

Checkstyle, in order of preference:

| Form | Use it for |
|---|---|
| `@SuppressWarnings("checkstyle:ConstantName")` | a single declaration — cannot drift |
| `// CHECKSTYLE:OFF <Check> — why` … `// CHECKSTYLE:ON <Check>` | a contiguous run of declarations breaking the same rule |
| `// CHECKSTYLE.SUPPRESS: <Check> for <n> lines` | a one-off; `n` counts the comment's own line plus the next `n` |
| `<suppress checks=… files=…/>` (no `lines`) | a whole file or a naming convention across a package |

Sonar: **`// NOSONAR java:Sxxxx - why`**, one space, modern `java:` prefix (not the legacy `squid:`), reason
after a dash. Two things to know:

- **`NOSONAR` suppresses every issue on that line, whatever the rule.** The rule id after it is documentation
  only — Sonar does not parse it. So keep the suppressed line short, or a future real bug on it is silenced too.
- **Prefer `@SuppressWarnings("java:Sxxxx")` when there is a declaration to annotate**, because that *is*
  rule-scoped. Sonar honors it with no configuration; measured on `Memoizer`'s volatile field, where swapping
  the comment for the annotation kept `S3077` quiet. Reach for `NOSONAR` when the issue is on a statement,
  a `catch`, or anything else you cannot annotate.

Merely writing the word `NOSONAR` in prose suppresses that line, so describe one as "the suppression below"
rather than repeating the keyword.

`NOSONAR` only works on the offending line, so it cannot be moved above. If the line is too long to carry the
trailing comment, spotless will wrap it and strand the rule id underneath — shorten the line rather than
working around the wrap. The AIX uptime pattern is the worked example: splitting the regex into named
constants both shortened it and made Sonar stop reporting `S5843` at all, so the suppression could go.

## Conventions that apply to nearly every class

**Concurrency annotations.** Roughly two thirds of the classes in this project carry
`@ThreadSafe`, `@Immutable`, `@NotThreadSafe`, or `@GuardedBy` from
`oshi.annotation.concurrent`. These are documentation, not enforcement — but new classes are
expected to declare one, and matching the annotation of the class you are modeling yours on is the
right default.

**Nullability.** Every package in `oshi-common`, `oshi-core` and `oshi-core-ffm` is `@NullMarked`
(JSpecify), so **every type usage in a signature is non-null unless you annotate it `@Nullable`**.
`@NullMarked` does not extend to subpackages, so the marking is applied at two levels. The API
packages — `oshi`, `oshi.hardware`, `oshi.software.os`, `oshi.util` and its subpackages, `oshi.spi`,
`oshi.annotation` and `oshi.ffm` — carry it in their `package-info.java`, which is the only form a
consumer sees on a Java 8 or classpath build. Everything behind them is marked on the three
`module-info.java` descriptors, which a consumer sees only on the module path; those packages are
internal either way. The native mapping packages under `oshi.jna` and `oshi.ffm.platform` are
explicitly `@NullUnmarked` — they mirror C structs and libraries, where nullability is the operating
system's to state, not OSHI's. Their *utility* neighbours (`oshi.jna.util`, `oshi.util.platform.*`)
are marked. `oshi-metrics` is not marked: it implements Micrometer interfaces that carry no
nullability annotations of their own. New packages are expected to be marked; a new one under
`oshi.jna` or `oshi.ffm.platform` gets the opt-out `package-info.java` its siblings have.

**This is enforced, not documentation.** NullAway runs in the `error-prone` profile with
`OnlyNullMarked=true` and `JSpecifyMode=true`, at ERROR severity, so a violation inside a marked
package fails the build — in main *and* test sources, since `-DskipTests` skips execution but not
compilation. `OnlyNullMarked=true` means the analysis follows the marking, not the module list:
mark a package and it is checked, everywhere, with no build-file change. That cuts both ways —
marking a package is a commitment to fixing every finding in it, so add one marking at a time and
run `./mvnw -Perror-prone clean install` before assuming it is free.

**The module-level marking is only enforced because the `error-prone` profile changes how
`module-info.java` is compiled.** `oshi-common` and `oshi-core` normally *exclude* it from the
release-8 compile and build it separately at release 9, so NullAway — which runs during the main
compile — would never read the module declaration. Under the profile, those two modules compile
whole at release 9 with `module-info.java` included. Do not "simplify" that duplicated compiler
configuration away: it silently switches off nullness checking for every implementation package.
The shipped artifacts are unaffected, since the release build does not use the profile.

Three findings are worth recognizing on sight. *Assigning `@Nullable` expression to `@NonNull`
field* — normalize at the assignment, do not annotate the field. *Parameter is `@NonNull`, but
overridden method's parameter is `@Nullable`* — an override cannot narrow a parameter, so repeat
the interface's `@Nullable` on it; this is the one that reaches test stubs. *Dereferenced
expression is `@Nullable`* — NullAway cannot see through a predicate, so `Util.isBlank(s)` does not
establish that `s` is non-null on the following line.

**Normalize with `ParseUtil.getStringValueOrUnknown` / `getStringValueOrEmpty` rather than an
inline ternary.** They take a `@Nullable String` and return a non-null one — `Constants.UNKNOWN`
for a blank input, and `""` for a null one respectively — so the guarantee lives in the return type
the analyzer already reads. That is why they work where `Util.isBlank` does not: a predicate would
need a `@Contract` annotation, a normalizer needs nothing. Use them for the value case and keep
`Util.isBlank` for the branch case.

In test sources, assert rather than suppress: NullAway runs with
`HandleTestAssertionLibraries=true`, so JUnit's `assertNotNull` narrows a nullable for the rest of
the method. Hamcrest's `assertThat(x, is(notNullValue()))` does **not** — it reads as an ordinary
call, so convert those to `assertNotNull` where the following line dereferences `x`.

Do not reach for `@Nullable` to make a null return legal: the convention is a **sentinel, not a
null** — `Constants.UNKNOWN` or `""` for strings, an empty collection or array, and `0`/`-1`/`NaN`
for numbers, whichever is outside the value's real range. Annotate a return `@Nullable` only when
absence is genuinely meaningful to the caller and no sentinel can express it (`getProcess(int)` for a
process that is not running). Nullable *parameters* are the common case and mean "optional argument".
The jspecify dependency is `optional` in `oshi-common/pom.xml` and `requires static org.jspecify` in
its `module-info.java`; do not make it a hard dependency.

**Memoization.** System calls are expensive, and callers poll. Values that are constant (CPU model,
serial number) or expensive-but-slow-changing are wrapped in
`oshi.util.Memoizer.memoize(supplier, ...)` — usually as a `Supplier<T>` field, with
`defaultExpiration()` for values that must refresh. Follow the pattern in the surrounding class
when adding a getter that makes a native call, and read [PERFORMANCE.md](PERFORMANCE.md) before
adding one to a path that callers hit in a loop.

**Prefer a native call to anything else.** Below that, the ranking is platform-dependent, and the
common assumption that reading a file always beats running a command is wrong here. On Linux,
`/proc` and `/sys` are kernel interfaces rather than real disk, so `FileUtil` reads are cheap and
preferred over `ExecutingCommand`. On other platforms a file read is an actual disk read, with no
inherent advantage over spawning a command — there, `ExecutingCommand` is a legitimate way to reach
information the native APIs do not expose, not a last resort to be engineered around.

## The JNA/FFM twin rule

Nearly every class in `oshi-core` has a counterpart in `oshi-core-ffm` implementing the same
`oshi-common` interface for the same platform. **A behavior change to one is almost always a bug
unless the twin gets it too.** This has been the single largest source of defects in the project:
one implementation gains a null guard, a charset fix, or a sentinel-value check, and the other
quietly keeps returning wrong data on the same input.

When you change a HAL or OS component:

1. Find the twin (same package path, `oshi-core` ↔ `oshi-core-ffm`) and apply the equivalent change.
2. If the twin is intentionally different, say why in the commit message.
3. Better still, do not write it twice: if the change is not specific to JNA or FFM types, hoist it
   into `oshi-common` so both implementations inherit it and cannot diverge again. See
   [Where code belongs](#where-code-belongs). Rebuild from the root afterward; see the
   stale-`oshi-common` note above.

`oshi-benchmark`'s `oshi.comparison` tests (`NativeComparisonTest`, `WmiComparisonTest`,
`RegistryComparisonTest`, `PerfmonComparisonTest`, `NativeFreeComparisonTest`) assert that the
implementations agree; they run under `-Paggregate-coverage`.

## Writing code you cannot run

Most of this repository targets platforms you are not on. When editing AIX, Solaris, BSD, or a
Windows/macOS path from a different host:

- Copy the shape of the nearest working sibling implementation rather than inventing a new approach.
- Native struct offsets, field widths, and `sysctl`/`ioctl` constants are per-OS and per-architecture.
  Verify them against the platform's real headers; do not infer them from another OS.
- Say plainly in the PR description what you could not verify. "Compiles; untested on AIX" is a
  useful and welcome statement.
- CI covers the rest: GitHub Actions for Windows/macOS/Linux, vmactions VMs for the BSDs and
  Solaris, and the GCC compile farm (`cfarm.yaml`) for AIX and SPARC. Some of those run only on
  push to master or via `workflow_dispatch`, not on PRs.

## Writing tests for hardware that may not exist

CI runs on virtual machines with no battery, no sensors, no discrete GPU, sometimes no sound card,
and occasionally a single logical processor. Tests must pass there *and* on real hardware.

- Assert on invariants (non-null, correct type, ordering, a plausible range), not on specific values.
- Zero and empty collections are legitimate results almost everywhere. `getCpuVoltage() == 0` on a
  machine with no voltage sensor is correct behavior, not a failure.
- Beware timing-sensitive assertions comparing two live readings — CPU frequency, tick counters, and
  uptime all move between samples. That is the main source of flaky tests here.
- Any `oshi-core` test that allocates JNA needs
  `@DisabledIfSystemProperty(named = "os.name", matches = "(?i).*netbsd.*")`; the NetBSD CI runner
  has no `libjnidispatch`.

## Pull request conventions

- Branch from `master`; PRs target `master`.
- Add a `CHANGELOG.md` entry under *Next Release* **only for user-visible changes**. Pure
  refactors, dedup, test-only changes, and CI config get no entry.
- Do not add dependencies without discussion, and do not bump dependency versions by hand —
  Renovate handles that.
- Commit messages describe what changed and why. Do not add sign-off or `Co-Authored-By` trailers
  unless asked.
- Do not commit generated output, `target/`, or IDE files.

## Further reading

- [README.md](README.md) — features, usage, module overview
- [CONTRIBUTING.md](CONTRIBUTING.md) — fork/branch/PR workflow for humans
- [FAQ.md](FAQ.md) — platform quirks and common user problems; useful background on *why* a
  value looks odd on a given OS
- [PERFORMANCE.md](PERFORMANCE.md) — which calls are expensive and why; read before adding a
  native call to a hot path
- [UPGRADING.md](UPGRADING.md) — the breaking-change history and API compatibility policy
- [RELEASING.md](RELEASING.md) — the release process (maintainers only)
- [SECURITY.md](SECURITY.md) — vulnerability reporting

For worked examples of *using* the API — what a caller actually writes to reach a given piece of data —
read `oshi-demo` rather than asking for a walkthrough. It is kept current and covers the common shapes
(full system dump, per-process listing, JSON export, Swing and web front ends, jbang entry points). This
file deliberately does not duplicate that: it covers what an agent gets wrong *changing* the library, not
what a user writes to consume it.
