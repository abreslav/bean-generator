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

import com.google.common.collect.Lists;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.buildergen.entities.Entity;
import org.jetbrains.jet.buildergen.entities.Relation;
import org.jetbrains.jet.buildergen.java.ClassPrinter;
import org.jetbrains.jet.buildergen.java.JavaKeyWords;
import org.jetbrains.jet.buildergen.java.code.BinaryOperation;
import org.jetbrains.jet.buildergen.java.code.CodeFactory;
import org.jetbrains.jet.buildergen.java.code.PieceOfCode;
import org.jetbrains.jet.buildergen.java.declarations.beans.MethodBean;
import org.jetbrains.jet.buildergen.runtime.LiteralReference;

import java.util.List;

import static org.jetbrains.jet.buildergen.java.code.CodeUtil.constructorCall;

public class GeneratorUtil {

    @NotNull
    public static String variableNameByRelation(@NotNull Relation<?> relation) {
        return JavaKeyWords.escapeJavaKeyWordWithUnderscore(StringUtil.decapitalize(relation.getName()));
    }

    public static <E> E ifVariableIsNullReturnNullStatement(CodeFactory<E> f, String variableName) {
        return ifExpressionIsNullReturnNullStatement(f, f.variableReference(variableName));
    }

    public static <E> E ifExpressionIsNullReturnNullStatement(CodeFactory<E> f, E expression) {
        return f._if(
                f.binary(
                        expression,
                        BinaryOperation.EQ,
                        f._null()
                ),
                f._return(f._null()));
    }

    public interface MethodBody {
        <E> void body(@NotNull CodeFactory<E> f, @NotNull List<E> statements);
    }

    public static void createMethodBody(@NotNull MethodBean method, @NotNull final MethodBody body) {
        method.put(
                ClassPrinter.METHOD_BODY,
                new PieceOfCode() {
                    @Override
                    public <E> E create(@NotNull CodeFactory<E> f) {
                        List<E> statements = Lists.newArrayList();

                        body.body(f, statements);

                        return f.block(statements);
                    }
                }
        );
    }
}
