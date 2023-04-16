/*
 * @(#) StringAssembler.kt
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

import io.kjson.JSONString
import io.kjson.parser.ParseException
import io.kjson.parser.ParserErrors.ILLEGAL_SYNTAX
import net.pwall.json.JSONFunctions.ILLEGAL_CHAR
import net.pwall.json.JSONFunctions.ILLEGAL_ESCAPE_SEQUENCE
import net.pwall.json.JSONFunctions.ILLEGAL_UNICODE_SEQUENCE
import net.pwall.json.JSONFunctions.UNTERMINATED_STRING
import net.pwall.json.JSONFunctions.isSpaceCharacter

/**
 * Streaming [Assembler] implementation to process strings.
 *
 * @author  Peter Wall
 */
class StringAssembler(private val pointer: String) : Assembler {

    enum class State { NORMAL, BACKSLASH, UNICODE1, UNICODE2, UNICODE3, UNICODE4, COMPLETE }

    private var state = State.NORMAL
    private val sb = StringBuilder()
    private var unicode = 0

    override val complete: Boolean
        get() = state == State.COMPLETE

    override val valid: Boolean
        get() = complete

    override val value: JSONString
        get() = if (complete) JSONString(sb.toString()) else throw ParseException(UNTERMINATED_STRING, pointer)

    override fun accept(ch: Char): Boolean {
        when (state) {
            State.NORMAL -> {
                when (ch) {
                    '"' -> state = State.COMPLETE
                    '\\' -> state = State.BACKSLASH
                    in ' '..Char.MAX_VALUE -> sb.append(ch)
                    else -> throw ParseException("$ILLEGAL_CHAR $ch", pointer)
                }
            }
            State.BACKSLASH -> {
                when (ch) {
                    '"' -> escapeChar('"')
                    '\\' -> escapeChar('\\')
                    '/' -> escapeChar('/')
                    'b' -> escapeChar('\b')
                    'f' -> escapeChar('\u000C')
                    'n' -> escapeChar('\n')
                    'r' -> escapeChar('\r')
                    't' -> escapeChar('\t')
                    'u' -> state = State.UNICODE1
                    else -> throw ParseException(ILLEGAL_ESCAPE_SEQUENCE, pointer)
                }
            }
            State.UNICODE1 -> {
                unicode = checkHex(ch)
                state = State.UNICODE2
            }
            State.UNICODE2 -> {
                unicode = (unicode shl 4) or checkHex(ch)
                state = State.UNICODE3
            }
            State.UNICODE3 -> {
                unicode = (unicode shl 4) or checkHex(ch)
                state = State.UNICODE4
            }
            State.UNICODE4 -> {
                sb.append(((unicode shl 4) or checkHex(ch)).toChar())
                state = State.NORMAL
            }
            State.COMPLETE -> {
                if (!isSpaceCharacter(ch))
                    throw ParseException(ILLEGAL_SYNTAX, pointer)
            }
        }
        return true
    }

    private fun escapeChar(ch: Char) {
        sb.append(ch)
        state = State.NORMAL
    }

    private fun checkHex(ch: Char): Int = when (ch) {
        in '0'..'9' -> ch.code - '0'.code
        in 'A'..'F' -> ch.code - 'A'.code + 10
        in 'a'..'f' -> ch.code - 'a'.code + 10
        else -> throw ParseException(ILLEGAL_UNICODE_SEQUENCE, pointer)
    }

}
