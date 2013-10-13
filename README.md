OSHI
====

Oshi is a free JNA-based (native) operating system information library for Java. It doesn't require any additional native DLLs and aims to provide a cross-platform implementation to retrieve system information, such as version, memory, CPU, disk, etc.

Download
--------

* [Oshi 1.1](http://code.dblock.org/downloads/oshi/oshi-1.1.zip)

Where are we?
-------------

Oshi is a very young project. We'd like *you* to contribute a *nix port. Read the [project intro](http://code.dblock.org/introducing-oshi-operating-system-and-hardware-information-java).

Sample Output
-------------

Here's sample test output:

    Microsoft Windows 7
    2 CPU(s):
     Intel(R) Core(TM)2 Duo CPU T7300  @ 2.00GHz
     Intel(R) Core(TM)2 Duo CPU T7300  @ 2.00GHz
    Memory: 532.1 MB/2.0 GB

How is this different from ...
------------------------------

* [Sigar](http://www.hyperic.com/products/sigar): Sigar is GPL. Oshi is distributed under the MIT license. Oshi also uses [JNA](https://github.com/twall/jna) and doesn't require a native DLL to be installed.

