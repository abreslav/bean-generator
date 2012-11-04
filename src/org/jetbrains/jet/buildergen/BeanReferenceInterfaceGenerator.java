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
import org.jetbrains.jet.buildergen.java.declarations.beans.ClassBean;
import org.jetbrains.jet.buildergen.java.declarations.beans.JavaDeclarationUtil;
import org.jetbrains.jet.buildergen.java.declarations.beans.ParameterBean;
import org.jetbrains.jet.buildergen.java.types.TypeUtil;
import org.jetbrains.jet.buildergen.runtime.BeanReference;

import java.util.Map;

import static org.jetbrains.jet.buildergen.java.ClassPrinter.METHOD_BODY;
import static org.jetbrains.jet.buildergen.java.code.CodeUtil.methodCallStatement;

/**
 * @author abreslav
 */
public class BeanReferenceInterfaceGenerator extends EntityRepresentationGenerator {

    @NotNull
    @Override
    protected ClassKind getClassKind() {
        return ClassKind.INTERFACE;
    }

    @Override
    public String getEntityRepresentationName(@NotNull Entity entity) {
        return entity.getName() + "BeanReference";
    }

    @Override
    protected void generateSupertypes(EntityRepresentationContext<ClassBean> context, ClassBean classBean, Entity entity) {
        classBean.getSuperInterfaces().add(TypeUtil.type(BeanReference.class));
        generateSupertypesFromSuperEntities(context, classBean, entity);
    }

    @Override
    protected void generateClassMembers(EntityRepresentationContext<ClassBean> context, ClassBean classBean, Entity entity) {
    }
}
