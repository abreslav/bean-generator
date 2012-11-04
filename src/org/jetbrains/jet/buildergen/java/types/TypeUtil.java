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

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.buildergen.EntityBuilder;
import org.jetbrains.jet.buildergen.entities.Entity;
import org.jetbrains.jet.buildergen.java.declarations.ClassModel;
import org.jetbrains.jet.buildergen.java.declarations.WildcardKind;

import java.util.Arrays;
import java.util.List;

/**
 * @author abreslav
 */
@SuppressWarnings("unchecked")
public class TypeUtil {

    private static final TypeData[] EMPTY = new TypeData[0];

    public static <E> E constructedType(
            @NotNull TypeFactory<E> factory,
            @NotNull String packageName, @NotNull String className, E... arguments
    ) {
        return factory.constructedType(packageName, className, Arrays.asList(arguments));
    }

    public static TypeData type(@NotNull final String packageName, @NotNull final String className, final TypeData... arguments) {
        return new TypeData() {
            @Override
            public <E> E create(@NotNull final TypeFactory<E> f) {
                List<E> argTypes = Lists.newArrayList(Collections2.transform(Arrays.asList(arguments),
                                                                             new Function<TypeData, E>() {
                                                                                 @Override
                                                                                 public E apply(TypeData data) {
                                                                                     return data.create(f);
                                                                                 }
                                                                             }));
                return f.constructedType(packageName, className, argTypes);
            }
        };
    }

    public static TypeData type(@NotNull ClassModel classModel, TypeData... arguments) {
        return type(classModel.getPackageFqName(), classModel.getName(), arguments);
    }

    public static TypeData typeWithTypeDataArguments(@NotNull Class<?> javaClass, TypeData... arguments) {
        return type(javaClass.getPackage().getName(), javaClass.getSimpleName(), arguments);
    }

    public static TypeData type(@NotNull Class<?> javaClass, Class<?>... arguments) {
        TypeData[] args = javaClassesToTypes(Arrays.asList(arguments)).toArray(new TypeData[arguments.length]);
        return type(javaClass.getPackage().getName(), javaClass.getSimpleName(), args);
    }

    @NotNull
    public static List<TypeData> javaClassesToTypes(List<Class<?>> arguments) {
        return Lists.newArrayList(Collections2.transform(arguments,
                                                         new Function<Class<?>, TypeData>() {
                                                             @Override
                                                             public TypeData apply(Class<?> data) {
                                                                 return typeWithTypeDataArguments(data, EMPTY);
                                                             }
                                                         }));
    }

    @NotNull
    public static List<TypeData> javaClassesToTypes(Class<?>... arguments) {
        return javaClassesToTypes(Arrays.asList(arguments));
    }

    public static TypeData _void() {
        return type("", "void");
    }

    public static TypeData getDataType(Entity entity) {
        EntityBuilder.ClassName dataClassName = entity.getData(EntityBuilder.DATA_CLASS);
        return type(dataClassName.getPackageFqName(), dataClassName.getClassName());
    }

    @NotNull
    public static TypeData wildcard(@Nullable final TypeData upperBound) {
        return new TypeData() {
            @Override
            public <E> E create(@NotNull TypeFactory<E> f) {
                return f.wildcardType(upperBound == null ? WildcardKind.BARE : WildcardKind.EXTENDS, upperBound == null ? null : upperBound.create(f));
            }
        };
    }
}