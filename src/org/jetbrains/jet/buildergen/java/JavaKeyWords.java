/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.buildergen.java;

import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.NotNull;

public class JavaKeyWords {
    private static final ImmutableSet<String> JAVA_KEY_WORDS = ImmutableSet.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else", "enum", "extends", "false", "final", "finally",
            "float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long",
            "native", "new", "null", "package", "private", "protected", "public", "return", "short", "static",
            "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "true", "try",
            "void", "volatile", "while"
    );

    public static boolean isJavaKeyWord(@NotNull String id) {
        return JAVA_KEY_WORDS.contains(id);
    }

    @NotNull
    public static String escapeJavaKeyWord(@NotNull String id, @NotNull String prefix) {
        if (isJavaKeyWord(id)) {
            return prefix + id;
        }
        return id;
    }

    @NotNull
    public static String escapeJavaKeyWordWithUnderscore(@NotNull String id) {
        return escapeJavaKeyWord(id, "_");
    }

}
