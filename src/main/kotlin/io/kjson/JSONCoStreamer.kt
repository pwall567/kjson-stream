/*
 * @(#) JSONCoStreamer.kt
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

import io.jstuff.json.JSONFunctions.isSpaceCharacter
import io.kstuff.pipeline.AbstractIntCoAcceptor

import io.kjson.JSONStreamer.Companion.getAssembler
import io.kjson.JSONStreamer.State
import io.kjson.parser.ParseException
import io.kjson.parser.ParseOptions
import io.kjson.parser.ParserConstants.BOM
import io.kjson.parser.ParserConstants.rootPointer
import io.kjson.parser.ParserErrors.EXCESS_CHARS
import io.kjson.parser.ParserErrors.ILLEGAL_SYNTAX
import io.kjson.parser.ParserErrors.JSON_INCOMPLETE
import io.kjson.stream.Assembler

/**
 * A non-blocking streaming JSON parser.
 *
 * @author  Peter Wall
 */
class JSONCoStreamer(private val parseOptions: ParseOptions = ParseOptions.DEFAULT) :
        AbstractIntCoAcceptor<JSONValue?>() {

    private var state: State = State.BOM_POSSIBLE
    private var child: Assembler = Assembler.NullAssembler

    override val result: JSONValue?
        get() = if (complete) child.value else throw ParseException(JSON_INCOMPLETE)

    override val complete: Boolean
        get() = state == State.COMPLETE || state == State.CHILD && child.valid

    override suspend fun acceptInt(value: Int) {
        val ch = value.toChar()
        when (state) {
            State.BOM_POSSIBLE -> {
                state = State.INITIAL
                if (ch != BOM && !isSpaceCharacter(ch)) {
                    child = getAssembler(ch, parseOptions, rootPointer, 0)
                    state = State.CHILD
                }
            }
            State.INITIAL -> {
                if (!isSpaceCharacter(ch)) {
                    child = getAssembler(ch, parseOptions, rootPointer, 0)
                    state = State.CHILD
                }
            }
            State.CHILD -> {
                if (!child.accept(ch) && !isSpaceCharacter(ch))
                    throw ParseException(ILLEGAL_SYNTAX)
                if (child.complete)
                    state = State.COMPLETE
            }
            State.COMPLETE -> {
                if (!isSpaceCharacter(ch))
                    throw ParseException(EXCESS_CHARS)
            }
        }
    }

}
