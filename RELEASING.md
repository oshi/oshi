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

### Ongoing Maintenance

As development progresses, update version in [pom.xml](pom.xml) using -SNAPSHOT appended to the new version using [Semantic Versioning](https://semver.org/) standards:
* Increment major version (x.0) for API-breaking changes or additions
* Increment minor version (x.1) for substantive additions, bugfixes and changes that are backwards compatible
* Increment patch version (x.x.1) for minor bugfixes or changes that are backwards compatible
