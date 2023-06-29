/*
 * @(#) JSONLinesPipeline.kt
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

import io.kjson.JSONStreamer.Companion.getAssembler
import io.kjson.parser.ParseException
import io.kjson.parser.ParseOptions
import io.kjson.parser.ParserConstants.BOM
import io.kjson.parser.ParserErrors.ILLEGAL_SYNTAX
import io.kjson.stream.Assembler
import io.kjson.util.AcceptorAdapter
import net.pwall.json.JSONFunctions.isSpaceCharacter
import net.pwall.pipeline.AbstractIntObjectPipeline
import net.pwall.pipeline.Acceptor

/**
 * JSON Lines pipeline class.  The class accepts a stream of characters in JSON Lines form, and emits JSON values as
 * they are completed.
 *
 * @author  Peter Wall
 * @param   <R>     the pipeline result type
 */
class JSONLinesPipeline<R>(
    downstream: Acceptor<JSONValue?, R>,
    private val parseOptions: ParseOptions = ParseOptions.DEFAULT,
) : AbstractIntObjectPipeline<JSONValue?, R>(downstream), IntConsumer {

    enum class State { BOM_POSSIBLE, INITIAL, CHILD }

    private var state: State = State.BOM_POSSIBLE
    private var child: Assembler = Assembler.NullAssembler
    private var count = 0

    override fun isComplete(): Boolean = state != State.CHILD || child.valid

    override fun acceptInt(value: Int) {
        val ch = value.toChar()
        when (state) {
            State.BOM_POSSIBLE -> {
                state = State.INITIAL
                if (ch != BOM && !isSpaceCharacter(ch)) {
                    child = getAssembler(ch, parseOptions, "/$count")
                    state = State.CHILD
                }
            }
            State.INITIAL -> {
                if (!isSpaceCharacter(ch)) {
                    child = getAssembler(ch, parseOptions, "/$count")
                    state = State.CHILD
                }
            }
            State.CHILD -> {
                if (!child.accept(ch) && !isSpaceCharacter(ch))
                    throw ParseException(ILLEGAL_SYNTAX)
                if (child.complete) {
                    emit(child.value)
                    count++
                    state = State.INITIAL
                }
            }
        }
    }

    companion object {

        fun pipeTo(
            parseOptions: ParseOptions = ParseOptions.DEFAULT,
            block: (JSONValue?) -> Unit,
        ): JSONLinesPipeline<Unit> = JSONLinesPipeline(AcceptorAdapter(block), parseOptions)

    }

}
