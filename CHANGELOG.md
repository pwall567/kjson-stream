# Change Log

The format is based on [Keep a Changelog](http://keepachangelog.com/).

## [3.6] - 2025-11-13
### Changed
- `pom.xml`: changed parent POM to `io.kstuff:kstuff-maven:2.0` and added `groupId`
- `deploy.yml`: switched upload target to `central`
- `pom.xml`: updated dependency versions

## [3.5] - 2025-06-09
### Changed
- `pom.xml`: updated dependency version

## [3.4] - 2025-06-07
### Changed
- `pom.xml`: updated dependency versions

## [3.3] - 2025-02-01
### Changed
- `pom.xml`: updated Kotlin version to 2.0.21, updated dependency versions

## [3.2] - 2024-12-12
### Changed
- `pom.xml`: updated dependency version
- tests : switched to `should-test` library

## [3.1] - 2024-08-17
### Changed
- `pom.xml`: updated dependency version

## [3.0] - 2024-07-22
### Changed
- `ObjectAssembler`: converted to use duplicate key check in `JSONObject` (breaking change)

## [2.0] - 2024-07-09
### Added
- `build.yml`, `deploy.yml`: converted project to GitHub Actions
### Changed
- `pom.xml`: updated dependency versions, upgraded Kotlin version to 1.9.24
### Removed
- `.travis.yml`

## [1.12] - 2024-02-18
### Changed
- `pom.xml`: updated dependency version

## [1.11] - 2024-02-14
### Changed
- `pom.xml`: updated dependency version

## [1.10] - 2024-02-11
### Changed
- `pom.xml`: updated dependency version

## [1.9] - 2024-01-01
### Changed
- `pom.xml`: updated dependency version

## [1.8] - 2023-12-31
### Changed
- `pom.xml`: updated dependency version

## [1.7] - 2023-12-11
### Changed
- `NumberAssembler`: optimised decimal conversion
- `JSONStreamer`: optimised reading from `Reader`
- `pom.xml`: updated dependency versions

## [1.6] - 2023-09-25
### Changed
- `pom.xml`: updated dependency version

## [1.5] - 2023-07-24
### Changed
- `pom.xml`: updated Kotlin version to 1.8.22
- `pom.xml`: updated dependency version

## [1.4] - 2023-07-05
### Added
- `JSONLinesPipeline`, `JSONLinesCoPipeline`
### Changed
- `JSONStreamer`: added `parse()` function
- `JSONCoStreamer`: removed duplicated code
- `pom.xml`: updated dependency versions
- `ArrayAssembler`, `ObjectAssembler`, `JSONCoPipeline`, `JSONCoStreamer`, `JSONPipeline`, `JSONStreamer`: added check
  for excessive nesting
- `ArrayAssembler`, `ObjectAssembler`: switched to use Builder classes

## [1.3] - 2023-06-04
### Changed
- `pom.xml`: updated dependency version

## [1.2] - 2023-05-21
### Changed
- `pom.xml`: updated dependency versions

## [1.1] - 2023-05-07
### Changed
- `pom.xml`: updated dependency versions

## [1.0] - 2023-04-25
### Changed
- `pom.xml`: updated dependency versions, promoted to version 1.0

## [0.3] - 2023-04-23
### Changed
- `pom.xml`: updated dependency versions

## [0.2] - 2023-04-16
### Added
- `JSONCoStreamer`, `JSONCoPipeline`, `CoAcceptorAdapter`: added non-blocking capability

## [0.1] - 2023-04-16
### Added
- all files: initial versions
