Releasing OSHI
=====================

### Credentials

* Put your [repository credentials in your Maven settings.xml file](https://central.sonatype.org/pages/apache-maven.html#distribution-management-and-authentication) for both snapshot and staging repositories in [pom.xml](pom.xml).
* Put your [gpg certificate credentials in the settings.xml file](https://central.sonatype.org/pages/apache-maven.html#gpg-signed-components)
* The `central` server in `settings.xml` is also used by the upload script (see below).

### Snapshots

* Other than during releases, the version number in the pom.xml should end in -SNAPSHOT
* A GitHub Action deploys snapshots for pushes to the master branch. Snapshot releases may also be
manually deployed using `mvn clean deploy`

### Prepare

* Make sure tests pass on all configured CI operating systems.
* Manually run tests on any non-CI-covered OS using `mvn clean test`.
* Review [SonarQube](https://sonarcloud.io/dashboard?id=com.github.oshi%3Aoshi-parent) for any bugs.
* Run the javac lint sweep described under [Pre-release bug hunt](#pre-release-bug-hunt) below.
* Choose an appropriate [version number](https://semver.org/) for the release
    * Proactively change version numbers in the download links on [README.md](README.md).
    * Copy `README.md` to `src/site/markdown/index.md`, then apply the site transformations below.
      The site is published at `oshi.ooo`, where repo-relative paths do not resolve, so a plain copy
      ships broken links. Re-copy the **whole file** rather than hand-editing the changed lines —
      every drift found so far came from partial syncs.
        * HTML-escape `&`, `<`, and `>` inside markdown link targets — but **not** inside fenced code
          blocks (`git clone ... && cd oshi` must stay unescaped)
        * Rewrite repo-relative links to full `https://github.com/oshi/oshi/blob/master/...` URLs:
          `FAQ.md`, `PERFORMANCE.md`, `UPGRADING.md`, `CONTRIBUTING.md`, `RELEASING.md`,
          `SECURITY.md`, `SUPPORT.md`, `oshi-demo/`, `oshi-metrics/`, `oshi-benchmark/`, and
          `oshi-core/src/test/java/oshi/SystemInfoTest.java` (keep any `#anchor` suffix)
        * Rewrite links to docs that are themselves site pages to their sibling `.html`:
          `src/site/markdown/SampleOutput.md` → `SampleOutput.html`, and
          `src/site/resources/Projects.md` → `Projects.html`
        * Verify: `wc -l` should match between the two files, and every line in
          `diff README.md src/site/markdown/index.md` should be explained by one of the
          transformations above
    * Change release dates and in-progress versions in `CHANGELOG.md`
    * Move "Your contribution here" to a new empty "In Progress" section
    * Commit changes as a "x.x release" (no need to push upstream yet)
* Verify the release-profile build before tagging:
    ```sh
    mvn -Prelease clean install
    ```
    * The `attach-javadocs` execution binds `javadoc:jar`, which lives **only in the `release`
      profile** and is therefore not exercised by any routine build. It is stricter than the
      `javadoc:javadoc` report goal used in the normal gate: it fails on unresolved `{@link}`
      references that the report goal tolerates. A missing import for a type referenced only in
      javadoc is the usual culprit, and it will not surface until `release:perform`.
    * **Always `clean` first.** The javadoc goals skip when their output is already present, so a
      re-run without `clean` can report `BUILD SUCCESS` without checking anything.

### Pre-release bug hunt

Error Prone, SonarCloud, Coverity, and the `oshi.comparison` twin-agreement tests all run on every
PR, so they need no pre-release step. The one bug-hunting signal that is **not** continuously
enforced is javac's own `-Xlint`, and it is worth sweeping once per release.

It is deliberately not wired into the build. `-Xlint:all` expands to a different set of categories on
every JDK, and an unrecognized category is a hard `error: invalid flag` rather than a warning — so it
cannot be suppressed, and a blocking config would break the *oldest* JDK in the matrix rather than
the newest. `restricted` requires JDK 22+, `this-escape` requires 21+, and the cfarm Solaris SPARC
job builds on 17. Run it by hand instead, forking the compiler so javac reads the environment
variable:

```sh
JDK_JAVAC_OPTIONS="-Xlint:all -Xmaxwarns 100000" \
    ./mvnw clean install -DskipTests -Dmaven.compiler.fork=true > /tmp/lint.log 2>&1
grep '^\[WARNING\]' /tmp/lint.log | sed -E 's/.*\[([a-z-]+)\].*/\1/' | sort | uniq -c | sort -rn
```

* **`-Xmaxwarns` is not optional.** javac stops after 100 warnings *per compilation* by default and
  says nothing about it, which hid 48% of the findings (237 reported of 457 real) the first time this
  was surveyed — including both of the only two genuine bugs it found. A per-module count of exactly
  100 is truncation, not a result.
* **`clean` is not optional either**, or incremental compilation recompiles nothing and cheerfully
  reports zero warnings.
* Use a JDK 25+ toolchain, or `oshi-core-ffm`, `oshi-benchmark`, and `oshi-dist` are silently skipped.
  Findings are *not* OS-dependent — javac compiles every platform's sources on every host — so a
  single sweep on any one machine covers all supported platforms.
* **The value is the delta, not the total.** As of 7.5.0 the expected result is 455 findings in
  exactly three categories, all structural and all deliberate:

  | Category | Count | Why it is expected |
  |---|---|---|
  | `restricted` | 285 | FFM downcall and `reinterpret` calls, which are the entire point of `oshi-core-ffm`; the runtime side is declared by the native-access flag |
  | `this-escape` | 149 | Memoized `this::method` suppliers, invoked only after construction, plus abstract accessors whose implementations return stateless singletons |
  | `exports` | 21 | Public `oshi.ffm` methods returning types from non-exported packages, which [AGENTS.md](AGENTS.md) already declares off-limits to dependents |

  Anything in a fourth category is new and worth reading. The 7.5.0 sweep surfaced two real defects
  this way — a missing `serialVersionUID` and a raw `List` array — both of which had been hidden
  under the 100-warning cap through several earlier passes.

### Release

* To perform a full release including the FFM artifact you must have `JAVA_HOME` pointing to JDK 25 or higher.

See [this page](https://central.sonatype.org/pages/apache-maven.html#performing-a-release-deployment-with-the-maven-release-plugin) for a summary of the below steps
* `mvn clean deploy`
    * Do a final snapshot release
    * Note this does **not** activate the `release` profile, so it will not catch the javadoc-jar
      errors described in the Prepare section — run `mvn -Prelease clean install` for those
    * If pom sorting or license headers are rewritten as part of this deployment, commit the changes
* `mvn release:clean`
    * Takes a few seconds
* `mvn release:prepare`
    * Takes a few minutes
    * This will ask for the version being released, removing -SNAPSHOT
    * This will suggest the next version, increment appropriately
* `mvn release:perform`
    * Takes a few minutes
    * This builds the release from the tag in `target/checkout`, creates `central-bundle.zip`, and uploads it to the Central Portal
    * This also pushes to [gh_pages](https://oshi.github.io/oshi)
    * **If the upload fails** (e.g. broken pipe), the bundle is preserved at `target/checkout/target/central-publishing/central-bundle.zip`. Upload it manually:
      ```sh
      ./scripts/upload-to-central.sh
      ```
      This reads credentials from `settings.xml` and uploads with retry/timeout handling. The deployment is created as `USER_MANAGED` for review before publishing.
* Log on to [Central Portal](https://central.sonatype.com/publishing) and publish the validated deployment (if the automatic publish did not succeed).

* Release the site; this can be done anytime after `release:prepare`:
    * Create/reset/rebase your local `site` branch to the just-released tag
    * Push your local `site` branch upstream

* Add a title and release notes [to the tag](https://github.com/oshi/oshi/tags) on GitHub and publish the release to make it current.
    * Publishing fires the `Attach dist archive to GitHub Release` workflow, which rebuilds
      `oshi-dist-x.x.x.zip` from the tag and uploads it as a release asset. **The workflow must
      already be on master before `release:prepare` cuts the tag**, or the checked-out tag will not
      contain it and nothing will fire.
    * If the workflow fails or the tag predates it, re-run it with an explicit tag:
      ```sh
      gh workflow run dist-release.yaml -f tag=oshi-parent-x.x.x
      ```
    * Or upload by hand from the `release:perform` build, which runs in `target/checkout`:
      ```sh
      gh release upload oshi-parent-x.x.x target/checkout/oshi-dist/target/oshi-dist-x.x.x.zip
      ```
    * The rebuilt jars are functionally identical to Central's but not byte-identical: the manifest
      carries a `Build-Time` and no `project.build.outputTimestamp` is set, so checksums differ.
      Central remains authoritative for the individual jars, but starting with 7.5.1 the zip itself
      is no longer published there (see `excludeArtifacts` in the parent pom), so **this release
      asset is its only distribution point** — confirm the upload succeeded before announcing.

### Ongoing Maintenance

As development progresses, update version in [pom.xml](pom.xml) using -SNAPSHOT appended to the new version using [Semantic Versioning](https://semver.org/) standards:
* Increment major version (x.0) for API-breaking changes or additions
* Increment minor version (x.1) for substantive additions, bugfixes and changes that are backwards compatible
* Increment patch version (x.x.1) for minor bugfixes or changes that are backwards compatible
