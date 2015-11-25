# Guide to upgrading from Oshi 1.x to 2.x

Even though its a major release, Oshi 2.0 functionality is identical to 1.5.2.
For the most part, the highest level APIs demonstrated in the test classes
have remained the same.  Several lower level packages and classes have been
moved and/or renamed.

## Package Changes

New packages `oshi.hardware.platform.*` were created and contain the platform-
specific implementations of interfaces in `oshi.hardware`, with implementing
classes renamed to prepend the platform name to the interface name.  Similar
renaming was done for implementations of `oshi.software.os` in the respective
`oshi.software.os.*` packages.

## Application Changes

(In Progress -- Processor singular vs. plural, see Issue #102)
