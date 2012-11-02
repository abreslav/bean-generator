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

package org.jetbrains.jet.buildergen.processors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.buildergen.EntityRepresentationGenerator;
import org.jetbrains.jet.buildergen.MutableBeanInterfaceGenerator;
import org.jetbrains.jet.buildergen.TypeTransformer;
import org.jetbrains.jet.buildergen.entities.Entity;
import org.jetbrains.jet.buildergen.entities.Multiplicity;
import org.jetbrains.jet.buildergen.entities.Relation;
import org.jetbrains.jet.buildergen.java.code.CodeFactory;
import org.jetbrains.jet.buildergen.java.types.TypeData;

import java.util.List;

import static org.jetbrains.jet.buildergen.java.code.CodeUtil._for;
import static org.jetbrains.jet.buildergen.java.code.CodeUtil.methodCallStatement;

public class BeanProcessorGeneratorUtil {
    public static final String LOOP_ITEM = "item";

    public static <E> void assignRelation(
            @NotNull CodeFactory<E> f,
            @NotNull Entity out,
            @NotNull Relation<?> relation,
            E outExpression,
            E inExpression,
            @NotNull BeanProcessorGenerator.ExpressionConverter converter,
            @NotNull TypeTransformer interfaceTypes,
            @NotNull List<E> statements
    ) {
        if (relation.getMultiplicity().isCollection()) {
            TypeData elementType = getTargetElementType(interfaceTypes, relation);
            String oneElementAdderName = MutableBeanInterfaceGenerator.getSingleElementAdderName(relation);
            statements.add(
                    _for(f, elementType, LOOP_ITEM, inExpression,
                         methodCallStatement(f, outExpression, oneElementAdderName,
                                             converter.convertedExpression(f, f.variableReference(LOOP_ITEM)))
                    )
            );
        }
        else {
            String setterName = EntityRepresentationGenerator.getSetterName(relation);
            statements.add(
                    methodCallStatement(f, outExpression, setterName,
                                        converter.convertedExpression(f, inExpression))
            );
        }
    }

    @NotNull
    public static BeanProcessorGenerator.RelationVisitor<TypeData> getTypes(@NotNull final TypeTransformer types) {
        return new BeanProcessorGenerator.RelationVisitor<TypeData>() {
            @Override
            public TypeData reference(@NotNull Relation<?> relation, @NotNull Entity target) {
                return getTargetElementType(types, relation);
            }

            @Override
            public TypeData entity(@NotNull Relation<?> relation, @NotNull Entity target) {
                return getTargetElementType(types, relation);
            }

            @Override
            public TypeData data(@NotNull Relation<?> relation) {
                return getTargetElementType(types, relation);
            }
        };
    }

    public static TypeData getTargetElementType(@NotNull TypeTransformer types, @NotNull Relation<?> relation) {
        return types.relationToType(relation, Multiplicity.ONE);
    }
}
