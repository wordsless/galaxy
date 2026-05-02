/*
 * MIT License
 *
 * Copyright (c) ${YEAR} Qiang Li (李强)
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

package com.github.wordsless.galaxy.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Interface for constructing AI prompts.
 * Defines the standard rule to build complete prompts based on query, context,
 * conversation history and errors.
 */
public abstract class PromptProvider {

    /**
     * Reads a file from the classpath and returns its content as a string.
     * @param fileName the file name (e.g., "config/test.properties")
     * @return file content as a string
     * @throws IllegalArgumentException if the file is not found in classpath
     * @throws IOException if an I/O error occurs
     */
    public String readFromClasspath(String fileName) throws IOException {
        // Obtain the context class loader for the current thread (works in most environments)
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        // Alternative: ClassLoader.getSystemClassLoader()
        // If using Class.getResourceAsStream("/" + fileName), note that a leading "/" means absolute path

        try (InputStream is = classLoader.getResourceAsStream(fileName)) {
            if (is == null) {
                throw new IllegalArgumentException("File not found in classpath: " + fileName);
            }
            // Read all lines using UTF-8 encoding
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }
            return content.toString();
        }
    }

}