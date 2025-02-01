/*
 * @(#) ObjectAssemblerTest.kt
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
import io.kstuff.test.shouldBeType
import io.kstuff.test.shouldThrow

import io.kjson.JSONArray
import io.kjson.JSONBoolean
import io.kjson.JSONDecimal
import io.kjson.JSONException
import io.kjson.JSONInt
import io.kjson.JSONObject
import io.kjson.JSONString
import io.kjson.parser.ParseException
import io.kjson.parser.ParseOptions
import io.kjson.parser.ParserConstants.rootPointer

class ObjectAssemblerTest {

    @Test fun `should create empty object`() {
        val assembler = ObjectAssembler(ParseOptions.DEFAULT, rootPointer, 0)
        assembler.complete shouldBe false
        assembler.valid shouldBe false
        assembler.accept('}')
        assembler.complete shouldBe true
        assembler.valid shouldBe true
        with(assembler.value) {
            isEmpty() shouldBe true
        }
    }

    @Test fun `should create object with a single entry`() {
        val assembler = ObjectAssembler(ParseOptions.DEFAULT, rootPointer, 0)
        for (ch in "\"first\":0}") {
            assembler.complete shouldBe false
            assembler.valid shouldBe false
            assembler.accept(ch)
        }
        assembler.complete shouldBe true
        assembler.valid shouldBe true
        with(assembler.value) {
            size shouldBe 1
            this["first"] shouldBe JSONInt(0)
        }
    }

    @Test fun `should create object with two entries`() {
        val assembler = ObjectAssembler(ParseOptions.DEFAULT, rootPointer, 0)
        for (ch in """"first":1,"second":2}""") {
            assembler.complete shouldBe false
            assembler.valid shouldBe false
            assembler.accept(ch)
        }
        assembler.complete shouldBe true
        assembler.valid shouldBe true
        with(assembler.value) {
            size shouldBe 2
            this["first"] shouldBe JSONInt(1)
            this["second"] shouldBe JSONInt(2)
        }
    }

    @Test fun `should create object with complex mix of entries`() {
        val assembler = ObjectAssembler(ParseOptions.DEFAULT, rootPointer, 0)
        for (ch in """"first":"tiger","second":false,"third":123,"fourth":[111,222],"fifth":null,"last":7.5}""") {
            assembler.complete shouldBe false
            assembler.valid shouldBe false
            assembler.accept(ch)
        }
        assembler.complete shouldBe true
        assembler.valid shouldBe true
        with(assembler.value) {
            size shouldBe 6
            this["first"] shouldBe JSONString("tiger")
            this["second"] shouldBe JSONBoolean.FALSE
            this["third"] shouldBe JSONInt(123)
            with(this["fourth"]) {
                shouldBeType<JSONArray>()
                size shouldBe 2
                this[0] shouldBe JSONInt(111)
                this[1] shouldBe JSONInt(222)
            }
            this["fifth"] shouldBe null
            this["last"] shouldBe JSONDecimal("7.5")
        }
    }

    @Test fun `should fail on trailing comma when option not set`() {
        val assembler = ObjectAssembler(ParseOptions.DEFAULT, rootPointer, 0)
        for (ch in """ "a":111,"b":222, """)
            assembler.accept(ch)
        shouldThrow<ParseException>("Trailing comma in JSON object") {
            assembler.accept('}')
        }
    }

    @Test fun `should accept trailing comma when option set`() {
        val parseOptions = ParseOptions(objectTrailingComma = true)
        val assembler = ObjectAssembler(parseOptions, rootPointer, 0)
        for (ch in """ "a":111,"b":222,} """)
            assembler.accept(ch)
        assembler.valid shouldBe true
        assembler.complete shouldBe true
        with(assembler.value) {
            size shouldBe 2
            this["a"] shouldBe JSONInt(111)
            this["b"] shouldBe JSONInt(222)
        }
    }

    @Test fun `should fail on missing comma`() {
        val assembler = ObjectAssembler(ParseOptions.DEFAULT, "/test8", 1)
        for (ch in """ "a":1 """)
            assembler.accept(ch)
        shouldThrow<ParseException>("Missing comma in JSON object, at /test8") {
            assembler.accept('"')
        }
    }

    @Test fun `should throw error on duplicate key`() {
        val parseOptions = ParseOptions(objectKeyDuplicate = JSONObject.DuplicateKeyOption.ERROR)
        val assembler = ObjectAssembler(parseOptions, "/test9", 0)
        for (ch in "\"abc\":0,\"abc\":9")
            assembler.accept(ch)
        shouldThrow<JSONException>("Duplicate key, at /test9/abc") {
            assembler.accept('}')
        }
    }

    @Test fun `should allow duplicate key and take first instance`() {
        val parseOptions = ParseOptions(objectKeyDuplicate = JSONObject.DuplicateKeyOption.TAKE_FIRST)
        val assembler = ObjectAssembler(parseOptions, "/test9", 0)
        for (ch in """"abc":0,"abc":1}""")
            assembler.accept(ch)
        assembler.valid shouldBe true
        assembler.complete shouldBe true
        with(assembler.value) {
            size shouldBe 1
            this["abc"] shouldBe JSONInt(0)
        }
    }

    @Test fun `should allow duplicate key and take last instance`() {
        val parseOptions = ParseOptions(objectKeyDuplicate = JSONObject.DuplicateKeyOption.TAKE_LAST)
        val assembler = ObjectAssembler(parseOptions, "/test9", 0)
        for (ch in """"abc":0,"abc":1}""")
            assembler.accept(ch)
        assembler.valid shouldBe true
        assembler.complete shouldBe true
        with(assembler.value) {
            size shouldBe 1
            this["abc"] shouldBe JSONInt(1)
        }
    }

    @Test fun `should allow duplicate key if identical`() {
        val parseOptions = ParseOptions(objectKeyDuplicate = JSONObject.DuplicateKeyOption.CHECK_IDENTICAL)
        val assembler = ObjectAssembler(parseOptions, "/test9", 0)
        for (ch in """"abc":1,"abc":1}""")
            assembler.accept(ch)
        assembler.valid shouldBe true
        assembler.complete shouldBe true
        with(assembler.value) {
            size shouldBe 1
            this["abc"] shouldBe JSONInt(1)
        }
    }

}
