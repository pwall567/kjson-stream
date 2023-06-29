/*
 * @(#) JSONLinesPipelineTest.kt
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

package io.kjson

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.expect

import io.kjson.JSON.asInt
import net.pwall.pipeline.ListAcceptor

class JSONLinesPipelineTest {

    @Test fun `should create and use JSON Lines pipeline`() {
        val result = mutableListOf<JSONValue?>()
        val pipeline = JSONLinesPipeline.pipeTo { result.add(it) }
        pipeline.accept("{\"a\":111,\"b\":222}\n")
        pipeline.accept("{\"a\":99,\"b\":88}\n")
        assertTrue(pipeline.isComplete)
        with(result) {
            expect(2) { size }
            with(this[0]) {
                assertTrue(this is JSONObject)
                expect(111) { this["a"].asInt }
                expect(222) { this["b"].asInt }
            }
            with(this[1]) {
                assertTrue(this is JSONObject)
                expect(99) { this["a"].asInt }
                expect(88) { this["b"].asInt }
            }
        }
    }

    @Test fun `should create pipeline using constructor`() {
        val pipeline = JSONLinesPipeline(ListAcceptor())
        pipeline.accept("{\"aa\":1001,\"bb\":2001}\n")
        pipeline.accept("{\"aa\":1002,\"bb\":2002}\n")
        with(pipeline.result) {
            expect(2) { size }
            with(this[0]) {
                assertTrue(this is JSONObject)
                expect(1001) { this["aa"].asInt }
                expect(2001) { this["bb"].asInt }
            }
            with(this[1]) {
                assertTrue(this is JSONObject)
                expect(1002) { this["aa"].asInt }
                expect(2002) { this["bb"].asInt }
            }
        }
    }

    @Test fun `should allow BOM at start of data`() {
        val pipeline = JSONLinesPipeline(ListAcceptor())
        pipeline.accept(0xFEFF)
        pipeline.accept("{\"aaa\":999,\"bbb\":777}\n")
        with(pipeline.result) {
            expect(1) { size }
            with(this[0]) {
                assertTrue(this is JSONObject)
                expect(999) { this["aaa"].asInt }
                expect(777) { this["bbb"].asInt }
            }
        }
    }

    @Test fun `should accept empty data stream`() {
        val result = mutableListOf<JSONValue?>()
        val pipeline = JSONLinesPipeline.pipeTo { result.add(it) }
        assertTrue(pipeline.isComplete)
        with(result) {
            expect(0) { size }
        }
    }

}
