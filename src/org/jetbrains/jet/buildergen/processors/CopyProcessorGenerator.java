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
import org.jetbrains.jet.buildergen.EntityRepresentationContext;
import org.jetbrains.jet.buildergen.TypeTransformer;
import org.jetbrains.jet.buildergen.entities.Entity;
import org.jetbrains.jet.buildergen.entities.Relation;
import org.jetbrains.jet.buildergen.java.code.CodeFactory;
import org.jetbrains.jet.buildergen.java.code.CodeUtil;
import org.jetbrains.jet.buildergen.java.declarations.ClassModel;
import org.jetbrains.jet.buildergen.java.types.TypeData;
import org.jetbrains.jet.buildergen.java.types.TypeUtil;

import java.util.List;

import static org.jetbrains.jet.buildergen.java.code.CodeUtil.methodCall;
import static org.jetbrains.jet.buildergen.processors.BeanProcessorGenerator.*;

public class CopyProcessorGenerator {

    public static ClassModel generate(
            @NotNull String packageName,
            @NotNull String className,
            @NotNull final EntityRepresentationContext<? extends ClassModel> interfaces,
            @NotNull final EntityRepresentationContext<? extends ClassModel> implementations) {
        final TypeTransformer interfaceTypes = new TypeTransformer(interfaces);
        final TypeTransformer implTypes = new TypeTransformer(interfaces);
        return BeanProcessorGenerator.generate(packageName, className, interfaces, new BeanProcessorGenerationStrategy() {
            @NotNull
            @Override
            public TypeData getInType(@NotNull Entity entity) {
                return TypeUtil.simpleType(interfaces.getRepresentation(entity));
            }

            @NotNull
            @Override
            public TypeData getOutType(@NotNull Entity entity) {
                return TypeUtil.simpleType(interfaces.getRepresentation(entity));
            }

            @Override
            public void defineContextFields(@NotNull ClassModel generatorClass) {
            }

            @Override
            public void defineAdditionalMethods(@NotNull ClassModel generatorClass) {
            }

            @Override
            public <E> E expressionToAssignToOut(@NotNull CodeFactory<E> f, @NotNull Entity entity) {
                return CodeUtil.constructorCall(f, implementations.getRepresentation(entity));
            }

            @Override
            public <E> void traceMethodBody(@NotNull CodeFactory<E> f, @NotNull Entity entity) {
            }

            @Override
            public <E> void assignRelation(
                    @NotNull CodeFactory<E> f,
                    @NotNull Entity out,
                    @NotNull Relation<?> relation,
                    E outExpression,
                    E inExpression,
                    @NotNull ExpressionConverter converter,
                    @NotNull List<E> statements
            ) {
                BeanProcessorGeneratorUtil.assignRelation(f, out, relation, outExpression, inExpression, converter, interfaceTypes, statements);
            }

            @NotNull
            @Override
            public RelationVisitor<TypeData> getConversionInType() {
                return BeanProcessorGeneratorUtil.getTypes(interfaceTypes);
            }

            @NotNull
            @Override
            public RelationVisitor<TypeData> getConversionOutType() {
                return getConversionInType();
            }

            @Override
            public <E> RelationVisitorVoid convertRelationMethodBody(final CodeFactory<E> f, final E inExpression, @NotNull final List<E> statements) {
                return new RelationVisitorVoid() {
                    @Override
                    public void reference(@NotNull Relation<?> relation, @NotNull Entity target) {
                        statements.add(
                                f._return(inExpression)
                        );
                    }

                    @Override
                    public void entity(@NotNull Relation<?> relation, @NotNull Entity target) {
                        statements.add(
                                f._return(
                                        methodCall(f,
                                                   null, BeanProcessorGenerator.processEntityMethodName(target), inExpression)
                                )
                        );
                    }

                    @Override
                    public void data(@NotNull Relation<?> relation) {
                        statements.add(
                                f._return(inExpression)
                        );
                    }
                };
            }
        });
    }
}
