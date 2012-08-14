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

package org.jetbrains.jet.lang.descriptors.builders.generator.java.code;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.ClassModel;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.types.TypeData;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author abreslav
 */
public class CodeUtil {

    public static <E> E block(@NotNull CodeFactory<E> factory, E... statements) {
        return factory.block(Arrays.asList(statements));
    }

    public static <E> E methodCall(@NotNull CodeFactory<E> factory, @Nullable E receiver, String methodName, E... arguments) {
        return factory.methodCall(receiver, methodName, Arrays.asList(arguments));
    }

    public static <E> E methodCallStatement(@NotNull CodeFactory<E> factory, @Nullable E receiver, String methodName, E... arguments) {
        return factory.statement(methodCall(factory, receiver, methodName, arguments));
    }

    public static <E> E constructorCall(@NotNull CodeFactory<E> factory, @NotNull ClassModel classModel, E... arguments) {
        return factory.constructorCall(classModel, Collections.<TypeData>emptyList(), Arrays.asList(arguments));
    }
}
