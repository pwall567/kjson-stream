/*
 * @(#) JSONPipeline.kt
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

import java.util.function.IntConsumer

import io.kjson.parser.ParseException
import io.kjson.parser.ParseOptions
import io.kjson.parser.ParserConstants.BOM
import io.kjson.parser.ParserConstants.rootPointer
import io.kjson.parser.ParserErrors.ILLEGAL_ARRAY
import io.kjson.parser.ParserErrors.ILLEGAL_SYNTAX
import io.kjson.parser.ParserErrors.MISSING_COMMA_ARRAY
import io.kjson.parser.ParserErrors.TRAILING_COMMA_ARRAY
import io.kjson.stream.Assembler
import io.kjson.util.AcceptorAdapter
import net.pwall.json.JSONFunctions.isSpaceCharacter
import net.pwall.pipeline.AbstractIntObjectPipeline
import net.pwall.pipeline.Acceptor

/**
 * JSON array pipeline class.  The class accepts a stream of characters in the form of a JSON array, and emits JSON
 * values as they are completed.
 *
 * @author  Peter Wall
 */
class JSONPipeline<R> internal constructor(
    downstream: Acceptor<JSONValue?, R>,
    private val parseOptions: ParseOptions,
    private val pointer: String,
    private var state: State = State.FIRST, // the internal constructor assumes open bracket already seen
) : AbstractIntObjectPipeline<JSONValue?, R>(downstream), IntConsumer {

    constructor(
        downstream: Acceptor<JSONValue?, R>,
        parseOptions: ParseOptions = ParseOptions.DEFAULT,
    ) : this(downstream, parseOptions, rootPointer, State.BOM_POSSIBLE)

    enum class State { BOM_POSSIBLE, INITIAL, FIRST, CONTINUATION, CHILD, COMMA_EXPECTED, COMPLETE }

    private var child: Assembler = Assembler.NullAssembler
    private var count = 0

    override fun isComplete(): Boolean = state == State.COMPLETE

    override fun acceptInt(value: Int) {
        val ch = value.toChar()
        var consumed: Boolean
        while (true) {
            consumed = true
            when (state) {
                State.BOM_POSSIBLE -> {
                    state = State.INITIAL
                    if (ch != BOM && !isSpaceCharacter(ch)) {
                        if (ch == '[')
                            state = State.FIRST
                        else
                            throw ParseException(ILLEGAL_ARRAY)
                    }
                }
                State.INITIAL -> {
                    if (!isSpaceCharacter(ch)) {
                        if (ch == '[')
                            state = State.FIRST
                        else
                            throw ParseException(ILLEGAL_ARRAY)
                    }
                }
                State.FIRST -> {
                    if (!isSpaceCharacter(ch)) {
                        if (ch == ']')
                            state = State.COMPLETE
                        else {
                            child = JSONStreamer.getAssembler(ch, parseOptions, "$pointer/$count")
                            state = State.CHILD
                        }
                    }
                }
                State.CONTINUATION -> {
                    if (!isSpaceCharacter(ch)) {
                        state = if (ch == ']') {
                            if (parseOptions.arrayTrailingComma)
                                State.COMPLETE
                            else
                                throw ParseException(TRAILING_COMMA_ARRAY)
                        }
                        else {
                            child = JSONStreamer.getAssembler(ch, parseOptions, "$pointer/$count")
                            State.CHILD
                        }
                    }
                }
                State.CHILD -> {
                    consumed = child.accept(ch)
                    if (child.complete) {
                        emit(child.value)
                        count++
                        state = State.COMMA_EXPECTED
                    }
                }
                State.COMMA_EXPECTED -> {
                    if (!isSpaceCharacter(ch)) {
                        state = when (ch) {
                            ',' -> State.CONTINUATION
                            ']' -> State.COMPLETE
                            else -> throw ParseException(MISSING_COMMA_ARRAY, pointer)
                        }
                    }
                }
                State.COMPLETE -> {
                    if (!isSpaceCharacter(ch))
                        throw ParseException(ILLEGAL_SYNTAX, pointer)
                }
            }
            if (consumed)
                break
        }
        if (state == State.COMPLETE)
            downstream.close()
    }

    companion object {

        fun pipeTo(
            parseOptions: ParseOptions = ParseOptions.DEFAULT,
            pointer: String = rootPointer,
            block: (JSONValue?) -> Unit,
        ): JSONPipeline<Unit> = JSONPipeline(AcceptorAdapter(block), parseOptions, pointer, State.BOM_POSSIBLE)

    }

}
