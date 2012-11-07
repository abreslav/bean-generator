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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.buildergen.entities.Entity;
import org.jetbrains.jet.buildergen.java.ClassPrinter;
import org.jetbrains.jet.buildergen.java.code.CodeFactory;
import org.jetbrains.jet.buildergen.java.code.CodeUtil;
import org.jetbrains.jet.buildergen.java.code.PieceOfCode;
import org.jetbrains.jet.buildergen.java.declarations.ClassKind;
import org.jetbrains.jet.buildergen.java.declarations.ClassModel;
import org.jetbrains.jet.buildergen.java.declarations.beans.ClassBean;
import org.jetbrains.jet.buildergen.java.declarations.beans.JavaDeclarationUtil;
import org.jetbrains.jet.buildergen.java.declarations.beans.MethodBean;
import org.jetbrains.jet.buildergen.java.declarations.beans.ParameterBean;
import org.jetbrains.jet.buildergen.java.types.TypeUtil;
import org.jetbrains.jet.buildergen.runtime.BeanReference;
import org.jetbrains.jet.buildergen.runtime.LiteralReference;
import org.jetbrains.jet.buildergen.runtime.ProxyReference;

import java.util.Collection;
import java.util.Map;

/**
 * @author abreslav
 */
public class BeanReferenceGenerator {

    @NotNull
    public static Collection<ClassModel> generateInterfaces(
            @NotNull final BeanGenerationContextImpl context,
            @NotNull String packageFqName
    ) {
        return EntityRepresentationGeneratorUtil.generateEntityRepresentations(
                context.getEntities(),
                ClassKind.INTERFACE,
                context.getReferenceInterfaces(),
                packageFqName,
                new EntityBeanGenerationStrategy() {
                    @NotNull
                    @Override
                    public String getEntityRepresentationName(@NotNull Entity entity) {
                        return entity.getName() + "BeanReference";
                    }

                    @Override
                    public void generateEntity(@NotNull Entity entity, @NotNull ClassBean classBean) {
                        classBean.getSuperInterfaces().add(TypeUtil.type(BeanReference.class));
                        classBean.getSuperInterfaces().addAll(
                                EntityRepresentationGeneratorUtil
                                        .entitiesToTypes(context.getReferenceInterfaces(), entity.getSuperEntities())
                        );
                    }
                }
        );
    }

    @NotNull
    public static Collection<ClassModel> generateLiteralClasses(
            @NotNull final BeanGenerationContextImpl context,
            @NotNull String packageFqName
    ) {
        return EntityRepresentationGeneratorUtil.generateEntityRepresentations(
                context.getEntities(),
                ClassKind.CLASS,
                context.getLiteralReferenceClasses(),
                packageFqName,
                new EntityBeanGenerationStrategy() {
                    @NotNull
                    @Override
                    public String getEntityRepresentationName(@NotNull Entity entity) {
                        return "Literal" + entity.getName() + "BeanReference";
                    }

                    @Override
                    public void generateEntity(@NotNull Entity entity, @NotNull ClassBean classBean) {
                        classBean.getSuperInterfaces().add(TypeUtil.type(context.getReferenceInterfaces().getRepresentation(entity)));
                        classBean.setSuperClass(TypeUtil.type(LiteralReference.class));

                        classBean.getConstructors().add(
                                createLiteralBeanReferenceConstructor()
                        );

                    }
                }
        );
    }

    @SuppressWarnings("unchecked")
    private static MethodBean createLiteralBeanReferenceConstructor() {
        final String parameterName = "data";
        return JavaDeclarationUtil.publicConstructor()
                .addParameter(
                        new ParameterBean()
                                .setType(TypeUtil.type(Object.class))
                                .setName(parameterName)
                )
                .put(
                        ClassPrinter.METHOD_BODY,
                        new PieceOfCode() {
                            @Override
                            public <E> E create(@NotNull CodeFactory<E> f) {
                                return CodeUtil.methodCallStatement(f, null, "super", f.variableReference(parameterName));
                            }
                        }
                );
    }

    @NotNull
    public static Collection<ClassModel> generateProxyClasses(
            @NotNull final BeanGenerationContextImpl context,
            @NotNull String packageFqName
    ) {
        return EntityRepresentationGeneratorUtil.generateEntityRepresentations(
                context.getEntities(),
                ClassKind.CLASS,
                context.getProxyReferenceClasses(),
                packageFqName,
                new EntityBeanGenerationStrategy() {
                    @NotNull
                    @Override
                    public String getEntityRepresentationName(@NotNull Entity entity) {
                        return "Proxy" + entity.getName() + "BeanReference";
                    }

                    @Override
                    public void generateEntity(@NotNull Entity entity, @NotNull ClassBean classBean) {
                        classBean.getSuperInterfaces().add(TypeUtil.type(context.getReferenceInterfaces().getRepresentation(entity)));
                        classBean.setSuperClass(TypeUtil.type(ProxyReference.class));

                        classBean.getConstructors().add(
                                createProxyBeanReferenceConstructor()
                        );

                    }
                }
        );
    }


    @SuppressWarnings("unchecked")
    private static MethodBean createProxyBeanReferenceConstructor() {
        final String map = "map";
        final String key = "key";
        final String _default = "_default";
        return JavaDeclarationUtil.publicConstructor()
                .addParameter(
                        new ParameterBean()
                                .addAnnotation(CommonAnnotations.NOT_NULL)
                                .setType(TypeUtil.typeWithTypeDataArguments(Map.class, TypeUtil.wildcard(null), TypeUtil.wildcard(null)))
                                .setName(map)
                )
                .addParameter(
                        new ParameterBean()
                                .setType(TypeUtil.type(Object.class))
                                .setName(key)
                )
                .addParameter(
                        new ParameterBean()
                                .setType(TypeUtil.type(Object.class))
                                .setName(_default)
                )
                .put(
                        ClassPrinter.METHOD_BODY,
                        new PieceOfCode() {
                            @Override
                            public <E> E create(@NotNull CodeFactory<E> f) {
                                return CodeUtil.methodCallStatement(f, null, "super",
                                                                    f.variableReference(map),
                                                                    f.variableReference(key),
                                                                    f.variableReference(_default));
                            }
                        }
                );
    }

    private BeanReferenceGenerator() {}
}
