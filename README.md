# kjson-stream

[![Build Status](https://travis-ci.com/pwall567/kjson-stream.svg?branch=main)](https://app.travis-ci.com/github/pwall567/kjson-stream)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/static/v1?label=Kotlin&message=v1.7.21&color=7f52ff&logo=kotlin&logoColor=7f52ff)](https://github.com/JetBrains/kotlin/releases/tag/v1.7.21)
[![Maven Central](https://img.shields.io/maven-central/v/io.kjson/kjson-stream?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.kjson%22%20AND%20a:%kjson-stream%22)

JSON Kotlin streaming library

## Background

The JSON parser in the [`kjson-core`](https://github.com/pwall567/kjson-core) library takes its input from a string of
JSON text in memory.
This pattern of operation does not suit all applications; there is often a requirement to parse incoming data on the fly
&ndash; that is, to accept a stream of characters and assemble them into JSON values (objects, arrays and primitive
elements).

The `kjson-stream` library provides two classes that operate in this manner:

- [`JSONStreamer`](#jsonstreamer) accepts characters which it uses to build a single JSON value, most often a JSON
  object.
- [`JSONPipeline`](#jsonpipeline) accepts characters which must take the form of a JSON array, and emits each member of
  the array, as it is completed, to a downstream receiving function.

## `pipelines` Library

The `kjson-stream` library makes use of the [`pipelines`](https://github.com/pwall567/pipelines) library.
This library provides a mechanism for working with streams of data &ndash; characters or more complex elements &ndash;
with `Acceptor` classes accepting a stream of data, and `Pipeline` classes both accepting the data and emitting a
transformed form of the data to a downstream `Acceptor`.

The `Pipeline` transformations may be one-to-one, one-to-many or many-to-one, and UTF-8 encoding or decoding provides a
good example.
UTF-8 encoding is both a one-to-one and a one-to-many transform (only the characters outside the ASCII range will
transform into more than one output byte); JSON parsing is clearly a many-to-one transform.

## `JSONStreamer`

The `JSONStreamer` class assembles input characters into a single value.
For example:
```kotlin
        val streamer = JSONStreamer()
        while (true) {
            val ch = reader.read() // reader is a Reader (FileReader, StringReader etc.)
            if (ch < 0)
                break
            streamer.accept(ch)
        }
        val json = streamer.result // json will be of type JSONValue?
```
One point to note is that the input to the `accept` function is an `Int`, not a `Char`.
This allows the `JSONStreamer` class to be used in a decoding pipeline with character set decoder, as follows:
```kotlin
        val streamer = UTF8_CodePoint(JSONStreamer())
        streamer.accept(inputStream) // an InputStream (FileInputStream, ByteArrayInputStream etc.)
        val json = streamer.result
```

To allow [lenient parsing](https://github.com/pwall567/kjson-core#lenient-parsing) as described in the
[`kjson-core`](https://github.com/pwall567/kjson-core) library, the `JSONStreamer` constructor takes an optional
`ParseOptions` parameter.

## `JSONPipeline`

The `JSONPipeline` class expects its input to be in the form of a JSON array, and it emits each array item in turn to
the downstream `Acceptor`.
Like other `Pipeline` classes it may be constructed with the downstream `Acceptor` as a parameter, but a more convenient
approach is to use the `pipeTo` function:
```kotlin
        val pipeline = JSONPipeline.pipeTo { processitem(it) }
```
The lambda will be called with each array item in turn.

As with `JSONStreamer`, a `ParseOptions` object may be passed as a parameter to the constructor or the `pipeTo` function
if required.

## Non-Blocking

Non-blocking versions of these classes are available, using the
[`co-pipelines`](https://github.com/pwall567/co-pipelines) library.

The `JSONCoStreamer` class operates in the same manner as `JSONStreamer`, except that the `accept` function is a suspend
function.
This is likely to be of little utility since the `accept` function does not invoke any non-blocking functions; it is
provided mainly to act as the terminal `CoAcceptor` in a pipeline.

The `JSONCoPipeline` class is much more interesting.
The downstream function which receives completed array items is called as a suspend function, meaning that each item in
the array may be processed as it arrives, in a non-blocking manner.

For example:
```kotlin
        val pipeline = JSONCoPipeline.pipeTo {
            invokeSuspendFunction(it)
        }
```

## Dependency Specification

The latest version of the library is 1.3, and it may be obtained from the Maven Central repository.

### Maven
```xml
    <dependency>
      <groupId>io.kjson</groupId>
      <artifactId>kjson-stream</artifactId>
      <version>1.3</version>
    </dependency>
```
### Gradle
```groovy
    implementation "io.kjson:kjson-stream:1.3"
```
### Gradle (kts)
```kotlin
    implementation("io.kjson:kjson-stream:1.3")
```

Peter Wall

2023-06-04
