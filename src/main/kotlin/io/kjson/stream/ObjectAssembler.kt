/*
 * @(#) ObjectAssembler.kt
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

import io.kjson.JSON.asString
import io.kjson.JSONObject
import io.kjson.JSONValue
import io.kjson.JSONStreamer.Companion.getAssembler
import io.kjson.parser.ParseException
import io.kjson.parser.ParseOptions
import io.kjson.parser.ParserConstants.identifierContinuationSet
import io.kjson.parser.ParserConstants.identifierStartSet
import io.kjson.parser.ParserErrors.DUPLICATE_KEY
import io.kjson.parser.ParserErrors.ILLEGAL_KEY
import io.kjson.parser.ParserErrors.ILLEGAL_SYNTAX
import io.kjson.parser.ParserErrors.MAX_DEPTH_EXCEEDED
import io.kjson.parser.ParserErrors.MISSING_COLON
import io.kjson.parser.ParserErrors.MISSING_COMMA_OBJECT
import io.kjson.parser.ParserErrors.OBJECT_INCOMPLETE
import io.kjson.parser.ParserErrors.TRAILING_COMMA_OBJECT
import net.pwall.json.JSONFunctions.isSpaceCharacter

/**
 * Streaming [Assembler] implementation to process objects.
 *
 * @author  Peter Wall
 */
class ObjectAssembler(
    private val parseOptions: ParseOptions,
    private val pointer: String,
    private val depth: Int,
) : Assembler {

    init {
        if (depth >= parseOptions.maximumNestingDepth)
            throw ParseException(MAX_DEPTH_EXCEEDED)
    }

    enum class State { INITIAL, CONTINUATION, UNQUOTED_ID, KEYWORD, COLON_EXPECTED, ENTRY, CHILD, COMMA_EXPECTED,
            COMPLETE }

    private var state: State = State.INITIAL
    private var child: Assembler = Assembler.NullAssembler
    private val entries = mutableListOf<Pair<String, JSONValue?>>()
    private var name = ""
    private var ignore = false
    private var checkPrevious = -1
    private val unquotedId = StringBuilder()

    override val complete: Boolean
        get() = state == State.COMPLETE

    override val valid: Boolean
        get() = complete

    override val value: JSONObject
        get() = if (complete) JSONObject.from(entries) else throw ParseException(OBJECT_INCOMPLETE, pointer)

    override fun accept(ch: Char): Boolean {
        var consumed: Boolean
        while (true) {
            consumed = true
            when (state) {
                State.INITIAL -> {
                    if (!isSpaceCharacter(ch)) {
                        state =  when (ch) {
                            '}' -> State.COMPLETE
                            '"' -> State.KEYWORD.also { child = StringAssembler(pointer) }
                            else -> throw ParseException(ILLEGAL_KEY, pointer)
                        }
                    }
                }
                State.CONTINUATION -> {
                    if (!isSpaceCharacter(ch)) {
                        state = when (ch) {
                            '}' -> {
                                if (parseOptions.objectTrailingComma)
                                    State.COMPLETE
                                else
                                    throw ParseException(TRAILING_COMMA_OBJECT)
                            }
                            '"' -> {
                                child = StringAssembler(pointer)
                                State.KEYWORD
                            }
                            in identifierStartSet -> {
                                if (parseOptions.objectKeyUnquoted) {
                                    unquotedId.append(ch)
                                    State.UNQUOTED_ID
                                }
                                else
                                    throw ParseException(ILLEGAL_KEY, pointer)
                            }
                            else -> throw ParseException(ILLEGAL_KEY, pointer)
                        }
                    }
                }
                State.UNQUOTED_ID -> {
                    if (ch in identifierContinuationSet)
                        unquotedId.append(ch)
                    else {
                        consumed = false
                        name = unquotedId.toString()
                        unquotedId.setLength(0)
                        duplicateKeyCheck()
                        state = State.COLON_EXPECTED
                    }
                }
                State.KEYWORD -> {
                    ignore = false
                    checkPrevious = -1
                    child.accept(ch)
                    if (child.complete) {
                        name = child.value.asString
                        duplicateKeyCheck()
                        state = State.COLON_EXPECTED
                    }
                }
                State.COLON_EXPECTED -> {
                    if (!isSpaceCharacter(ch)) {
                        state = when (ch) {
                            ':' -> State.ENTRY
                            else -> throw ParseException(MISSING_COLON, pointer)
                        }
                    }
                }
                State.ENTRY -> {
                    if (!isSpaceCharacter(ch)) {
                        child = getAssembler(ch, parseOptions, "$pointer/${entries.size}", depth)
                        state = State.CHILD
                    }
                }
                State.CHILD -> {
                    consumed = child.accept(ch)
                    if (child.complete) {
                        val value = child.value
                        if (!ignore) {
                            if (checkPrevious >= 0) {
                                if (entries[checkPrevious].second != value)
                                    throw ParseException("$DUPLICATE_KEY \"$name\"", pointer)
                            }
                            else
                                entries.add(name to value)
                        }
                        state = State.COMMA_EXPECTED
                    }
                }
                State.COMMA_EXPECTED -> {
                    if (!isSpaceCharacter(ch)) {
                        state = when (ch) {
                            ',' -> State.CONTINUATION
                            '}' -> State.COMPLETE
                            else -> throw ParseException(MISSING_COMMA_OBJECT, pointer)
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
        return true
    }

    private fun duplicateKeyCheck() {
        checkPrevious = entries.indexOfFirst { it.first == name }
        if (checkPrevious >= 0) {
            when (parseOptions.objectKeyDuplicate) {
                ParseOptions.DuplicateKeyOption.ERROR -> throw ParseException("$DUPLICATE_KEY \"$name\"", pointer)
                ParseOptions.DuplicateKeyOption.TAKE_FIRST -> ignore = true
                ParseOptions.DuplicateKeyOption.TAKE_LAST -> {
                    entries.removeAt(checkPrevious)
                    checkPrevious = -1
                }
                ParseOptions.DuplicateKeyOption.CHECK_IDENTICAL -> {}
            }
        }
    }

}
