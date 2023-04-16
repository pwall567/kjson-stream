/*
 * @(#) StringAssemblerTest.kt
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
import kotlin.test.assertTrue
import kotlin.test.expect

import io.kjson.JSONString
import io.kjson.parser.ParseException
import io.kjson.parser.ParserConstants.rootPointer

class StringAssemblerTest {

    @Test fun `should build simple string`() {
        val assembler = StringAssembler(rootPointer)
        assertFalse(assembler.complete)
        assertFalse(assembler.valid)
        for (ch in "simple") {
            assembler.accept(ch)
            assertFalse(assembler.complete)
            assertFalse(assembler.valid)
        }
        assembler.accept('"')
        assertTrue(assembler.complete)
        assertTrue(assembler.valid)
        expect(JSONString("simple")) { assembler.value }
    }

    @Test fun `should accept backslash sequences`() {
        val assembler = StringAssembler(rootPointer)
        for (ch in "a\\nbc \\t \\\\ \\\" \\r x") {
            assembler.accept(ch)
            assertFalse(assembler.complete)
            assertFalse(assembler.valid)
        }
        assembler.accept('"')
        assertTrue(assembler.complete)
        assertTrue(assembler.valid)
        expect(JSONString("a\nbc \t \\ \" \r x")) { assembler.value }
    }

    @Test fun `should accept unicode sequences`() {
        val assembler = StringAssembler(rootPointer)
        for (ch in "a\\u2014bc") {
            assembler.accept(ch)
            assertFalse(assembler.complete)
            assertFalse(assembler.valid)
        }
        assembler.accept('"')
        assertTrue(assembler.complete)
        assertTrue(assembler.valid)
        expect(JSONString("a\u2014bc")) { assembler.value }
    }

    @Test fun `should fail on incorrect backslash sequence`() {
        val assembler = StringAssembler(rootPointer)
        assembler.accept('a')
        assembler.accept('\\')
        assertFailsWith<ParseException> { assembler.accept('x') }.let {
            expect("Illegal escape sequence in JSON string") { it.message }
        }
    }

    @Test fun `should fail on incorrect unicode sequence`() {
        val assembler = StringAssembler(rootPointer)
        assembler.accept('a')
        assembler.accept('\\')
        assembler.accept('u')
        assertFailsWith<ParseException> { assembler.accept('x') }.let {
            expect("Illegal Unicode sequence in JSON string") { it.message }
        }
    }

}
