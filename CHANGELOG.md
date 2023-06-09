# Change Log

The format is based on [Keep a Changelog](http://keepachangelog.com/).

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
