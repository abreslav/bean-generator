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

package org.jetbrains.jet.buildergen;

import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.buildergen.entities.Entity;
import org.jetbrains.jet.buildergen.entities.Relation;
import org.jetbrains.jet.buildergen.java.JavaKeyWords;
import org.jetbrains.jet.buildergen.java.code.BinaryOperation;
import org.jetbrains.jet.buildergen.java.code.CodeFactory;
import org.jetbrains.jet.buildergen.java.types.TypeUtil;
import org.jetbrains.jet.buildergen.runtime.LiteralReference;

import static org.jetbrains.jet.buildergen.java.code.CodeUtil.classReference;
import static org.jetbrains.jet.buildergen.java.code.CodeUtil.methodCallWithTypeArgument;

public class GeneratorUtil {

    @NotNull
    public static String variableNameByRelation(@NotNull Relation<?> relation) {
        return JavaKeyWords.escapeJavaKeyWordWithUnderscore(StringUtil.decapitalize(relation.getName()));
    }

    static <E> E createLiteralReference(CodeFactory<E> f, Entity target, E referee) {
        Class<LiteralReference> literalReferenceClass = LiteralReference.class;
        return methodCallWithTypeArgument(f, classReference(f, literalReferenceClass.getPackage().getName(),
                                                                literalReferenceClass.getSimpleName()),
                                              "create",
                                              TypeUtil.getDataType(target),
                                              referee);
    }

    public static <E> E ifVariableIsNullReturnNullStatement(CodeFactory<E> f, String variableName) {
        return f._if(
                f.binary(
                        f.variableReference(variableName),
                        BinaryOperation.EQ,
                        f._null()
                ),
                f._return(f._null()));
    }
}