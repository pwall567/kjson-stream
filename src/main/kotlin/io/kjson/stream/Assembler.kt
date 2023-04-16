/*
 * @(#) Assembler.kt
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

import io.kjson.JSONException
import io.kjson.JSONValue

/**
 * Interface for JSON value streaming assembler classes.
 *
 * @author  Peter Wall
 */
interface Assembler {

    /** If `true`, value is complete and no more character may be accepted. */
    val complete: Boolean

    /** If `true`, value is valid but more characters may be accepted. */
    val valid: Boolean

    /** The result [JSONValue] (will throw exception if value is not available). */
    val value: JSONValue?

    /**
     * Accept a character from the JSON stream.
     *
     * @param   ch      the character
     * @return  `true` if the character was processed, `false` if not (_e.g._ next character after number)
     */
    fun accept(ch: Char): Boolean

    /**
     * Null implementation of `Assembler` to act as placeholder.
     */
    object NullAssembler : Assembler {
        override val complete: Boolean = false
        override val valid: Boolean = false
        override val value: JSONValue? = null
        override fun accept(ch: Char): Boolean {
            throw JSONException("Assembler not initialised")
        }
    }

}
