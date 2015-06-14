Releasing OSHI
=====================

### Prepare

* Make sure tests are green on [Travis CI](https://travis-ci.org/dblock/oshi).
* Update version in [pom.xml](pom.xml) using [Semantic Versioning](http://semver.org/) standards:
** Increment major version (1.0) for API-breaking changes or additions
** Increment minor version (1.1) for substantive additions, bugfixes and changes that are backwards compatible
** Increment patch version (1.1.1) for minor bugfixes or changes that are backwards compatible

* Compile, package, and assemble
** `mvn package -P dist`
** Rename the binary zipfile (under `target` directory) oshi-(version).zip
* Sign files to be uploaded 
** `gpg -ab pom.xml`
** `gpg -ab oshi-core-1.x.jar`
** `gpg -ab oshi-core-1.x-javadoc.jar`
** `gpg -ab oshi-core-1.x-sources.jar`
* Log in to the [Central Repository](https://oss.sonatype.org/)
** (Additional steps to be added.)


### Release

* On GitHub, [add a new release](https://github.com/dblock/oshi/releases/new).
** include the binary zipfile

* Change the download link in [README.md](README.md) to point to the uploaded zipfile
* Change "Next" in [CHANGELOG.md](CHANGELOG.md) to this new version.
* Move "Your contribution here." to a new empty "Next" section
* Amend these changes to the bump commit `git commit -a --amend --no-edit`
