/*
 * @(#) StringAssemblerTest.kt
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

import io.kjson.JSONString
import io.kjson.parser.ParseException
import io.kjson.parser.ParserConstants.rootPointer

class StringAssemblerTest {

    @Test fun `should build simple string`() {
        val assembler = StringAssembler(rootPointer)
        assembler.complete shouldBe false
        assembler.valid shouldBe false
        for (ch in "simple") {
            assembler.accept(ch)
            assembler.complete shouldBe false
            assembler.valid shouldBe false
        }
        assembler.accept('"')
        assembler.complete shouldBe true
        assembler.valid shouldBe true
        assembler.value shouldBe JSONString("simple")
    }

    @Test fun `should accept backslash sequences`() {
        val assembler = StringAssembler(rootPointer)
        for (ch in "a\\nbc \\t \\\\ \\\" \\r x") {
            assembler.accept(ch)
            assembler.complete shouldBe false
            assembler.valid shouldBe false
        }
        assembler.accept('"')
        assembler.complete shouldBe true
        assembler.valid shouldBe true
        assembler.value shouldBe JSONString("a\nbc \t \\ \" \r x")
    }

    @Test fun `should accept unicode sequences`() {
        val assembler = StringAssembler(rootPointer)
        for (ch in "a\\u2014bc") {
            assembler.accept(ch)
            assembler.complete shouldBe false
            assembler.valid shouldBe false
        }
        assembler.accept('"')
        assembler.complete shouldBe true
        assembler.valid shouldBe true
        assembler.value shouldBe JSONString("a\u2014bc")
    }

    @Test fun `should fail on incorrect backslash sequence`() {
        val assembler = StringAssembler(rootPointer)
        assembler.accept('a')
        assembler.accept('\\')
        shouldThrow<ParseException>("Illegal escape sequence in JSON string") {
            assembler.accept('x')
        }
    }

    @Test fun `should fail on incorrect unicode sequence`() {
        val assembler = StringAssembler(rootPointer)
        assembler.accept('a')
        assembler.accept('\\')
        assembler.accept('u')
        shouldThrow<ParseException>("Illegal Unicode sequence in JSON string") {
            assembler.accept('x')
        }
    }

}
