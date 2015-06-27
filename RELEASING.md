Releasing OSHI
=====================

### Prepare

* Make sure tests are green on [Travis CI](https://travis-ci.org/dblock/oshi).
* Update version in [pom.xml](pom.xml) using [Semantic Versioning](http://semver.org/) standards:
	* Increment major version (1.0) for API-breaking changes or additions
	* Increment minor version (1.1) for substantive additions, bugfixes and changes that are backwards compatible
	* Increment patch version (1.1.1) for minor bugfixes or changes that are backwards compatible
	* If incrementing was already been done on a -SNAPSHOT release, remove the -SNAPSHOT suffix
	* Change the download link in [README.md](README.md) to point to the new version
	* Copy [README.md](README.md) to [src/site/markdown/README.md](src/site/markdown/README.md)
	* Change "Next" or in-progress version in [CHANGELOG.md](CHANGELOG.md) to this new version.
	* Move "Your contribution here." to a new empty "Next" section
	* Commit changes as a "prep for x.x release"

### Build site

* `mvn site`
* `mvn site:deploy` --> Will push to gh pages (https://dblock.github.io/oshi)

### Build code

* `mvn clean install`

### Release

* `mvn release:clean`
* `mvn release:prepare`
* `mvn release:perform`

### Package and assemble
* `mvn package -P dist`
* Rename the binary zipfile (under `target` directory) oshi-(version).zip

### Deploy to the [Central Repository](https://oss.sonatype.org/)
* `mvn clean deploy -P dist`
* The above requires you to have previously:
	* Put your repository credentials in your Maven settings.xml file
	* Put your gpg certificate credentials in the settings.xml file
	* See [this page](http://central.sonatype.org/pages/apache-maven.html) for a summary of steps

### Tag Release
* On GitHub, [add a new release](https://github.com/dblock/oshi/releases/new).
	* include the binary zipfile
	
* As development progresses, update version in [pom.xml](pom.xml) using -SNAPSHOT appended to the new version