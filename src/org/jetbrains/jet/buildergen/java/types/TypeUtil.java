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

package org.jetbrains.jet.buildergen.java.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.buildergen.EntityBuilder;
import org.jetbrains.jet.buildergen.entities.Entity;
import org.jetbrains.jet.buildergen.java.declarations.ClassModel;

import java.util.Arrays;

/**
 * @author abreslav
 */
public class TypeUtil {
    public static <E> E constructedType(
            @NotNull TypeFactory<E> factory,
            @NotNull String packageName, @NotNull String className, E... arguments
    ) {
        return factory.constructedType(packageName, className, Arrays.asList(arguments));
    }

    public static TypeData simpleType(@NotNull final ClassModel classModel) {
        return simpleType(classModel.getPackageFqName(), classModel.getName());
    }

    public static TypeData simpleType(@NotNull final String packageName, @NotNull final String className) {
        return new TypeData() {
            @Override
            public <E> E create(@NotNull TypeFactory<E> f) {
                return constructedType(f, packageName, className);
            }
        };
    }

    public static TypeData _void() {
        return simpleType("", "void");
    }

    public static TypeData getDataType(Entity entity) {
        EntityBuilder.ClassName dataClassName = entity.getData(EntityBuilder.DATA_CLASS);
        return simpleType(dataClassName.getPackageFqName(), dataClassName.getClassName());
    }
}
