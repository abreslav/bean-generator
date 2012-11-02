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
import org.jetbrains.jet.utils.Printer;

import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.buildergen.java.ClassPrinter.FIELD_INITIALIZER;
import static org.jetbrains.jet.buildergen.java.ClassPrinter.METHOD_BODY;
import static org.jetbrains.jet.buildergen.java.code.CodeUtil.*;
import static org.jetbrains.jet.buildergen.processors.BeanProcessorGenerator.*;
import static org.jetbrains.jet.buildergen.processors.BeanProcessorGeneratorUtil.LOOP_ITEM;

@SuppressWarnings("unchecked")
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
                return TypeUtil.type(interfaces.getRepresentation(entity));
            }

            @NotNull
            @Override
            public TypeData getOutType(@NotNull Entity entity) {
                return TypeUtil.type(Void.class);
            }

            @Override
            public void defineContextFields(@NotNull ClassModel generatorClass) {
                generatorClass.getFields().add(
                        new FieldBean()
                                .setVisibility(Visibility.PRIVATE)
                                .setFinal(true)
                                .setType(TypeUtil.type(StringBuilder.class))
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
                                .setType(TypeUtil.type(Printer.class))
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
                            .setReturnType(TypeUtil.type(String.class))
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
                Collections.addAll(statements,
                                   _printer.println(f,
                                                    f.string(entity.getName()),
                                                    f.string("@"),
                                                    methodCall(f, f.classReference(CodeUtil.getClassBean(Integer.class)), "toHexString",
                                                               methodCall(f, f.classReference(CodeUtil.getClassBean(System.class)),
                                                                          "identityHashCode", inExpression))
                                   ),

                                   _printer.pushIndent(f)
                );
            }

            @Override
            public <E> void afterEntityMethodBody(
                    @NotNull CodeFactory<E> f, @NotNull Entity entity, E inExpression, E outExpression, @NotNull List<E> statements
            ) {
                statements.add(
                        _printer.popIndent(f)
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
                        return TypeUtil.type(Void.class);
                    }

                    @Override
                    public TypeData entity(@NotNull Relation<?> relation, @NotNull Entity target) {
                        return TypeUtil.type(Void.class);
                    }

                    @Override
                    public TypeData data(@NotNull Relation<?> relation) {
                        return TypeUtil.type(Void.class);
                    }
                };
            }

            @Override
            public <E> RelationVisitorVoid convertRelationMethodBody(final CodeFactory<E> f, final E inExpression, @NotNull final List<E> statements) {
                return new RelationVisitorVoid() {
                    @Override
                    public void reference(@NotNull Relation<?> relation, @NotNull Entity target) {
                        Collections.addAll(statements,
                                           _printer.print(f, f.string(relation.getName() + " = ")),

                                           _printer.printlnWithNoIndent(f,
                                                        methodCall(f, inExpression, "resolve")
                                           ),

                                           f._return(f._null())
                        );
                    }

                    @Override
                    public void entity(@NotNull Relation<?> relation, @NotNull Entity target) {
                        Collections.addAll(statements,
                                           GeneratorUtil.ifExpressionIsNullReturnNullStatement(f, inExpression),

                                           _printer.println(f, f.string(relation.getName() + " = ")),

                                           _printer.pushIndent(f),

                                           methodCallStatement(f, null,
                                                               BeanProcessorGenerator.processEntityMethodName(target), inExpression),

                                           _printer.popIndent(f),

                                           f._return(f._null())
                        );
                    }

                    @Override
                    public void data(@NotNull Relation<?> relation) {
                        Collections.addAll(statements,
                                _printer.println(f, f.string(relation.getName() + " = "), inExpression),

                                f._return(f._null())
                        );
                    }
                };
            }
        });
    }

    private static class _printer {
        private static <E> E printerMethod(CodeFactory<E> f, String methodName, E... args) {
            return methodCallStatement(f, f.variableReference(PRINTER), methodName, args);
        }

        private static <E> E println(CodeFactory<E> f, E... args) {
            return printerMethod(f, "println", args);
        }

        private static <E> E print(CodeFactory<E> f, E... args) {
            return printerMethod(f, "print", args);
        }

        private static <E> E printlnWithNoIndent(CodeFactory<E> f, E... args) {
            return printerMethod(f, "printlnWithNoIndent", args);
        }

        private static <E> E printWithNoIndent(CodeFactory<E> f, E... args) {
            return printerMethod(f, "printWithNoIndent", args);
        }

        private static <E> E pushIndent(CodeFactory<E> f) {
            return printerMethod(f, "pushIndent");
        }

        private static <E> E popIndent(CodeFactory<E> f) {
            return printerMethod(f, "popIndent");
        }
    }
}
