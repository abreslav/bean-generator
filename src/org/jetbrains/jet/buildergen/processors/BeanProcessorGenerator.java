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
import org.jetbrains.jet.buildergen.EntityRepresentationGenerator;
import org.jetbrains.jet.buildergen.GeneratorUtil;
import org.jetbrains.jet.buildergen.entities.Entity;
import org.jetbrains.jet.buildergen.entities.EntityUtil;
import org.jetbrains.jet.buildergen.entities.Relation;
import org.jetbrains.jet.buildergen.java.code.CodeFactory;
import org.jetbrains.jet.buildergen.java.declarations.ClassKind;
import org.jetbrains.jet.buildergen.java.declarations.ClassModel;
import org.jetbrains.jet.buildergen.java.declarations.MethodModel;
import org.jetbrains.jet.buildergen.java.declarations.Visibility;
import org.jetbrains.jet.buildergen.java.declarations.beans.ClassBean;
import org.jetbrains.jet.buildergen.java.declarations.beans.MethodBean;
import org.jetbrains.jet.buildergen.java.declarations.beans.ParameterBean;
import org.jetbrains.jet.buildergen.java.types.TypeData;
import org.jetbrains.jet.buildergen.java.types.TypeUtil;

import java.util.List;

import static org.jetbrains.jet.buildergen.EntityRepresentationGenerator.NOT_NULL;
import static org.jetbrains.jet.buildergen.java.code.CodeUtil.methodCall;
import static org.jetbrains.jet.buildergen.java.code.CodeUtil.methodCallStatement;

public class BeanProcessorGenerator {

    public static ClassModel generate(
            @NotNull String packageName,
            @NotNull String className,
            @NotNull EntityRepresentationContext<? extends ClassModel> context,
            @NotNull BeanProcessorGenerationStrategy strategy
    ) {
        return new Generator(strategy).generate(packageName, className, context);
    }

    interface BeanProcessorGenerationStrategy {
        @NotNull
        TypeData getInType(@NotNull Entity entity);

        @NotNull
        TypeData getOutType(@NotNull Entity entity);

        void defineContextFields(@NotNull ClassModel generatorClass);
        void defineAdditionalMethods(@NotNull ClassModel generatorClass);

        <E> E expressionToAssignToOut(@NotNull CodeFactory<E> f, @NotNull Entity entity);

        <E> void traceMethodBody(
                @NotNull CodeFactory<E> f,
                @NotNull Entity entity,
                E inExpression,
                E outExpression,
                @NotNull List<E> statements
        );

        <E> void assignRelation(
                @NotNull CodeFactory<E> f,
                @NotNull Entity out,
                @NotNull Relation<?> relation,
                E outExpression,
                E inExpression,
                @NotNull ExpressionConverter converter,
                @NotNull List<E> statements
        );


        @NotNull
        RelationVisitor<TypeData> getConversionInType();
        @NotNull
        RelationVisitor<TypeData> getConversionOutType();

        <E> RelationVisitorVoid convertRelationMethodBody(CodeFactory<E> f, E inExpression, @NotNull List<E> statements);
    }

    public interface RelationVisitor<R> {
        R reference(@NotNull Relation<?> relation, @NotNull Entity target);

        R entity(@NotNull Relation<?> relation, @NotNull Entity target);

        R data(@NotNull Relation<?> relation);
    }

    public interface RelationVisitorVoid {
        void reference(@NotNull Relation<?> relation, @NotNull Entity target);

        void entity(@NotNull Relation<?> relation, @NotNull Entity target);

        void data(@NotNull Relation<?> relation);
    }

    public interface ExpressionConverter {
        <E> E convertedExpression(@NotNull CodeFactory<E> f, E subject);
    }

    private static void executeForRelation(@NotNull Relation<?> relation, @NotNull final RelationVisitorVoid visitor) {
        getFromRelation(relation, new RelationVisitor<Void>() {
            @Override
            public Void reference(@NotNull Relation<?> relation, @NotNull Entity target) {
                visitor.reference(relation, target);
                return null;
            }

            @Override
            public Void entity(@NotNull Relation<?> relation, @NotNull Entity target) {
                visitor.entity(relation, target);
                return null;
            }

            @Override
            public Void data(@NotNull Relation<?> relation) {
                visitor.data(relation);
                return null;
            }
        });
    }

    private static <R, D> R getFromRelation(@NotNull Relation<?> relation, @NotNull RelationVisitor<R> visitor) {
        Object target = relation.getTarget();
        if (target instanceof Entity) {
            Entity targetEntity = (Entity) target;
            if (EntityUtil.isReference(relation)) {
                return visitor.reference(relation, targetEntity);
            }
            else {
                return visitor.entity(relation, targetEntity);
            }
        }
        else {
            return visitor.data(relation);
        }
    }

    private static class Generator {

        private static final String PROCESS_METHOD_PARAMETER_NAME = "in";
        private static final String PROCESS_METHOD_RESULT_NAME = "out";

        private final BeanProcessorGenerationStrategy m;

        private Generator(@NotNull BeanProcessorGenerationStrategy m) {
            this.m = m;
        }

        @NotNull
        public ClassModel generate(
                @NotNull String packageName,
                @NotNull String className,
                @NotNull EntityRepresentationContext<? extends ClassModel> context
        ) {
            ClassModel result = new ClassBean()
                    .setVisibility(Visibility.PUBLIC)
                    .setAbstract(false)
                    .setKind(ClassKind.CLASS)
                    .setPackageFqName(packageName)
                    .setName(className);

            m.defineContextFields(result);

            for (Entity entity : context.getEntities()) {
                result.getMethods().add(generateProcessEntityMethod(entity));
                for (Relation<?> relation : EntityUtil.getAllRelations(entity)) {
                    result.getMethods().add(generateProcessRelationMethod(entity, relation));
                    result.getMethods().add(generateConvertRelationMethod(entity, relation));
                }
            }

            for (Entity entity : context.getEntities()) {
                result.getMethods().add(generateTraceMethod(entity));
            }

            m.defineAdditionalMethods(result);

            return result;
        }

        @NotNull
        private MethodModel generateProcessEntityMethod(@NotNull final Entity entity) {
            MethodBean method = new MethodBean()
                    .setVisibility(Visibility.PUBLIC)
                    .setReturnType(m.getOutType(entity))
                    .setName(processEntityMethodName(entity))
                    .addParameter(
                            new ParameterBean()
                                .setType(m.getInType(entity))
                                .setName(PROCESS_METHOD_PARAMETER_NAME)
                    );

            GeneratorUtil.createMethodBody(method, new GeneratorUtil.MethodBody() {
                @Override
                public <E> void body(@NotNull CodeFactory<E> f, @NotNull List<E> statements) {
                    processEntityMethodBody(f, entity, statements);
                }
            });

            return method;
        }


        /*

            Out processIn(In in) {
                if (in == null) return null;

                Out out = new OutBean();
                traceIn(in, out);

                processIn_R1(in, out);
                processIn_R2(in, out);
                processIn_R3(in, out);

                return out;
            }

            void processIn_R1(In in, Out, out) {
                out.setR1(convertIn_R1(in.getR1()));
            }

            void processIn_R1(In in, Out, out) {
                for (R1 r1 : in.getR1s()) {
                  out.addR1(convertIn_R1(in.getR1()));
                }
            }
         */

        private <E> void processEntityMethodBody(@NotNull CodeFactory<E> f, @NotNull Entity entity, @NotNull List<E> statements) {
            statements.add(
                    GeneratorUtil.ifVariableIsNullReturnNullStatement(f, PROCESS_METHOD_PARAMETER_NAME)
            );

            // create out
            statements.add(
                    f.statement(
                        f.variableDeclaration(
                                m.getOutType(entity),
                                PROCESS_METHOD_RESULT_NAME,
                                m.expressionToAssignToOut(f, entity)
                        )
                    )
            );

            // trace (+ assert)
            statements.add(
                    methodCallStatement(f, null,
                                        traceMethodName(entity),
                                        f.variableReference(PROCESS_METHOD_PARAMETER_NAME),
                                        f.variableReference(PROCESS_METHOD_RESULT_NAME))
            );

            for (Relation<?> relation : EntityUtil.getAllRelations(entity)) {
                statements.add(
                        methodCallStatement(f, null,
                                            processRelationMethodName(entity, relation),
                                            f.variableReference(PROCESS_METHOD_PARAMETER_NAME),
                                            f.variableReference(PROCESS_METHOD_RESULT_NAME))
                );
            }

            statements.add(
                    f._return(f.variableReference(PROCESS_METHOD_RESULT_NAME))
            );
        }

        @NotNull
        private MethodModel generateProcessRelationMethod(@NotNull final Entity entity, @NotNull final Relation<?> relation) {
            MethodBean method = new MethodBean()
                    .setVisibility(Visibility.PUBLIC)
                    .setReturnType(TypeUtil._void())
                    .setName(processRelationMethodName(entity, relation))
                    .addParameter(
                            new ParameterBean()
                                .addAnnotation(NOT_NULL)
                                .setType(m.getInType(entity))
                                .setName(PROCESS_METHOD_PARAMETER_NAME)
                    )
                    .addParameter(
                            new ParameterBean()
                                .setType(m.getOutType(entity))
                                .setName(PROCESS_METHOD_RESULT_NAME)
                    );

            GeneratorUtil.createMethodBody(method,
                                           new GeneratorUtil.MethodBody() {
                                               @Override
                                               public <E> void body(@NotNull CodeFactory<E> f, @NotNull List<E> statements) {
                                                   m.assignRelation(
                                                           f, entity, relation,
                                                           f.variableReference(PROCESS_METHOD_RESULT_NAME),
                                                           getRelationExpression(f, relation, f.variableReference(PROCESS_METHOD_PARAMETER_NAME)),
                                                           new ExpressionConverter() {

                                                               @Override
                                                               public <E> E convertedExpression(@NotNull CodeFactory<E> f, E subject) {
                                                                   return methodCall(f, null,
                                                                                     convertRelationMethodName(entity, relation),
                                                                                     subject);
                                                               }
                                                           },
                                                           statements
                                                   );
                                               }
                                           });

            return method;
        }

        @NotNull
        private MethodModel generateConvertRelationMethod(@NotNull final Entity entity, @NotNull final Relation<?> relation) {
            MethodBean method = new MethodBean()
                    .setVisibility(Visibility.PUBLIC)
                    .setReturnType(getConversionOutType(relation))
                    .setName(convertRelationMethodName(entity, relation))
                    .addParameter(
                            new ParameterBean()
                                    .setType(getConversionInType(relation))
                                    .setName(PROCESS_METHOD_PARAMETER_NAME)
                    );

            GeneratorUtil.createMethodBody(method,
                                           new GeneratorUtil.MethodBody() {
                                               @Override
                                               public <E> void body(@NotNull CodeFactory<E> f, @NotNull List<E> statements) {
                                                   executeForRelation(relation,
                                                                      m.convertRelationMethodBody(f,
                                                                                                  f.variableReference(PROCESS_METHOD_PARAMETER_NAME),
                                                                                                  statements));
                                               }
                                           });

            return method;
        }

        private TypeData getConversionInType(Relation<?> relation) {
            return getFromRelation(relation, m.getConversionInType());
        }

        private TypeData getConversionOutType(Relation<?> relation) {
            return getFromRelation(relation, m.getConversionOutType());
        }

        @NotNull
        private MethodModel generateTraceMethod(@NotNull final Entity entity) {
            MethodBean method = new MethodBean()
                    .setVisibility(Visibility.PUBLIC)
                    .setReturnType(TypeUtil._void())
                    .setName(traceMethodName(entity))
                    .addParameter(
                            new ParameterBean()
                                    .setType(TypeUtil.simpleType(Object.class))
                                    .setName(PROCESS_METHOD_PARAMETER_NAME)
                    )
                    .addParameter(
                            new ParameterBean()
                                    .setType(TypeUtil.simpleType(Object.class))
                                    .setName(PROCESS_METHOD_RESULT_NAME)
                    );

            GeneratorUtil.createMethodBody(method, new GeneratorUtil.MethodBody() {
                @Override
                public <E> void body(@NotNull CodeFactory<E> f, @NotNull List<E> statements) {
                    m.traceMethodBody(f,
                                      entity,
                                      f.variableReference(PROCESS_METHOD_PARAMETER_NAME),
                                      f.variableReference(PROCESS_METHOD_RESULT_NAME),
                                      statements);
                }
            });

            return method;
        }

        private <E> E getRelationExpression(@NotNull CodeFactory<E> f, @NotNull Relation<?> relation, E receiver) {
            String getterName = EntityRepresentationGenerator.getGetterName(relation);
            return methodCall(f, receiver, getterName);
        }

    }

    @NotNull
    public static String processEntityMethodName(@NotNull Entity entity) {
        return "process" + entity.getName();
    }

    @NotNull
    public static String traceMethodName(@NotNull Entity entity) {
        return "trace" + entity.getName();
    }

    @NotNull
    public static String processRelationMethodName(@NotNull Entity entity, @NotNull Relation<?> relation) {
        return "process" + entity.getName() + "_" + relation.getName();
    }

    @NotNull
    public static String convertRelationMethodName(@NotNull Entity entity, @NotNull Relation<?> relation) {
        return "convert" + entity.getName() + "_" + relation.getName();
    }
}
