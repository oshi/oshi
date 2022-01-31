Releasing OSHI
=====================
### Required JDK

* To perform a full release including the modular artifact you must have `JAVA_HOME` pointing to JDK 11 or higher.

### Credentials

* Put your [repository credentials in your Maven settings.xml file](https://central.sonatype.org/pages/apache-maven.html#distribution-management-and-authentication) for both snapshot and staging repositories in [pom.xml](pom.xml). 
* Put your [gpg certificate credentials in the settings.xml file](https://central.sonatype.org/pages/apache-maven.html#gpg-signed-components)

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
    * Copy [README.md](README.md) to [src/site/markdown/README.md](src/site/markdown/README.md)
        * HTML-escape `&`, `<`, and `>` in any links in the site version
    * Change "Next" or in-progress version in [CHANGELOG.md](CHANGELOG.md) to this new version.
    * Move "Your contribution here." to a new empty "Next" section
    * Commit changes as a "prep for x.x release"

### Release Non-Modular Artifacts

See [this page](https://central.sonatype.org/pages/apache-maven.html#performing-a-release-deployment-with-the-maven-release-plugin) for a summary of the below steps
* `mvn clean deploy`
    * Do a final snapshot release and fix any errors in the javadocs
    * If pom sorting or license headers are rewritten as part of this deployment, commit the changes
* `mvn release:clean`
    * Takes a few seconds
* `mvn release:prepare`
    * Takes a few minutes
    * This will ask for the version being released, removing -SNAPSHOT
    * This will suggest the next version, increment appropriately
* `mvn release:perform`
    * Takes a few minutes. 
    * This pushes the release to the [OSSRH](https://oss.sonatype.org/) staging repository
    * This also pushes to [gh_pages](https://oshi.github.io/oshi)
* Log on to [Nexus](https://oss.sonatype.org/) and [release the deployment from OSSRH to the Central Repository](https://central.sonatype.org/pages/releasing-the-deployment.html).
	
* Add a title and release notes [to the tag](https://github.com/oshi/oshi/tags) on GitHub and publish the release to make it current.

### Ongoing Maintenance

As development progresses, update version in [pom.xml](pom.xml) using -SNAPSHOT appended to the new version using [Semantic Versioning](https://semver.org/) standards:
* Increment major version (x.0) for API-breaking changes or additions
* Increment minor version (x.1) for substantive additions, bugfixes and changes that are backwards compatible
* Increment patch version (x.x.1) for minor bugfixes or changes that are backwards compatible