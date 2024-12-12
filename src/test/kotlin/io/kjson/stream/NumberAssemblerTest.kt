/*
 * @(#) NumberAssemblerTest.kt
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
import io.kstuff.test.shouldBeSameInstance
import io.kstuff.test.shouldBeType
import io.kstuff.test.shouldThrow

import io.kjson.JSONDecimal
import io.kjson.JSONInt
import io.kjson.JSONLong
import io.kjson.parser.ParseException
import io.kjson.parser.ParserConstants.rootPointer
import io.kjson.parser.ParserErrors.ILLEGAL_LEADING_ZERO

class NumberAssemblerTest {

    @Test fun `should parse simple number`() {
        val assembler = NumberAssembler('1', rootPointer)
        assembler.valid shouldBe true
        assembler.complete shouldBe false
        assembler.accept('2')
        assembler.valid shouldBe true
        assembler.complete shouldBe false
        assembler.accept('3')
        assembler.valid shouldBe true
        assembler.complete shouldBe false
        assembler.value.let {
            it.shouldBeType<JSONInt>()
            it shouldBe JSONInt(123)
        }
    }

    @Test fun `should parse zero`() {
        val assembler = NumberAssembler('0', rootPointer)
        assembler.valid shouldBe true
        assembler.complete shouldBe false
        assembler.value shouldBeSameInstance JSONInt.ZERO
    }

    @Test fun `should parse negative number`() {
        val assembler = NumberAssembler('-', rootPointer)
        assembler.valid shouldBe false
        assembler.complete shouldBe false
        for (ch in "123") {
            assembler.accept(ch)
            assembler.valid shouldBe true
            assembler.complete shouldBe false
        }
        assembler.value.let {
            it.shouldBeType<JSONInt>()
            it shouldBe JSONInt(-123)
        }
    }

    @Test fun `should parse long number`() {
        val assembler = NumberAssembler('1', rootPointer)
        assembler.valid shouldBe true
        assembler.complete shouldBe false
        for (ch in "23456789123456789") {
            assembler.accept(ch)
            assembler.valid shouldBe true
            assembler.complete shouldBe false
        }
        assembler.value.let {
            it.shouldBeType<JSONLong>()
            it shouldBe JSONLong(123456789123456789)
        }
    }

    @Test fun `should parse floating point number`() {
        val assembler = NumberAssembler('1', rootPointer)
        assembler.valid shouldBe true
        assembler.complete shouldBe false
        for (ch in "23.50")
            assembler.accept(ch)
        assembler.valid shouldBe true
        assembler.complete shouldBe false
        assembler.value shouldBe JSONDecimal("123.50")
    }

    @Test fun `should parse maximum int value`() {
        val assembler = NumberAssembler('2', rootPointer)
        for (ch in "147483647")
            assembler.accept(ch)
        assembler.valid shouldBe true
        assembler.complete shouldBe false
        assembler.value.let {
            it.shouldBeType<JSONInt>()
            it shouldBe JSONInt(Int.MAX_VALUE)
        }
    }

    @Test fun `should parse number greater than maximum int value`() {
        val assembler = NumberAssembler('2', rootPointer)
        for (ch in "147483648")
            assembler.accept(ch)
        assembler.valid shouldBe true
        assembler.complete shouldBe false
        assembler.value.let {
            it.shouldBeType<JSONLong>()
            it shouldBe JSONLong(2147483648)
        }
    }

    @Test fun `should parse minimum int value`() {
        val assembler = NumberAssembler('-', rootPointer)
        for (ch in "2147483648")
            assembler.accept(ch)
        assembler.valid shouldBe true
        assembler.complete shouldBe false
        assembler.value.let {
            it.shouldBeType<JSONInt>()
            it shouldBe JSONInt(Int.MIN_VALUE)
        }
    }

    @Test fun `should parse number greater than minimum int value`() {
        val assembler = NumberAssembler('-', rootPointer)
        for (ch in "2147483649")
            assembler.accept(ch)
        assembler.valid shouldBe true
        assembler.complete shouldBe false
        assembler.value.let {
            it.shouldBeType<JSONLong>()
            it shouldBe JSONLong(-2147483649)
        }
    }

    @Test fun `should parse maximum long value`() {
        val assembler = NumberAssembler('9', rootPointer)
        for (ch in "223372036854775807")
            assembler.accept(ch)
        assembler.valid shouldBe true
        assembler.complete shouldBe false
        assembler.value.let {
            it.shouldBeType<JSONLong>()
            it shouldBe JSONLong(Long.MAX_VALUE)
        }
    }

    @Test fun `should parse number greater than maximum long value`() {
        val assembler = NumberAssembler('9', rootPointer)
        for (ch in "223372036854775808")
            assembler.accept(ch)
        assembler.valid shouldBe true
        assembler.complete shouldBe false
        assembler.value.let {
            it.shouldBeType<JSONDecimal>()
            assembler.value shouldBe JSONDecimal("9223372036854775808")
        }
    }

    @Test fun `should parse minimum long value`() {
        val assembler = NumberAssembler('-', rootPointer)
        for (ch in "9223372036854775808")
            assembler.accept(ch)
        assembler.valid shouldBe true
        assembler.complete shouldBe false
        assembler.value.let {
            it.shouldBeType<JSONLong>()
            it shouldBe JSONLong(Long.MIN_VALUE)
        }
    }

    @Test fun `should parse number greater than minimum long value`() {
        val assembler = NumberAssembler('-', rootPointer)
        for (ch in "9223372036854775809")
            assembler.accept(ch)
        assembler.valid shouldBe true
        assembler.complete shouldBe false
        assembler.value.let {
            it.shouldBeType<JSONDecimal>()
            assembler.value shouldBe JSONDecimal("-9223372036854775809")
        }
    }

    @Test fun `should fail on leading zeros`() {
        val assembler = NumberAssembler('0', "/test1")
        shouldThrow<ParseException>("$ILLEGAL_LEADING_ZERO, at /test1") {
            assembler.accept('1')
        }
    }

}
