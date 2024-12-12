/*
 * @(#) ArrayAssemblerTest.kt
 *
 * kjson-stream  JSON Kotlin streaming library
 * Copyright (c) 2023, 2024 Peter Wall
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.kjson.stream

import kotlin.test.Test

import io.kstuff.test.shouldBe
import io.kstuff.test.shouldBeNonNull
import io.kstuff.test.shouldBeType
import io.kstuff.test.shouldThrow

import io.kjson.JSONArray
import io.kjson.JSONBoolean
import io.kjson.JSONDecimal
import io.kjson.JSONInt
import io.kjson.JSONString
import io.kjson.JSONValue
import io.kjson.parser.ParseException
import io.kjson.parser.ParseOptions
import io.kjson.parser.ParserConstants.rootPointer
import io.kjson.parser.ParserErrors.MAX_DEPTH_EXCEEDED

class ArrayAssemblerTest {

    @Test fun `should create empty array`() {
        val assembler = ArrayAssembler(ParseOptions.DEFAULT, rootPointer, 0)
        assembler.complete shouldBe false
        assembler.valid shouldBe false
        assembler.accept(']')
        assembler.complete shouldBe true
        assembler.valid shouldBe true
        with(assembler.value) {
            isEmpty() shouldBe true
        }
    }

    @Test fun `should create array with a single entry`() {
        val assembler = ArrayAssembler(ParseOptions.DEFAULT, rootPointer, 0)
        for (ch in "0]") {
            assembler.complete shouldBe false
            assembler.valid shouldBe false
            assembler.accept(ch)
        }
        assembler.complete shouldBe true
        assembler.valid shouldBe true
        with(assembler.value) {
            size shouldBe 1
            this[0] shouldBe JSONInt(0)
        }
    }

    @Test fun `should create array with two entries`() {
        val assembler = ArrayAssembler(ParseOptions.DEFAULT, rootPointer, 0)
        for (ch in "0,1]") {
            assembler.complete shouldBe false
            assembler.valid shouldBe false
            assembler.accept(ch)
        }
        assembler.complete shouldBe true
        assembler.valid shouldBe true
        with(assembler.value) {
            size shouldBe 2
            this[0] shouldBe JSONInt(0)
            this[1] shouldBe JSONInt(1)
        }
    }

    @Test fun `should create array with complex mix of entries`() {
        val assembler = ArrayAssembler(ParseOptions.DEFAULT, rootPointer, 0)
        for (ch in "\"zebra\",true,1,[],null,2.5]") {
            assembler.complete shouldBe false
            assembler.valid shouldBe false
            assembler.accept(ch)
        }
        assembler.complete shouldBe true
        assembler.valid shouldBe true
        with(assembler.value) {
            size shouldBe 6
            this[0] shouldBe JSONString("zebra")
            this[1] shouldBe JSONBoolean.TRUE
            this[2] shouldBe JSONInt(1)
            with(this[3]) {
                shouldBeNonNull()
                shouldBeType<JSONArray>()
                isEmpty() shouldBe true
            }
            this[4] shouldBe null
            this[5] shouldBe JSONDecimal("2.5")
        }
    }

    @Test fun `should accept whitespace between entries`() {
        val assembler = ArrayAssembler(ParseOptions.DEFAULT, rootPointer, 0)
        for (ch in " \"zebra\"   , true  , 1 , [ ]    , null ,  2.5] ")
            assembler.accept(ch)
        assembler.complete shouldBe true
        assembler.valid shouldBe true
        with(assembler.value) {
            size shouldBe 6
            this[0] shouldBe JSONString("zebra")
            this[1] shouldBe JSONBoolean.TRUE
            this[2] shouldBe JSONInt(1)
            with(this[3]) {
                shouldBeNonNull()
                shouldBeType<JSONArray>()
                isEmpty() shouldBe true
            }
            this[4] shouldBe null
            this[5] shouldBe JSONDecimal("2.5")
        }
    }

    @Test fun `should fail on invalid array content`() {
        val assembler = ArrayAssembler(ParseOptions.DEFAULT, rootPointer, 0)
        shouldThrow<ParseException>("Illegal JSON syntax, at /0") {
            assembler.accept('a')
        }
    }

    @Test fun `should fail on invalid array content later in array`() {
        val assembler = ArrayAssembler(ParseOptions.DEFAULT, rootPointer, 0)
        assembler.accept('0')
        assembler.accept(',')
        shouldThrow<ParseException>("Illegal JSON syntax, at /1") {
            assembler.accept('a')
        }
    }

    @Test fun `should fail on invalid content in nested array`() {
        val assembler = ArrayAssembler(ParseOptions.DEFAULT, rootPointer, 0)
        assembler.accept('[')
        shouldThrow<ParseException>("Illegal JSON syntax, at /0/0") {
            assembler.accept('a')
        }
    }

    @Test fun `should fail on missing closing bracket`() {
        val assembler = ArrayAssembler(ParseOptions.DEFAULT, rootPointer, 0)
        assembler.accept('0')
        shouldThrow<ParseException>("JSON array is incomplete") {
            assembler.value
        }
    }

    @Test fun `should fail on trailing comma when option not set`() {
        val assembler = ArrayAssembler(ParseOptions.DEFAULT, rootPointer, 0)
        assembler.accept('0')
        assembler.accept(',')
        shouldThrow<ParseException>("Trailing comma in JSON array") {
            assembler.accept(']')
        }
    }

    @Test fun `should accept trailing comma when option set`() {
        val parseOptions = ParseOptions(arrayTrailingComma = true)
        val assembler = ArrayAssembler(parseOptions, rootPointer, 0)
        assembler.accept('0')
        assembler.accept(',')
        assembler.accept(']')
        assembler.valid shouldBe true
        assembler.complete shouldBe true
        with(assembler.value) {
            size shouldBe 1
            this[0] shouldBe JSONInt(0)
        }
    }

    @Test fun `should fail on missing comma`() {
        val assembler = ArrayAssembler(ParseOptions.DEFAULT, "/test9", 1)
        assembler.accept('0')
        assembler.accept(' ')
        shouldThrow<ParseException>("Missing comma in JSON array, at /test9") {
            assembler.accept('0')
        }
    }

    @Test fun `should allow nesting up to maximum depth`() {
        val parseOptions = ParseOptions(maximumNestingDepth = 50)
        val assembler = ArrayAssembler(parseOptions, rootPointer, 0)
        repeat(49) { assembler.accept('[') }
        assembler.accept('1')
        repeat(50) { assembler.accept(']') }
        assembler.valid shouldBe true
        assembler.complete shouldBe true
        var result: JSONValue? = assembler.value
        for (i in 0 until parseOptions.maximumNestingDepth) {
            result.shouldBeType<JSONArray>()
            result = result[0]
        }
        result shouldBe JSONInt(1)
    }

    @Test fun `should throw exception on nesting depth exceeded`() {
        val parseOptions = ParseOptions(maximumNestingDepth = 50)
        val assembler = ArrayAssembler(parseOptions, rootPointer, 0)
        repeat(49) { assembler.accept('[') }
        shouldThrow<ParseException>(MAX_DEPTH_EXCEEDED) {
            assembler.accept('[')
        }.let {
            it.text shouldBe MAX_DEPTH_EXCEEDED
            it.pointer shouldBe rootPointer
        }
    }

    @Test fun `should throw exception on nesting depth exceeded 2`() {
        val parseOptions = ParseOptions(maximumNestingDepth = 2)
        val assembler = ArrayAssembler(parseOptions, rootPointer, 0)
        assembler.accept('[')
        shouldThrow<ParseException>(MAX_DEPTH_EXCEEDED) {
            assembler.accept('[')
        }.let {
            it.text shouldBe MAX_DEPTH_EXCEEDED
            it.pointer shouldBe rootPointer
        }
    }

}
