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

package org.jetbrains.jet.lang.descriptors.builders.generator;

import com.google.common.collect.Lists;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.WildcardKind;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.beans.ClassBean;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.types.TypeData;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.types.TypeFactory;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.types.TypeUtil;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
* @author abreslav
*/
public class TypeTransformer {

    protected enum Variance {
        NONE,
        IN,
        OUT
    }

    private final EntityRepresentationContext<ClassBean> context;

    public TypeTransformer(EntityRepresentationContext<ClassBean> context) {
        this.context = context;
    }

    public  <T> TypeData relationToType(@NotNull Relation<T> relation) {
        return targetToType(relation.getTarget(), relation.getMultiplicity(), Variance.NONE);
    }

    public  <T> TypeData relationToVariantType(@NotNull Relation<T> relation, @NotNull Variance variance) {
        return targetToType(relation.getTarget(), relation.getMultiplicity(), variance);
    }

    public  <T> TypeData targetToType(T target, Multiplicity multiplicity) {
        return targetToType(target, multiplicity, Variance.NONE);
    }

    public <T> TypeData targetToType(T target, Multiplicity multiplicity, Variance variance) {
        if (target instanceof Entity) {
            Entity entity = (Entity) target;
            return typeWithMultiplicity(multiplicity, TypeUtil.simpleType(context.getRepresentation(entity)), variance);
        }
        else if (target instanceof Type) {
            Type type = (Type) target;
            return typeWithMultiplicity(multiplicity, reflectionType(type), variance);
        }
        throw new IllegalArgumentException("Unsupported target type:" + target);
    }

    public static TypeData typeWithMultiplicity(Multiplicity multiplicity, TypeData elementType, Variance variance) {
        switch (multiplicity) {
            case ZERO_OR_ONE:
            case ONE:
                return elementType;
            case LIST:
                return collectionType(List.class, variance, elementType);
            case SET:
                return collectionType(Set.class, variance, elementType);
            case COLLECTION:
                return collectionType(Collection.class, variance, elementType);
        }
        throw new IllegalStateException("Unknown multiplicity: " + multiplicity);
    }

    public static TypeData collectionType(final Class<? extends Collection> aClass, final Variance variance, final TypeData type) {
        return new TypeData() {
            @Override
            public <E> E create(@NotNull TypeFactory<E> f) {
                return f.constructedType(
                        aClass.getPackage().getName(),
                        getNameWithEnclosingClasses(aClass),
                        Collections.singletonList(wildcard(f, variance, type.create(f))));
            }
        };
    }

    public static <E> E wildcard(TypeFactory<E> f, Variance variance, E e) {
        switch (variance) {
            case NONE:
                return e;
            case IN:
                return f.wildcardType(WildcardKind.SUPER, e);
            case OUT:
                return f.wildcardType(WildcardKind.EXTENDS, e);
        }
        throw new IllegalStateException("Unknown variance: " + variance);
    }

    public static TypeData reflectionType(@NotNull Type type) {
        if (type instanceof Class<?>) {
            Class<?> theClass = (Class<?>) type;
            return classToTypeBean(theClass);
        }
        if (type instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) type;
            return new TypeData() {
                @Override
                public <E> E create(@NotNull final TypeFactory<E> f) {
                    Class<?> rawType = (Class<?>) parameterizedType.getRawType();
                    return f.constructedType(
                            rawType.getPackage().getName(),
                            getNameWithEnclosingClasses(rawType),
                            reflectionTypes(f, parameterizedType.getActualTypeArguments())
                    );
                }
            };
        }
        throw new IllegalArgumentException("Unsupported reflection type: " + type);
    }

    private static <E> List<E> reflectionTypes(final TypeFactory<E> f, Type... types) {
        return ContainerUtil.map(types, new Function<Type, E>() {
            @Override
            public E fun(Type type) {
                return reflectionType(type).create(f);
            }
        });
    }

    private static TypeData classToTypeBean(final Class<?> theClass) {
        assert theClass.getPackage() != null;
        return new TypeData() {
            @Override
            public <E> E create(@NotNull TypeFactory<E> f) {
                return TypeUtil.constructedType(f, theClass.getPackage().getName(), getNameWithEnclosingClasses(theClass));
            }
        };
    }

    private static String getNameWithEnclosingClasses(@NotNull Class<?> aClass) {
        List<String> names = Lists.newArrayList();
        Class<?> c = aClass;
        while (c != null) {
            names.add(c.getSimpleName());
            c = c.getEnclosingClass();
        }
        Collections.reverse(names);
        return StringUtil.join(names, ".");
    }
}
