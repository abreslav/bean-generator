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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.buildergen.entities.Entity;
import org.jetbrains.jet.buildergen.entities.EntityUtil;
import org.jetbrains.jet.buildergen.entities.Relation;
import org.jetbrains.jet.buildergen.java.ClassPrinter;
import org.jetbrains.jet.buildergen.java.code.CodeFactory;
import org.jetbrains.jet.buildergen.java.code.PieceOfCode;
import org.jetbrains.jet.buildergen.java.declarations.*;
import org.jetbrains.jet.buildergen.java.declarations.beans.ClassBean;
import org.jetbrains.jet.buildergen.java.declarations.beans.FieldBean;
import org.jetbrains.jet.buildergen.java.declarations.beans.JavaDeclarationUtil;
import org.jetbrains.jet.buildergen.java.declarations.beans.MethodBean;
import org.jetbrains.jet.buildergen.java.types.TypeUtil;

import java.util.Collection;
import java.util.List;

import static org.jetbrains.jet.buildergen.BuilderClassGenerator.DELEGATE;
import static org.jetbrains.jet.buildergen.BuilderClassGenerator.RELATION_FOR_PARAMETER;
import static org.jetbrains.jet.buildergen.CommonAnnotations.NOT_NULL;
import static org.jetbrains.jet.buildergen.java.ClassPrinter.METHOD_BODY;
import static org.jetbrains.jet.buildergen.java.code.CodeUtil.*;

/**
 * @author abreslav
 */
public class BeanBuilderClassGenerator {

    private static final String BEAN = "bean";
    private static final String BUILD_RESULT = "buildResult";

    @NotNull
    public static Collection<ClassModel> generate(
            @NotNull final BeanGenerationContext context,
            @NotNull final EntityRepresentationContext<? extends ClassModel> builders,
            @NotNull final EntityRepresentationContextImpl beanBuilders,
            @NotNull String targetPackageFqName
    ) {
        return EntityRepresentationGeneratorUtil.generateEntityRepresentations(
            context.getBeanInterfaces().getEntities(),
            ClassKind.CLASS,
            beanBuilders,
            targetPackageFqName,
            new EntityBeanGenerationStrategy() {
                @NotNull
                @Override
                public String getEntityRepresentationName(@NotNull Entity entity) {
                    return entity.getName() + "BeanBuilder";
                }

                @Override
                public void generateEntity(@NotNull Entity entity, @NotNull ClassBean classBean) {
                    classBean.setSuperClass(TypeUtil.type(builders.getRepresentation(entity)));

                    generateClassMembers(
                            context,
                            builders,
                            beanBuilders,
                            classBean,
                            entity
                    );
                }
            }
        );
    }

    private static void generateClassMembers(
            BeanGenerationContext context,
            EntityRepresentationContext<? extends ClassModel> builders,
            EntityRepresentationContextImpl beanBuilders,
            ClassBean classBean,
            Entity entity
    ) {
        ClassModel builderClass = builders.getRepresentation(entity);
        ClassModel beanInterface = context.getBeanInterfaces().getRepresentation(entity);
        ClassModel beanImpl = context.getBeanImplementations().getRepresentation(entity);

        classBean.getFields().add(beanField(beanInterface, beanImpl));
        classBean.getConstructors().add(implementConstructor(builderClass));
        classBean.getConstructors().add(BuilderClassGenerator.defaultConstructor());
        classBean.getMethods().add(beanGetter(beanInterface));

        for (final MethodModel method : builderClass.getMethods()) {
            MethodBean impl = JavaDeclarationUtil.copy(method)
                                    .addAnnotation(CommonAnnotations.OVERRIDE)
                                    .setAbstract(false);
            final Relation<?> relation = method.getData(BuilderClassGenerator.RELATION_FOR_METHOD);

            PieceOfCode body;
            if (relation == null) {
                 // either open() or close()
                if (method.getName().equals("close")) {
                    continue; // no implementation needed for close()
                }
                // open()
                body = openBody(method);
            }
            else {
                // This must be an entity, everything else is taken care of in open()
                final Entity targetEntity = (Entity) relation.getTarget();
                if (EntityUtil.isReference(relation)) {
                    body = referenceSetterBody();
                }
                else {
                    body = builderMethodBody(beanBuilders, relation, targetEntity);
                }
            }

            impl.put(METHOD_BODY, body);
            classBean.getMethods().add(impl);
        }
    }

    private static FieldBean beanField(ClassModel beanInterface, final ClassModel beanImpl) {
        return new FieldBean()
                .setVisibility(Visibility.PRIVATE)
                .setFinal(true)
                .setType(TypeUtil.type(beanInterface))
                .setName(BEAN)
                .put(ClassPrinter.FIELD_INITIALIZER,
                     new PieceOfCode() {
                         @Override
                         public <E> E create(@NotNull CodeFactory<E> f) {
                             return constructorCall(f, beanImpl);
                         }
                     });
    }

    private static MethodBean implementConstructor(ClassModel builderClass) {
        return BuilderClassGenerator.constructorDeclaration(builderClass)
                .put(METHOD_BODY,
                     new PieceOfCode() {
                         @Override
                         public <E> E create(@NotNull CodeFactory<E> f) {
                             return f.statement(methodCall(f, null, "super", f.variableReference(DELEGATE)));
                         }
                     });
    }

    private static PieceOfCode openBody(final MethodModel method) {
        return new PieceOfCode() {
            @Override
            public <E> E create(@NotNull CodeFactory<E> f) {
                List<E> statements = Lists.newArrayList();
                for (ParameterModel parameter : method.getParameters()) {
                    Relation<?> relation = parameter.getData(RELATION_FOR_PARAMETER);
                    String name = relation.getMultiplicity().isCollection()
                                  ? MutableBeanInterfaceGenerator.getAllElementAdderName(relation)
                                  : EntityRepresentationGeneratorUtil.getSetterName(relation);
                    statements.add(
                            methodCallStatement(f, bean(f), name, f.variableReference(parameter.getName()))
                    );
                }
                return f.block(statements);
            }
        };
    }

    private static PieceOfCode referenceSetterBody() {
        return new PieceOfCode() {
             @Override
             public <E> E create(@NotNull CodeFactory<E> f) {
                     // this.bean.setTargetEntity(entity);
                     //return methodCallStatement(f, bean(f), getSetterName(relation), f.variableReference(BuilderClassGenerator.ENTITY));
                     return f.singleLineComment("can't write directly to the bean: types don't match");
             }
        };
    }

    private static PieceOfCode builderMethodBody(
            final EntityRepresentationContext<? extends ClassModel> beanBuilders,
            final Relation<?> relation,
            final Entity targetEntity
    ) {
        return new PieceOfCode() {
            @Override
            public <E> E create(@NotNull CodeFactory<E> f) {
                ClassModel targetBeanBuilder = beanBuilders.getRepresentation(targetEntity);
                String subBuilder = "subBuilder";
                String methodName = relation.getMultiplicity().isCollection()
                              ? MutableBeanInterfaceGenerator.getSingleElementAdderName(relation)
                              : EntityRepresentationGeneratorUtil.getSetterName(relation);
                return block(f,
                    // TargetEntityBeanBuilder subBuilder = new TargetEntityBeanBuilder();
                    f.statement(f.variableDeclaration(TypeUtil.type(targetBeanBuilder), subBuilder,
                                          constructorCall(f, targetBeanBuilder))),
                    // this.bean.addTargetEntity(subBuilder.getBean());
                    methodCallStatement(f, bean(f), methodName,
                                        methodCall(f, f.variableReference(subBuilder), BUILD_RESULT)),
                    // return subBuilder
                    f._return(f.variableReference(subBuilder))
                );
            }
        };
    }

    private static <E> E bean(CodeFactory<E> f) {
        return f.fieldReference(f._this(), BEAN);
    }

    private static MethodBean beanGetter(ClassModel beanInterface) {
        return new MethodBean()
            .addAnnotation(NOT_NULL)
            .setVisibility(Visibility.PUBLIC)
            .setReturnType(TypeUtil.type(beanInterface))
            .setName(BUILD_RESULT)
            .put(METHOD_BODY,
                 new PieceOfCode() {
                     @Override
                     public <E> E create(@NotNull CodeFactory<E> f) {
                         return f._return(bean(f));
                     }
                 });
    }

    private BeanBuilderClassGenerator() {}
}
