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

package org.jetbrains.jet.buildergen.java.declarations.beans;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.buildergen.EntityRepresentationGenerator;
import org.jetbrains.jet.buildergen.java.declarations.MethodModel;
import org.jetbrains.jet.buildergen.java.types.TypeData;

/**
 * @author abreslav
 */
public class JavaDeclarationUtil {

    @NotNull
    public static MethodBean copy(@NotNull MethodModel method) {
        return new MethodBean()
                .setVisibility(method.getVisibility())
                .addAnnotations(method.getAnnotations())
                .setAbstract(method.isAbstract())
                .setReturnType(method.getReturnType())
                .setName(method.getName())
                .addParameters(method.getParameters())
                .copyDataFrom(method);
    }

    private JavaDeclarationUtil() {}

    public static ParameterBean notNullParameter(TypeData beanInterfaceType, String name) {
        return new ParameterBean()
                .addAnnotation(EntityRepresentationGenerator.NOT_NULL)
                .setType(beanInterfaceType)
                .setName(name);
    }
}
