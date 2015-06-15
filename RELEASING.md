Releasing OSHI
=====================

### Prepare

* Make sure tests are green on [Travis CI](https://travis-ci.org/dblock/oshi).
* Update version in [pom.xml](pom.xml) using [Semantic Versioning](http://semver.org/) standards:
	* Increment major version (1.0) for API-breaking changes or additions
	* Increment minor version (1.1) for substantive additions, bugfixes and changes that are backwards compatible
	* Increment patch version (1.1.1) for minor bugfixes or changes that are backwards compatible
	* Note this incrementing may have already been done if development is on a -SNAPSHOT release

* Compile, package, and assemble
	* `mvn package -P dist`
	* Rename the binary zipfile (under `target` directory) oshi-(version).zip
* Deploy to the [Central Repository](https://oss.sonatype.org/)
	* `mvn clean deploy -P dist`
	* The above requires you to have previously:
		* Put your repository credentials in your Maven settings.xml file
		* Put your gpg certificate credentials in the settings.xml file
		* See [this page](http://central.sonatype.org/pages/apache-maven.html) for a summary of steps

### Release

* Change "Next" in [CHANGELOG.md](CHANGELOG.md) to this new version.
* Move "Your contribution here." to a new empty "Next" section
* Commit changes as a "prep for x.x release"

* On GitHub, [add a new release](https://github.com/dblock/oshi/releases/new).
	* include the binary zipfile

* Change the download link in [README.md](README.md) to point to the uploaded zipfile
	* Commit as an amended commit
	
* As development progresses, update version in [pom.xml](pom.xml) using -SNAPSHOT appended to the new version