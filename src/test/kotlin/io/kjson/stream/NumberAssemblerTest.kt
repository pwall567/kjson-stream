/*
 * @(#) NumberAssemblerTest.kt
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
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.expect

import io.kjson.JSONDecimal
import io.kjson.JSONInt
import io.kjson.JSONLong
import io.kjson.parser.ParseException
import io.kjson.parser.ParserConstants.rootPointer
import io.kjson.parser.ParserErrors.ILLEGAL_LEADING_ZERO

class NumberAssemblerTest {

    @Test fun `should parse simple number`() {
        val assembler = NumberAssembler('1', rootPointer)
        assertTrue(assembler.valid)
        assertFalse(assembler.complete)
        assembler.accept('2')
        assertTrue(assembler.valid)
        assertFalse(assembler.complete)
        assembler.accept('3')
        assertTrue(assembler.valid)
        assertFalse(assembler.complete)
        assembler.value.let {
            assertIs<JSONInt>(it)
            expect(JSONInt(123)) { it }
        }
    }

    @Test fun `should parse zero`() {
        val assembler = NumberAssembler('0', rootPointer)
        assertTrue(assembler.valid)
        assertFalse(assembler.complete)
        assertSame(JSONInt.ZERO, assembler.value)
    }

    @Test fun `should parse negative number`() {
        val assembler = NumberAssembler('-', rootPointer)
        assertFalse(assembler.valid)
        assertFalse(assembler.complete)
        for (ch in "123") {
            assembler.accept(ch)
            assertTrue(assembler.valid)
            assertFalse(assembler.complete)
        }
        assembler.value.let {
            assertIs<JSONInt>(it)
            expect(JSONInt(-123)) { it }
        }
    }

    @Test fun `should parse long number`() {
        val assembler = NumberAssembler('1', rootPointer)
        assertTrue(assembler.valid)
        assertFalse(assembler.complete)
        for (ch in "23456789123456789") {
            assembler.accept(ch)
            assertTrue(assembler.valid)
            assertFalse(assembler.complete)
        }
        assembler.value.let {
            assertIs<JSONLong>(it)
            expect(JSONLong(123456789123456789)) { it }
        }
    }

    @Test fun `should parse floating point number`() {
        val assembler = NumberAssembler('1', rootPointer)
        assertTrue(assembler.valid)
        assertFalse(assembler.complete)
        for (ch in "23.50")
            assembler.accept(ch)
        assertTrue(assembler.valid)
        assertFalse(assembler.complete)
        expect(JSONDecimal("123.50")) { assembler.value }
    }

    @Test fun `should parse maximum int value`() {
        val assembler = NumberAssembler('2', rootPointer)
        for (ch in "147483647")
            assembler.accept(ch)
        assertTrue(assembler.valid)
        assertFalse(assembler.complete)
        assembler.value.let {
            assertIs<JSONInt>(it)
            expect(JSONInt(Int.MAX_VALUE)) { it }
        }
    }

    @Test fun `should parse number greater than maximum int value`() {
        val assembler = NumberAssembler('2', rootPointer)
        for (ch in "147483648")
            assembler.accept(ch)
        assertTrue(assembler.valid)
        assertFalse(assembler.complete)
        assembler.value.let {
            assertIs<JSONLong>(it)
            expect(JSONLong(2147483648)) { it }
        }
    }

    @Test fun `should parse minimum int value`() {
        val assembler = NumberAssembler('-', rootPointer)
        for (ch in "2147483648")
            assembler.accept(ch)
        assertTrue(assembler.valid)
        assertFalse(assembler.complete)
        assembler.value.let {
            assertIs<JSONInt>(it)
            expect(JSONInt(Int.MIN_VALUE)) { it }
        }
    }

    @Test fun `should parse number greater than minimum int value`() {
        val assembler = NumberAssembler('-', rootPointer)
        for (ch in "2147483649")
            assembler.accept(ch)
        assertTrue(assembler.valid)
        assertFalse(assembler.complete)
        assembler.value.let {
            assertIs<JSONLong>(it)
            expect(JSONLong(-2147483649)) { it }
        }
    }

    @Test fun `should parse maximum long value`() {
        val assembler = NumberAssembler('9', rootPointer)
        for (ch in "223372036854775807")
            assembler.accept(ch)
        assertTrue(assembler.valid)
        assertFalse(assembler.complete)
        assembler.value.let {
            assertIs<JSONLong>(it)
            expect(JSONLong(Long.MAX_VALUE)) { it }
        }
    }

    @Test fun `should parse number greater than maximum long value`() {
        val assembler = NumberAssembler('9', rootPointer)
        for (ch in "223372036854775808")
            assembler.accept(ch)
        assertTrue(assembler.valid)
        assertFalse(assembler.complete)
        assembler.value.let {
            assertIs<JSONDecimal>(it)
            expect(JSONDecimal("9223372036854775808")) { assembler.value }
        }
    }

    @Test fun `should parse minimum long value`() {
        val assembler = NumberAssembler('-', rootPointer)
        for (ch in "9223372036854775808")
            assembler.accept(ch)
        assertTrue(assembler.valid)
        assertFalse(assembler.complete)
        assembler.value.let {
            assertIs<JSONLong>(it)
            expect(JSONLong(Long.MIN_VALUE)) { it }
        }
    }

    @Test fun `should parse number greater than minimum long value`() {
        val assembler = NumberAssembler('-', rootPointer)
        for (ch in "9223372036854775809")
            assembler.accept(ch)
        assertTrue(assembler.valid)
        assertFalse(assembler.complete)
        assembler.value.let {
            assertIs<JSONDecimal>(it)
            expect(JSONDecimal("-9223372036854775809")) { assembler.value }
        }
    }

    @Test fun `should fail on leading zeros`() {
        val assembler = NumberAssembler('0', "/test1")
        assertFailsWith<ParseException> { assembler.accept('1') }.let {
            expect("$ILLEGAL_LEADING_ZERO at /test1") { it.message }
        }
    }

}
