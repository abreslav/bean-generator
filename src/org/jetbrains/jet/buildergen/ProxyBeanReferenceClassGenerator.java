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
import org.jetbrains.jet.buildergen.java.code.CodeFactory;
import org.jetbrains.jet.buildergen.java.code.PieceOfCode;
import org.jetbrains.jet.buildergen.java.declarations.ClassKind;
import org.jetbrains.jet.buildergen.java.declarations.ClassModel;
import org.jetbrains.jet.buildergen.java.declarations.beans.ClassBean;
import org.jetbrains.jet.buildergen.java.declarations.beans.JavaDeclarationUtil;
import org.jetbrains.jet.buildergen.java.declarations.beans.ParameterBean;
import org.jetbrains.jet.buildergen.java.types.TypeUtil;
import org.jetbrains.jet.buildergen.runtime.ProxyReference;

import java.util.Map;

import static org.jetbrains.jet.buildergen.java.ClassPrinter.METHOD_BODY;
import static org.jetbrains.jet.buildergen.java.code.CodeUtil.methodCallStatement;

/**
 * @author abreslav
 */
public class ProxyBeanReferenceClassGenerator extends EntityRepresentationGenerator {

    private final EntityRepresentationContext<? extends ClassModel> beanReferenceInterfaces;

    public ProxyBeanReferenceClassGenerator(@NotNull EntityRepresentationContext<? extends ClassModel> beanReferenceInterfaces) {
        this.beanReferenceInterfaces = beanReferenceInterfaces;
    }

    @NotNull
    @Override
    protected ClassKind getClassKind() {
        return ClassKind.CLASS;
    }

    @Override
    public String getEntityRepresentationName(@NotNull Entity entity) {
        return "Proxy" + entity.getName() + "BeanReference";
    }

    @Override
    protected void generateSupertypes(EntityRepresentationContext<ClassBean> context, ClassBean classBean, Entity entity) {
        classBean.getSuperInterfaces().add(TypeUtil.type(beanReferenceInterfaces.getRepresentation(entity)));
        classBean.setSuperClass(TypeUtil.type(ProxyReference.class));
    }

    @Override
    protected void generateClassMembers(EntityRepresentationContext<ClassBean> context, ClassBean classBean, Entity entity) {
        final String map = "map";
        final String key = "key";
        classBean
                .getConstructors().add(
                JavaDeclarationUtil.publicConstructor()
                        .addParameter(
                                new ParameterBean()
                                        .addAnnotation(NOT_NULL)
                                        .setType(TypeUtil.typeWithTypeDataArguments(Map.class, TypeUtil.wildcard(null), TypeUtil.wildcard(null)))
                                        .setName(map)
                        )
                        .addParameter(
                                new ParameterBean()
                                        .setType(TypeUtil.type(Object.class))
                                        .setName(key)
                        )
                        .put(
                                METHOD_BODY,
                                new PieceOfCode() {
                                    @Override
                                    public <E> E create(@NotNull CodeFactory<E> f) {
                                        return methodCallStatement(f, null, "super", f.variableReference(map), f.variableReference(key));
                                    }
                                }
                        )
        );
    }
}
