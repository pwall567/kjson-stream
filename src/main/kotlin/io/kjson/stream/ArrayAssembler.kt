/*
 * @(#) ArrayAssembler.kt
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

import io.kjson.JSONArray
import io.kjson.JSONPipeline
import io.kjson.JSONValue
import io.kjson.parser.ParseException
import io.kjson.parser.ParseOptions
import io.kjson.parser.ParserErrors.ARRAY_INCOMPLETE
import io.kjson.util.AcceptorAdapter

/**
 * Streaming [Assembler] implementation to process arrays.
 *
 * @author  Peter Wall
 */
class ArrayAssembler(parseOptions: ParseOptions, pointer: String) : Assembler {

    private val list = mutableListOf<JSONValue?>()
    private val pipeline = JSONPipeline(AcceptorAdapter { list.add(it) }, parseOptions, pointer)

    override val complete: Boolean
        get() = pipeline.isComplete

    override val valid: Boolean
        get() = complete

    override val value: JSONArray
        get() = if (complete) JSONArray.from(list) else throw ParseException(ARRAY_INCOMPLETE)

    override fun accept(ch: Char): Boolean {
        pipeline.accept(ch.code)
        return true
    }

}
