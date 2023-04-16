/*
 * @(#) KeywordAssemblerTest.kt
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
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.expect

import io.kjson.JSONBoolean
import io.kjson.parser.ParseException
import io.kjson.parser.ParserConstants.rootPointer

class KeywordAssemblerTest {

    @Test fun `should accept keyword`() {
        val assembler = KeywordAssembler("true", JSONBoolean.TRUE, rootPointer)
        for (ch in "rue") {
            assertFalse(assembler.complete)
            assertFalse(assembler.valid)
            assembler.accept(ch)
        }
        assertTrue(assembler.complete)
        assertTrue(assembler.valid)
        expect(JSONBoolean.TRUE) { assembler.value }
    }

    @Test fun `should return null`() {
        val assembler = KeywordAssembler("null", null, rootPointer)
        for (ch in "ull") {
            assertFalse(assembler.complete)
            assertFalse(assembler.valid)
            assembler.accept(ch)
        }
        assertTrue(assembler.complete)
        assertTrue(assembler.valid)
        assertNull(assembler.value)
    }

    @Test fun `should fail on incorrect keyword`() {
        val assembler = KeywordAssembler("true", JSONBoolean.TRUE, rootPointer)
        assertFailsWith<ParseException> { assembler.accept('e') }.let {
            expect("Unrecognised JSON keyword") { it.message }
        }
    }

}
