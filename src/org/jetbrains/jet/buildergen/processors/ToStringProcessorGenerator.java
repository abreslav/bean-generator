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
import org.jetbrains.jet.buildergen.GeneratorUtil;
import org.jetbrains.jet.buildergen.TypeTransformer;
import org.jetbrains.jet.buildergen.entities.Entity;
import org.jetbrains.jet.buildergen.entities.Relation;
import org.jetbrains.jet.buildergen.java.code.CodeFactory;
import org.jetbrains.jet.buildergen.java.code.CodeUtil;
import org.jetbrains.jet.buildergen.java.code.PieceOfCode;
import org.jetbrains.jet.buildergen.java.declarations.ClassModel;
import org.jetbrains.jet.buildergen.java.declarations.Visibility;
import org.jetbrains.jet.buildergen.java.declarations.beans.FieldBean;
import org.jetbrains.jet.buildergen.java.declarations.beans.MethodBean;
import org.jetbrains.jet.buildergen.java.types.TypeData;
import org.jetbrains.jet.buildergen.java.types.TypeUtil;
import org.jetbrains.jet.buildergen.runtime.LiteralReferenceResolver;
import org.jetbrains.jet.utils.Printer;

import java.util.List;

import static org.jetbrains.jet.buildergen.java.ClassPrinter.FIELD_INITIALIZER;
import static org.jetbrains.jet.buildergen.java.ClassPrinter.METHOD_BODY;
import static org.jetbrains.jet.buildergen.java.code.CodeUtil.*;
import static org.jetbrains.jet.buildergen.processors.BeanProcessorGenerator.*;
import static org.jetbrains.jet.buildergen.processors.BeanProcessorGeneratorUtil.LOOP_ITEM;

public class ToStringProcessorGenerator {

    private static final String BUILDER = "builder";
    private static final String PRINTER = "printer";

    public static ClassModel generate(
            @NotNull String packageName,
            @NotNull String className,
            @NotNull final EntityRepresentationContext<? extends ClassModel> interfaces) {
        final TypeTransformer interfaceTypes = new TypeTransformer(interfaces);
        return BeanProcessorGenerator.generate(packageName, className, interfaces, new BeanProcessorGenerationStrategy() {
            @NotNull
            @Override
            public TypeData getInType(@NotNull Entity entity) {
                return TypeUtil.simpleType(interfaces.getRepresentation(entity));
            }

            @NotNull
            @Override
            public TypeData getOutType(@NotNull Entity entity) {
                return TypeUtil.simpleType(Void.class);
            }

            @Override
            public void defineContextFields(@NotNull ClassModel generatorClass) {
                generatorClass.getFields().add(
                        new FieldBean()
                                .setVisibility(Visibility.PRIVATE)
                                .setFinal(true)
                                .setType(TypeUtil.simpleType(StringBuilder.class))
                                .setName(BUILDER)
                                .put(FIELD_INITIALIZER,
                                     new PieceOfCode() {
                                         @Override
                                         public <E> E create(@NotNull CodeFactory<E> f) {
                                             return constructorCall(f, StringBuilder.class);
                                         }
                                     }
                                )
                );
                generatorClass.getFields().add(
                        new FieldBean()
                                .setVisibility(Visibility.PRIVATE)
                                .setFinal(true)
                                .setType(TypeUtil.simpleType(Printer.class))
                                .setName(PRINTER)
                                .put(FIELD_INITIALIZER,
                                     new PieceOfCode() {
                                         @Override
                                         public <E> E create(@NotNull CodeFactory<E> f) {
                                             return constructorCall(f, Printer.class, f.variableReference(BUILDER));
                                         }
                                     }
                                )
                );
            }

            @Override
            public void defineAdditionalMethods(@NotNull ClassModel generatorClass) {
                generatorClass.getMethods().add(
                        new MethodBean()
                            .setVisibility(Visibility.PUBLIC)
                            .setReturnType(TypeUtil.simpleType(String.class))
                            .setName("result")
                            .put(
                                    METHOD_BODY,
                                    new PieceOfCode() {
                                        @Override
                                        public <E> E create(@NotNull CodeFactory<E> f) {
                                            return f._return(
                                                    methodCall(f, f.variableReference(BUILDER), "toString")
                                            );
                                        }
                                    }
                            )
                );
            }

            @Override
            public <E> E expressionToAssignToOut(@NotNull CodeFactory<E> f, @NotNull Entity entity) {
                return f._null();
            }

            @Override
            public <E> void beforeEntityMethodBody(
                    @NotNull CodeFactory<E> f,
                    @NotNull Entity entity,
                    E inExpression,
                    E outExpression,
                    @NotNull List<E> statements
            ) {
                statements.add(
                        methodCallStatement(f, f.variableReference(PRINTER), "println",
                                            f.string(entity.getName()),
                                            f.string("@"),
                                            methodCall(f, f.classReference(CodeUtil.getClassBean(Integer.class)), "toHexString",
                                                methodCall(f, f.classReference(CodeUtil.getClassBean(System.class)), "identityHashCode", inExpression))
                                            )
                );
                statements.add(
                        methodCallStatement(f, f.variableReference(PRINTER), "pushIndent")
                );
            }

            @Override
            public <E> void afterEntityMethodBody(
                    @NotNull CodeFactory<E> f, @NotNull Entity entity, E inExpression, E outExpression, @NotNull List<E> statements
            ) {
                statements.add(
                        methodCallStatement(f, f.variableReference(PRINTER), "popIndent")
                );
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
                if (relation.getMultiplicity().isCollection()) {
                    TypeData elementType = BeanProcessorGeneratorUtil.getTargetElementType(interfaceTypes, relation);
                    statements.add(
                            _for(f, elementType, LOOP_ITEM, inExpression,
                                 f.statement(converter.convertedExpression(f, f.variableReference(LOOP_ITEM)))
                            )
                    );
                }
                else {
                    statements.add(
                            f.statement(converter.convertedExpression(f, inExpression))
                    );
                }
            }

            @NotNull
            @Override
            public RelationVisitor<TypeData> getConversionInType() {
                return BeanProcessorGeneratorUtil.getTypes(interfaceTypes);
            }

            @NotNull
            @Override
            public RelationVisitor<TypeData> getConversionOutType() {
                return new RelationVisitor<TypeData>() {
                    @Override
                    public TypeData reference(@NotNull Relation<?> relation, @NotNull Entity target) {
                        return TypeUtil.simpleType(Void.class);
                    }

                    @Override
                    public TypeData entity(@NotNull Relation<?> relation, @NotNull Entity target) {
                        return TypeUtil.simpleType(Void.class);
                    }

                    @Override
                    public TypeData data(@NotNull Relation<?> relation) {
                        return TypeUtil.simpleType(Void.class);
                    }
                };
            }

            @Override
            public <E> RelationVisitorVoid convertRelationMethodBody(final CodeFactory<E> f, final E inExpression, @NotNull final List<E> statements) {
                return new RelationVisitorVoid() {
                    @Override
                    public void reference(@NotNull Relation<?> relation, @NotNull Entity target) {
                        statements.add(
                                methodCallStatement(f,
                                                    f.variableReference(PRINTER), "print", f.string(relation.getName() + " = "))
                        );
                        statements.add(
                                methodCallStatement(f, f.variableReference(PRINTER), "printlnWithNoIndent",
                                                    methodCall(f,
                                                        f.fieldReference(f.classReference(CodeUtil.getClassBean(LiteralReferenceResolver.class)), "INSTANCE"),
                                                        "resolve", inExpression
                                                        )
                                )
                        );
                        statements.add(
                                f._return(f._null())
                        );
                    }

                    @Override
                    public void entity(@NotNull Relation<?> relation, @NotNull Entity target) {
                        statements.add(GeneratorUtil.ifExpressionIsNullReturnNullStatement(f, inExpression));
                        statements.add(
                                methodCallStatement(f,
                                                    f.variableReference(PRINTER), "println", f.string(relation.getName() + " = "))
                        );
                        statements.add(
                                methodCallStatement(f,
                                                    f.variableReference(PRINTER), "pushIndent")
                        );
                        statements.add(
                                methodCallStatement(f,
                                                    null, BeanProcessorGenerator.processEntityMethodName(target), inExpression)
                        );
                        statements.add(
                                methodCallStatement(f,
                                                    f.variableReference(PRINTER), "popIndent")
                        );
                        statements.add(
                                f._return(f._null())
                        );
                    }

                    @Override
                    public void data(@NotNull Relation<?> relation) {
                        statements.add(
                                methodCallStatement(f,
                                                    f.variableReference(PRINTER), "println", f.string(relation.getName() + " = "), inExpression)
                        );
                        statements.add(
                                f._return(f._null())
                        );
                    }
                };
            }
        });
    }
}
