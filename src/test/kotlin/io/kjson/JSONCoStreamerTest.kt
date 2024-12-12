/*
 * @(#) JSONCoStreamerTest.kt
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

import io.kjson.JSON.asString
import net.pwall.pipeline.accept

class JSONCoStreamerTest {

    @Test fun `should parse JSON as stream`() = runBlocking {
        val json = """{"works":"OK"}"""
        val streamer = JSONCoStreamer()
        for (ch in json)
            streamer.accept(ch.code)
        with(streamer.result) {
            shouldBeType<JSONObject>()
            this["works"].asString shouldBe "OK"
        }
    }

    @Test fun `should allow leading and trailing spaces`() = runBlocking {
        val json = """    {"works":"OK"}    """
        val streamer = JSONCoStreamer()
        streamer.accept(json)
        with(streamer.result) {
            shouldBeType<JSONObject>()
            this["works"].asString shouldBe "OK"
        }
    }

    @Test fun `should allow leading BOM`() = runBlocking {
        val json = "\uFEFF    {\"works\": \"OK\"}    "
        val streamer = JSONCoStreamer()
        streamer.accept(json)
        with(streamer.result) {
            shouldBeType<JSONObject>()
            this["works"].asString shouldBe "OK"
        }
    }

    @Test fun `should parse single value`() = runBlocking {
        val json = "   0   "
        val streamer = JSONCoStreamer()
        streamer.accept(json)
        with(streamer.result) {
            shouldBeType<JSONInt>()
            value shouldBe 0
        }
    }

}
