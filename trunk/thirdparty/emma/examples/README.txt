
The default ("all") target in both ANT build files in this directory is
equivalent to compiling and running some simple code in Main.java. To enable
coverage, simply add "emma" target before all other ANT targets:

ant -buildfile build-onthefly.xml emma all

or:

ant -buildfile build-offline.xml emma all

The two build files demonstrate two different ways of using EMMA and have
detailed comments. For a step-by-step explanation of what they do, see the
user guide in the "docs" directory.

To restore each example to a clean state, execute:

ant -buildfile build-onthefly.xml clean

or:

ant -buildfile build-offline.xml clean
