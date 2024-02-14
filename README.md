# kjson-stream

[![Build Status](https://travis-ci.com/pwall567/kjson-stream.svg?branch=main)](https://app.travis-ci.com/github/pwall567/kjson-stream)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/static/v1?label=Kotlin&message=v1.8.22&color=7f52ff&logo=kotlin&logoColor=7f52ff)](https://github.com/JetBrains/kotlin/releases/tag/v1.8.22)
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

## Streaming Input

### `JSONStreamer`

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

For those cases where the `JSONStreamer` is required to read the entire contents of a `Reader`:
```kotlin
        val json = JSONStreamer.parse(reader)
```

The result is a `JSONValue?`,  the same as would be obtained by calling `JSON.parse(reader.readText())`, but this
approach does not require the allocation of a `String` in memory to hold the entire JSON text.

To allow [lenient parsing](https://github.com/pwall567/kjson-core#lenient-parsing) as described in the
[`kjson-core`](https://github.com/pwall567/kjson-core) library, the `JSONStreamer` constructor and the `parse()`
function both take an optional `ParseOptions` parameter.

### `JSONPipeline`

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

### `JSONCoStreamer`

The `JSONCoStreamer` class operates in the same manner as `JSONStreamer`, except that the `accept` function is a suspend
function.
This is likely to be of little utility since the `accept` function does not invoke any non-blocking functions; it is
provided mainly to act as the terminal `CoAcceptor` in a pipeline.

### `JSONCoPipeline`

The `JSONCoPipeline` class is much more interesting.
The downstream function which receives completed array items is called as a suspend function, meaning that each item in
the array may be processed as it arrives, in a non-blocking manner.

For example:
```kotlin
        val pipeline = JSONCoPipeline.pipeTo {
            invokeSuspendFunction(it)
        }
```
Code in a different coroutine may now send data to `pipeline`, and the suspend function will be invoked with each
completed array item.

## JSON Lines

The [JSON Lines](https://jsonlines.org/) specification allows multiple JSON values to be specified in a single stream of
data, separated by newline (`\u000a`) characters.
For example, events may be logged to a file as a sequence of objects on separate lines; the alternative would be to
output a JSON array, but this would require a "`]`" terminator, complicating the shutdown of the process (particularly
abnormal shutdown).

```json lines
{"time":"2023-06-24T12:24:10.321+10:00","eventType":"ACCOUNT_OPEN","accountNumber": "123456789"}
{"time":"2023-06-24T12:24:10.321+10:00","eventType":"DEPOSIT","accountNumber": "123456789","amount":"1000.00"}
```

The individual items are usually objects (or sometimes arrays) formatted similarly, but that is not a requirement
&ndash; the items may be of any JSON type.

The `kjson-stream` library includes classes to process JSON Lines input in a streaming manner.

### `JSONLinesPipeline`

The `JSONLinesPipeline` is similar to `JSONPipeline`, except that it expects its input to take the form of a JSON Lines
data stream rather than a JSON array.
Like `JSONPipeline`, it can be instantiated using a constructor with an `Acceptor` parameter, or by the `pipeTo`
function:
```kotlin
        val pipeline = JSONLinesPipeline.pipeTo { processitem(it) }
```
The lambda will be invoked with each individual JSON value (note that a JSON Lines item may be the keyword "`null`", in
which case the parameter to the lambda will be `null`).

### `JSONLinesCoPipeline`

`JSONLinesCoPipeline` is the non-blocking equivalent of `JSONLinesPipeline`.
It can be instantiated using a constructor with a `CoAcceptor` parameter, or by the `pipeTo` function, which in this
case takes a `suspend` lambda:
```kotlin
        val pipeline = JSONLinesCoPipeline.pipeTo {
            invokeSuspendFunction(it)
        }
```

## Dependency Specification

The latest version of the library is 1.11, and it may be obtained from the Maven Central repository.

### Maven
```xml
    <dependency>
      <groupId>io.kjson</groupId>
      <artifactId>kjson-stream</artifactId>
      <version>1.11</version>
    </dependency>
```
### Gradle
```groovy
    implementation "io.kjson:kjson-stream:1.11"
```
### Gradle (kts)
```kotlin
    implementation("io.kjson:kjson-stream:1.11")
```

Peter Wall

2024-02-14
