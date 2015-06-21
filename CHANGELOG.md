Next
====

* [#50](https://github.com/dblock/oshi/pull/50): Added file store information - [@dbwiddis](https://github.com/dbwiddis).
* Your contribution here.

1.3 (in-progress)
-----------------
* See site page for individual commits going forwards as only major changes will be mentioned manually in change log.
* Upgraded to java 7 base support
* Upgraded JNA to 4.1.0
* Bringing all lessons learned from waffle over to oshi in one go.
  * Additionally raised to java 1.6.
  * Assembly is now performed on all builds.
  * Use sonatype parent
  * Added inception year
  * Added organization
  * Require Maven 3.2.3 or better
  * Added issues management
  * Added ci management
  * Added distribution for site pages to github gh pages
  * Added copyright
  * Added maven properties for java version (source and test side)
  * Added maven build timestamp
  * Set project and reporting encoding to UTF-8
  * Added entire default plugin set to control the build level of each even if not otherwise used.  This is intended to override mavens hidden base parent
  * Set incremental compilation at false due to maven bug and note the jira ticket
  * Set site page to allow markdown usage in order to show readme
  * Switch from legacy emma code coverage to jacoco code coverage
  * Add all the various reports such as javadocs, checkstyles, findbugs, pmd, nvd scan for security, etc
  * Add patch for git commit id for manifest along with jira to same issue
  * Add build time, copyright, os informaton, compiler information to manifest for all resulting builds
  * Remove rather manual pieces of release to nexus
  * Keep gpg signing in case we need to use deploy when maven release plugin fails so we can get to sonatype regardless

1.2 (6/13/2014)
================

* Added TODO list and enhanced README documentation - [@ptitvert](https://github.com/ptitvert)
* Added Travis-CI - [@dblock](https://github.com/dblock).
* [#3](https://github.com/dblock/oshi/pull/3): Mavenized project - [@le-yams](https://github.com/le-yams).
* [#5](https://github.com/dblock/oshi/pull/5): Added Linux support - [@ptitvert](https://github.com/ptitvert).
* [#7](https://github.com/dblock/oshi/pull/7): Added Mac OS X Support - [@ptitvert](https://github.com/ptitvert).
* [#13](https://github.com/dblock/oshi/pull/13): Support for Windows 8.1 and Windows Server 2008 R2 - [@NagyGa1](https://github.com/NagyGa1).
* [#15](https://github.com/dblock/oshi/pull/15), [#18](https://github.com/dblock/oshi/pull/18): Added support for CPU load - [@kamenitxan](https://github.com/kamenitxan), [@Sorceror](https://github.com/Sorceror).
* [#25](https://github.com/dblock/oshi/pull/25), [#29](https://github.com/dblock/oshi/pull/29): Included inactive/reclaimable memory amount in GlobalMemory#getAvailable on Mac/Linux - [@dbwiddis](https://github.com/dbwiddis).
* [#27](https://github.com/dblock/oshi/pull/27): Replaced all Mac OS X command line parsing with JNA or System properties - [@dbwiddis](https://github.com/dbwiddis).
* [#30](https://github.com/dblock/oshi/pull/30): Added processor vendor frequency information - [@alessiofachechi](https://github.com/alessiofachechi).
* [#32](https://github.com/dblock/oshi/pull/32): Added battery state information - [@dbwiddis](https://github.com/dbwiddis).

1.1 (10/13/2013)
================

* Added support for Windows 8 to `oshi.software.os.OperatingSystemVersion`, `oshi.software.os.windows.nt.OSVersionInfoEx` - [@laurent-r](https://github.com/laurent-r).

1.0 (6/23/2010)
===============

* Initial public release - [@dblock](https://github.com/dblock).
