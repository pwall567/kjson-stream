/*
 * @(#) JSONCoPipelineTest.kt
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

package io.kjson

import kotlin.test.Test
import kotlinx.coroutines.runBlocking

import io.kstuff.test.shouldBe

import io.kstuff.pipeline.ListCoAcceptor
import io.kstuff.pipeline.accept

class JSONCoPipelineTest {

    @Test fun `should create and use array pipeline`() = runBlocking {
        val result = mutableListOf<JSONValue?>()
        val pipeline = JSONCoPipeline.pipeTo { result.add(it) }
        pipeline.accept("[0,null,2]")
        val expected: List<JSONValue?> = listOf(JSONInt(0), null, JSONInt(2))
        result shouldBe expected
    }

    @Test fun `should use secondary constructor`() = runBlocking {
        val pipeline = JSONCoPipeline(ListCoAcceptor())
        pipeline.accept("[0,999,2]")
        val expected: List<JSONValue?> = listOf(JSONInt(0), JSONInt(999), JSONInt(2))
        pipeline.result shouldBe expected
    }

    @Test fun `should use accept leading and training spaces in pipeline`() = runBlocking {
        val result = mutableListOf<JSONValue?>()
        val pipeline = JSONCoPipeline.pipeTo { result.add(it) }
        pipeline.accept("   [0,\"hello\",2]   ")
        val expected: List<JSONValue?> = listOf(JSONInt(0), JSONString("hello"), JSONInt(2))
        result shouldBe expected
    }

    @Test fun `should allow BOM at start of array pipeline`() = runBlocking {
        val result = mutableListOf<JSONValue?>()
        val pipeline = JSONCoPipeline.pipeTo { result.add(it) }
        pipeline.accept("\uFEFF   [false,[]]   ")
        val expected: List<JSONValue?> = listOf(JSONBoolean.FALSE, JSONArray.EMPTY)
        result shouldBe expected
    }

}
