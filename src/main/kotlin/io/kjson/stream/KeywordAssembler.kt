/*
 * @(#) KeywordAssembler.kt
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

import io.kjson.JSONValue
import io.kjson.parser.ParseException
import io.kjson.parser.ParserErrors.ILLEGAL_SYNTAX
import io.kjson.parser.ParserErrors.INVALID_KEYWORD
import io.kjson.parser.ParserErrors.KEYWORD_INCOMPLETE
import net.pwall.json.JSONFunctions.isSpaceCharacter

/**
 * Streaming [Assembler] implementation to process keywords (`true`, `false` and `null`).
 *
 * @author  Peter Wall
 */
class KeywordAssembler(
    private val keyword: String,
    private val result: JSONValue?,
    private val pointer: String,
) : Assembler {

    private var index = 1

    override val complete: Boolean
        get() = index >= keyword.length

    override val valid: Boolean
        get() = complete

    override val value: JSONValue?
        get() = if (complete) result else throw ParseException(KEYWORD_INCOMPLETE, pointer)

    override fun accept(ch: Char): Boolean {
        if (complete) {
            if (!isSpaceCharacter(ch))
                throw ParseException(ILLEGAL_SYNTAX, pointer)
        }
        else {
            if (ch != keyword[index++])
                throw ParseException(INVALID_KEYWORD, pointer)
        }
        return true
    }

}
