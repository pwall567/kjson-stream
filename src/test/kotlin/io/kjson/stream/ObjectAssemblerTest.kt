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
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.expect

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
        assertFalse(assembler.complete)
        assertFalse(assembler.valid)
        assembler.accept('}')
        assertTrue(assembler.complete)
        assertTrue(assembler.valid)
        with(assembler.value) {
            assertTrue(isEmpty())
        }
    }

    @Test fun `should create object with a single entry`() {
        val assembler = ObjectAssembler(ParseOptions.DEFAULT, rootPointer, 0)
        for (ch in "\"first\":0}") {
            assertFalse(assembler.complete)
            assertFalse(assembler.valid)
            assembler.accept(ch)
        }
        assertTrue(assembler.complete)
        assertTrue(assembler.valid)
        with(assembler.value) {
            expect(1) { size }
            expect(JSONInt(0)) { this["first"] }
        }
    }

    @Test fun `should create object with two entries`() {
        val assembler = ObjectAssembler(ParseOptions.DEFAULT, rootPointer, 0)
        for (ch in """"first":1,"second":2}""") {
            assertFalse(assembler.complete)
            assertFalse(assembler.valid)
            assembler.accept(ch)
        }
        assertTrue(assembler.complete)
        assertTrue(assembler.valid)
        with(assembler.value) {
            expect(2) { size }
            expect(JSONInt(1)) { this["first"] }
            expect(JSONInt(2)) { this["second"] }
        }
    }

    @Test fun `should create object with complex mix of entries`() {
        val assembler = ObjectAssembler(ParseOptions.DEFAULT, rootPointer, 0)
        for (ch in """"first":"tiger","second":false,"third":123,"fourth":[111,222],"fifth":null,"last":7.5}""") {
            assertFalse(assembler.complete)
            assertFalse(assembler.valid)
            assembler.accept(ch)
        }
        assertTrue(assembler.complete)
        assertTrue(assembler.valid)
        with(assembler.value) {
            expect(6) { size }
            expect(JSONString("tiger")) { this["first"] }
            expect(JSONBoolean.FALSE) { this["second"] }
            expect(JSONInt(123)) { this["third"] }
            with(this["fourth"]) {
                assertTrue(this is JSONArray)
                expect(2) { size }
                expect(JSONInt(111)) { this[0] }
                expect(JSONInt(222)) { this[1] }
            }
            assertNull(this["fifth"])
            expect(JSONDecimal("7.5")) { this["last"] }
        }
    }

    @Test fun `should fail on trailing comma when option not set`() {
        val assembler = ObjectAssembler(ParseOptions.DEFAULT, rootPointer, 0)
        for (ch in """ "a":111,"b":222, """)
            assembler.accept(ch)
        assertFailsWith<ParseException> { assembler.accept('}') }.let {
            expect("Trailing comma in JSON object") { it.message }
        }
    }

    @Test fun `should accept trailing comma when option set`() {
        val parseOptions = ParseOptions(objectTrailingComma = true)
        val assembler = ObjectAssembler(parseOptions, rootPointer, 0)
        for (ch in """ "a":111,"b":222,} """)
            assembler.accept(ch)
        assertTrue(assembler.valid)
        assertTrue(assembler.complete)
        with(assembler.value) {
            expect(2) { size }
            expect(JSONInt(111)) { this["a"] }
            expect(JSONInt(222)) { this["b"] }
        }
    }

    @Test fun `should fail on missing comma`() {
        val assembler = ObjectAssembler(ParseOptions.DEFAULT, "/test8", 1)
        for (ch in """ "a":1 """)
            assembler.accept(ch)
        assertFailsWith<ParseException> { assembler.accept('"') }.let {
            expect("Missing comma in JSON object, at /test8") { it.message }
        }
    }

    @Test fun `should throw error on duplicate key`() {
        val parseOptions = ParseOptions(objectKeyDuplicate = JSONObject.DuplicateKeyOption.ERROR)
        val assembler = ObjectAssembler(parseOptions, "/test9", 0)
        for (ch in "\"abc\":0,\"abc\":9")
            assembler.accept(ch)
        assertFailsWith<JSONException> { assembler.accept('}') }.let {
            expect("Duplicate key - abc, at /test9") { it.message }
        }
    }

    @Test fun `should allow duplicate key and take first instance`() {
        val parseOptions = ParseOptions(objectKeyDuplicate = JSONObject.DuplicateKeyOption.TAKE_FIRST)
        val assembler = ObjectAssembler(parseOptions, "/test9", 0)
        for (ch in """"abc":0,"abc":1}""")
            assembler.accept(ch)
        assertTrue(assembler.valid)
        assertTrue(assembler.complete)
        with(assembler.value) {
            expect(1) { size }
            expect(JSONInt(0)) { this["abc"] }
        }
    }

    @Test fun `should allow duplicate key and take last instance`() {
        val parseOptions = ParseOptions(objectKeyDuplicate = JSONObject.DuplicateKeyOption.TAKE_LAST)
        val assembler = ObjectAssembler(parseOptions, "/test9", 0)
        for (ch in """"abc":0,"abc":1}""")
            assembler.accept(ch)
        assertTrue(assembler.valid)
        assertTrue(assembler.complete)
        with(assembler.value) {
            expect(1) { size }
            expect(JSONInt(1)) { this["abc"] }
        }
    }

    @Test fun `should allow duplicate key if identical`() {
        val parseOptions = ParseOptions(objectKeyDuplicate = JSONObject.DuplicateKeyOption.CHECK_IDENTICAL)
        val assembler = ObjectAssembler(parseOptions, "/test9", 0)
        for (ch in """"abc":1,"abc":1}""")
            assembler.accept(ch)
        assertTrue(assembler.valid)
        assertTrue(assembler.complete)
        with(assembler.value) {
            expect(1) { size }
            expect(JSONInt(1)) { this["abc"] }
        }
    }

}
