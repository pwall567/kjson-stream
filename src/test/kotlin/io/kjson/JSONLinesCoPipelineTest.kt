/*
 * @(#) JSONLinesCoPipelineTest.kt
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
import io.kstuff.test.shouldBeType

import io.kstuff.pipeline.ListCoAcceptor
import io.kstuff.pipeline.accept

import io.kjson.JSON.asInt

class JSONLinesCoPipelineTest {

    @Test fun `should create and use JSON Lines pipeline`() = runBlocking {
        val result = mutableListOf<JSONValue?>()
        val pipeline = JSONLinesCoPipeline.pipeTo { result.add(it) }
        pipeline.accept("{\"a\":111,\"b\":222}\n")
        pipeline.accept("{\"a\":99,\"b\":88}\n")
        pipeline.complete shouldBe true
        with(result) {
            size shouldBe 2
            with(this[0]) {
                shouldBeType<JSONObject>()
                this["a"].asInt shouldBe 111
                this["b"].asInt shouldBe 222
            }
            with(this[1]) {
                shouldBeType<JSONObject>()
                this["a"].asInt shouldBe 99
                this["b"].asInt shouldBe 88
            }
        }
    }

    @Test fun `should create pipeline using constructor`() = runBlocking {
        val pipeline = JSONLinesCoPipeline(ListCoAcceptor())
        pipeline.accept("{\"aa\":1001,\"bb\":2001}\n")
        pipeline.accept("{\"aa\":1002,\"bb\":2002}\n")
        with(pipeline.result) {
            size shouldBe 2
            with(this[0]) {
                shouldBeType<JSONObject>()
                this["aa"].asInt shouldBe 1001
                this["bb"].asInt shouldBe 2001
            }
            with(this[1]) {
                shouldBeType<JSONObject>()
                this["aa"].asInt shouldBe 1002
                this["bb"].asInt shouldBe 2002
            }
        }
    }

    @Test fun `should allow BOM at start of data`() = runBlocking {
        val pipeline = JSONLinesCoPipeline(ListCoAcceptor())
        pipeline.accept(0xFEFF)
        pipeline.accept("{\"aaa\":999,\"bbb\":777}\n")
        with(pipeline.result) {
            size shouldBe 1
            with(this[0]) {
                shouldBeType<JSONObject>()
                this["aaa"].asInt shouldBe 999
                this["bbb"].asInt shouldBe 777
            }
        }
    }

    @Test fun `should accept empty data stream`() = runBlocking {
        val result = mutableListOf<JSONValue?>()
        val pipeline = JSONLinesCoPipeline.pipeTo { result.add(it) }
        pipeline.complete shouldBe true
        with(result) {
            size shouldBe 0
        }
    }

}
