/*
 * @(#) NumberAssembler.kt
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

import io.kjson.JSONDecimal
import io.kjson.JSONInt
import io.kjson.JSONLong
import io.kjson.JSONValue
import io.kjson.parser.ParseException
import io.kjson.parser.ParserConstants.MAX_INTEGER_DIGITS_LENGTH
import io.kjson.parser.ParserConstants.MAX_LONG_DIGITS_LENGTH
import io.kjson.parser.ParserErrors.ILLEGAL_LEADING_ZERO
import io.kjson.parser.ParserErrors.ILLEGAL_NUMBER
import io.kjson.parser.ParserErrors.NUMBER_INCOMPLETE

/**
 * Streaming [Assembler] implementation to process numbers.
 *
 * @author  Peter Wall
 */
class NumberAssembler(firstChar: Char, private val pointer: String) : Assembler {

    enum class State { MINUS_SEEN, ZERO_SEEN, INTEGER, DOT_SEEN, FRACTION, E_SEEN, E_SIGN_SEEN, EXPONENT, COMPLETE,
        COMPLETE_FRACTION }

    private var state: State = when (firstChar) {
        '-' -> State.MINUS_SEEN
        '0' -> State.ZERO_SEEN
        in '1'..'9' -> State.INTEGER
        else -> throw ParseException(ILLEGAL_NUMBER, pointer)
    }
    private val number = StringBuilder().apply { append(firstChar) }

    override val complete: Boolean
        get() = state == State.COMPLETE || state == State.COMPLETE_FRACTION

    override val valid: Boolean
        get() = state == State.COMPLETE || state == State.COMPLETE_FRACTION || state == State.ZERO_SEEN ||
                state == State.INTEGER || state == State.FRACTION || state == State.EXPONENT

    override val value: JSONValue
        get() = when (state) {
            State.ZERO_SEEN -> JSONInt.ZERO
            State.INTEGER, State.COMPLETE -> when {
                number.length < MAX_INTEGER_DIGITS_LENGTH -> JSONInt(number.toString().toInt())
                number.length < MAX_LONG_DIGITS_LENGTH -> {
                    number.toString().toLong().let {
                        if (it in Int.MIN_VALUE..Int.MAX_VALUE) JSONInt(it.toInt()) else JSONLong(it)
                    }
                }
                else -> {
                    try {
                        JSONLong(number.toString().toLong())
                    }
                    catch (_: NumberFormatException) { // integer too big for Long becomes JSONDecimal
                        JSONDecimal(number.toString())
                    }
                }
            }
            State.FRACTION, State.EXPONENT, State.COMPLETE_FRACTION -> JSONDecimal(number.toString())
            else -> throw ParseException(NUMBER_INCOMPLETE, pointer)
        }

    override fun accept(ch: Char): Boolean {
        when (state) {
            State.MINUS_SEEN -> {
                state = when (ch) {
                    '0' -> State.ZERO_SEEN
                    in '1'..'9' -> State.INTEGER
                    else -> throw ParseException(ILLEGAL_NUMBER, pointer)
                }
            }
            State.ZERO_SEEN -> {
                state = when (ch) {
                    in '0'..'9' -> throw ParseException(ILLEGAL_LEADING_ZERO, pointer)
                    '.' -> State.DOT_SEEN
                    'e', 'E' -> State.E_SEEN
                    else -> State.COMPLETE
                }
            }
            State.INTEGER -> {
                if (ch !in '0'..'9') {
                    state = when (ch) {
                        '.' -> State.DOT_SEEN
                        'e', 'E' -> State.E_SEEN
                        else -> State.COMPLETE
                    }
                }
            }
            State.DOT_SEEN -> {
                state = when (ch) {
                    in '0'..'9' -> State.FRACTION
                    else -> throw ParseException(ILLEGAL_NUMBER, pointer)
                }
            }
            State.FRACTION -> {
                if (ch !in '0'..'9') {
                    state = when (ch) {
                        'e', 'E' -> State.E_SEEN
                        else -> State.COMPLETE_FRACTION
                    }
                }
            }
            State.E_SEEN -> {
                state = when (ch) {
                    '+', '-' -> State.E_SIGN_SEEN
                    in '0'..'9' -> State.EXPONENT
                    else -> throw ParseException(ILLEGAL_NUMBER, pointer)
                }
            }
            State.E_SIGN_SEEN -> {
                state = when (ch) {
                    in '0'..'9' -> State.EXPONENT
                    else -> throw ParseException(ILLEGAL_NUMBER, pointer)
                }
            }
            State.EXPONENT -> {
                if (ch !in '0'..'9')
                    state = State.COMPLETE_FRACTION
            }
            else -> throw ParseException(ILLEGAL_NUMBER, pointer)
        }
        if (state == State.COMPLETE || state == State.COMPLETE_FRACTION)
            return false
        number.append(ch)
        return true
    }

}
