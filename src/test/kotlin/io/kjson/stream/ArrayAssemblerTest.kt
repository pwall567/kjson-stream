/*
 * @(#) ArrayAssemblerTest.kt
 *
 * kjson-stream  JSON Kotlin streaming library
 * Copyright (c) 2023 Peter Wall
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.expect

import io.kjson.JSONArray
import io.kjson.JSONBoolean
import io.kjson.JSONDecimal
import io.kjson.JSONInt
import io.kjson.JSONString
import io.kjson.parser.ParseException
import io.kjson.parser.ParseOptions
import io.kjson.parser.ParserConstants.rootPointer

class ArrayAssemblerTest {

    @Test fun `should create empty array`() {
        val assembler = ArrayAssembler(ParseOptions.DEFAULT, rootPointer)
        assertFalse(assembler.complete)
        assertFalse(assembler.valid)
        assembler.accept(']')
        assertTrue(assembler.complete)
        assertTrue(assembler.valid)
        with(assembler.value) {
            assertTrue(isEmpty())
        }
    }

    @Test fun `should create array with a single entry`() {
        val assembler = ArrayAssembler(ParseOptions.DEFAULT, rootPointer)
        for (ch in "0]") {
            assertFalse(assembler.complete)
            assertFalse(assembler.valid)
            assembler.accept(ch)
        }
        assertTrue(assembler.complete)
        assertTrue(assembler.valid)
        with(assembler.value) {
            expect(1) { size }
            expect(JSONInt(0)) { this[0] }
        }
    }

    @Test fun `should create array with two entries`() {
        val assembler = ArrayAssembler(ParseOptions.DEFAULT, rootPointer)
        for (ch in "0,1]") {
            assertFalse(assembler.complete)
            assertFalse(assembler.valid)
            assembler.accept(ch)
        }
        assertTrue(assembler.complete)
        assertTrue(assembler.valid)
        with(assembler.value) {
            expect(2) { size }
            expect(JSONInt(0)) { this[0] }
            expect(JSONInt(1)) { this[1] }
        }
    }

    @Test fun `should create array with complex mix of entries`() {
        val assembler = ArrayAssembler(ParseOptions.DEFAULT, rootPointer)
        for (ch in "\"zebra\",true,1,[],null,2.5]") {
            assertFalse(assembler.complete)
            assertFalse(assembler.valid)
            assembler.accept(ch)
        }
        assertTrue(assembler.complete)
        assertTrue(assembler.valid)
        with(assembler.value) {
            expect(6) { size }
            expect(JSONString("zebra")) { this[0] }
            expect(JSONBoolean.TRUE) { this[1] }
            expect(JSONInt(1)) { this[2] }
            with(this[3]) {
                assertNotNull(this)
                assertTrue(this is JSONArray)
                assertTrue(isEmpty())
            }
            assertNull(this[4])
            expect(JSONDecimal("2.5")) { this[5] }
        }
    }

    @Test fun `should accept whitespace between entries`() {
        val assembler = ArrayAssembler(ParseOptions.DEFAULT, rootPointer)
        for (ch in " \"zebra\"   , true  , 1 , [ ]    , null ,  2.5] ")
            assembler.accept(ch)
        assertTrue(assembler.complete)
        assertTrue(assembler.valid)
        with(assembler.value) {
            expect(6) { size }
            expect(JSONString("zebra")) { this[0] }
            expect(JSONBoolean.TRUE) { this[1] }
            expect(JSONInt(1)) { this[2] }
            with(this[3]) {
                assertNotNull(this)
                assertTrue(this is JSONArray)
                assertTrue(isEmpty())
            }
            assertNull(this[4])
            expect(JSONDecimal("2.5")) { this[5] }
        }
    }

    @Test fun `should fail on invalid array content`() {
        val assembler = ArrayAssembler(ParseOptions.DEFAULT, rootPointer)
        assertFailsWith<ParseException> { assembler.accept('a') }.let {
            expect("Illegal JSON syntax at /0") { it.message }
        }
    }

    @Test fun `should fail on invalid array content later in array`() {
        val assembler = ArrayAssembler(ParseOptions.DEFAULT, rootPointer)
        assembler.accept('0')
        assembler.accept(',')
        assertFailsWith<ParseException> { assembler.accept('a') }.let {
            expect("Illegal JSON syntax at /1") { it.message }
        }
    }

    @Test fun `should fail on invalid content in nested array`() {
        val assembler = ArrayAssembler(ParseOptions.DEFAULT, rootPointer)
        assembler.accept('[')
        assertFailsWith<ParseException> { assembler.accept('a') }.let {
            expect("Illegal JSON syntax at /0/0") { it.message }
        }
    }

    @Test fun `should fail on missing closing bracket`() {
        val assembler = ArrayAssembler(ParseOptions.DEFAULT, rootPointer)
        assembler.accept('0')
        assertFailsWith<ParseException> { assembler.value }.let {
            expect("JSON array is incomplete") { it.message }
        }
    }

    @Test fun `should fail on trailing comma when option not set`() {
        val assembler = ArrayAssembler(ParseOptions.DEFAULT, rootPointer)
        assembler.accept('0')
        assembler.accept(',')
        assertFailsWith<ParseException> { assembler.accept(']') }.let {
            expect("Trailing comma in JSON array") { it.message }
        }
    }

    @Test fun `should accept trailing comma when option set`() {
        val parseOptions = ParseOptions(arrayTrailingComma = true)
        val assembler = ArrayAssembler(parseOptions, rootPointer)
        assembler.accept('0')
        assembler.accept(',')
        assembler.accept(']')
        assertTrue(assembler.valid)
        assertTrue(assembler.complete)
        with(assembler.value) {
            expect(1) { size }
            expect(JSONInt(0)) { this[0] }
        }
    }

    @Test fun `should fail on missing comma`() {
        val assembler = ArrayAssembler(ParseOptions.DEFAULT, "/test9")
        assembler.accept('0')
        assembler.accept(' ')
        assertFailsWith<ParseException> { assembler.accept('0') }.let {
            expect("Missing comma in JSON array at /test9") { it.message }
        }
    }

}
