# Java Toolchain Policy

This project separates the JDK used to run the build from the Java and Android
APIs that app code may use.

## Current Policy

- CI currently runs Gradle with JDK 17.
- App Java source and target compatibility are Java 17.
- Android runtime API availability is still bounded by `minSdk`.
- Do not use Java library APIs unless they are Android `minSdk`-safe or
  explicitly supported by desugaring.

## JDK Runtime vs App API

Changing the CI or local Gradle runtime from JDK 17 to JDK 21 may be a valid
toolchain maintenance task. It does not automatically allow app code to call
newer Android runtime APIs.

Example: `Stream#toList()` compiles on modern JDKs, but Android Lint may still
flag it as requiring a newer Android API level. Use `Collectors.toList()` or an
explicit list construction when the app must stay compatible with older
Android versions.

## Upgrade Guidance

If moving the build runtime to JDK 21:

1. Change only the CI / Gradle runtime first.
2. Keep `sourceCompatibility` and `targetCompatibility` at Java 17 unless there
   is a concrete source-language need.
3. Run both debug flavor builds, full unit tests, lint, and release assembly.
4. Treat any Android Lint `NewApi` issue as an Android API compatibility issue,
   not as evidence that the build JDK is too old.

## Packaging Notes

JDK runtime upgrades are not a packaging optimization by themselves. APK size
and structure are affected more directly by:

- R8 and keep rules
- resource shrinking
- dependency graph size
- native libraries and legacy JNI packaging
- release build configuration
- LSPosed metadata and flavor-specific assets
