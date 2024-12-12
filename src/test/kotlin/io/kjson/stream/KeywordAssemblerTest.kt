/*
 * @(#) KeywordAssemblerTest.kt
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
import io.kstuff.test.shouldThrow

import io.kjson.JSONBoolean
import io.kjson.parser.ParseException
import io.kjson.parser.ParserConstants.rootPointer

class KeywordAssemblerTest {

    @Test fun `should accept keyword`() {
        val assembler = KeywordAssembler("true", JSONBoolean.TRUE, rootPointer)
        for (ch in "rue") {
            assembler.complete shouldBe false
            assembler.valid shouldBe false
            assembler.accept(ch)
        }
        assembler.complete shouldBe true
        assembler.valid shouldBe true
        assembler.value shouldBe JSONBoolean.TRUE
    }

    @Test fun `should return null`() {
        val assembler = KeywordAssembler("null", null, rootPointer)
        for (ch in "ull") {
            assembler.complete shouldBe false
            assembler.valid shouldBe false
            assembler.accept(ch)
        }
        assembler.complete shouldBe true
        assembler.valid shouldBe true
        assembler.value shouldBe null
    }

    @Test fun `should fail on incorrect keyword`() {
        val assembler = KeywordAssembler("true", JSONBoolean.TRUE, rootPointer)
        shouldThrow<ParseException>("Unrecognised JSON keyword") {
            assembler.accept('e')
        }
    }

}
