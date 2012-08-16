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

import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.ClassModel;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.types.TypeData;

import java.util.List;

/**
* @author abreslav
*/
public interface CodeFactory<E> {
    E statement(E expression);

    E block(List<E> block);

    E fieldReference(E receiver, String field);

    E variableDeclaration(TypeData type, String name, @Nullable E initializer);

    E variableReference(String name);

    E methodCall(@Nullable E receiver, String method, List<E> arguments);

    E constructorCall(ClassModel classBeingInstantiated, List<TypeData> typeArguments, List<E> arguments);

    E assignment(E lhs, E rhs);

    E _return(@Nullable E subj);

    E string(String s);

    E integer(int i);

    E binary(E lhs, BinaryOperation op, E rhs);

    E _this();

    E _null();

    E _for(E variableDeclaration, E rangeExpression, E body);

    E _if(E condition, E body);
}
