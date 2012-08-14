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
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.*;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.beans.ClassBean;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.beans.DataHolderKeyImpl;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.types.TypeData;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.types.TypeFactory;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.types.TypeUtil;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * @author abreslav
 */
public abstract class EntityRepresentationGenerator {
    protected static DataHolderKey<Entity> ENTITY = DataHolderKeyImpl.create("ENTITY");

    public static TypeData OVERRIDE = new TypeData() {
        @Override
        public <E> E create(@NotNull TypeFactory<E> f) {
            return TypeUtil.constructedType(f, "java.lang", "Override");
        }
    };

    public static TypeData NULLABLE = new TypeData() {
        @Override
        public <E> E create(@NotNull TypeFactory<E> f) {
            return TypeUtil.constructedType(f, "org.jetbrains.annotations", "Nullable");
        }
    };

    public static TypeData NOT_NULL = new TypeData() {
        @Override
        public <E> E create(@NotNull TypeFactory<E> f) {
            return TypeUtil.constructedType(f, "org.jetbrains.annotations", "NotNull");
        }
    };

    protected EntityRepresentationGenerator() {
    }

    @NotNull
    protected abstract ClassKind getClassKind();

    public Collection<ClassModel> generate(
            @NotNull Collection<Entity> entities,
            @NotNull EntityRepresentationContext<ClassBean> context,
            @NotNull String targetPackageFqName
    ) {
        preProcess(entities, context);

        for (Entity entity : entities) {
            String readableBeanClassName = getEntityRepresentationName(entity);
            ClassBean classBean = new ClassBean()
                    .setPackageFqName(targetPackageFqName)
                    .setVisibility(Visibility.PUBLIC)
                    .setKind(getClassKind())
                    .setName(readableBeanClassName)
                    .put(ENTITY, entity);
            context.registerRepresentation(entity, classBean);
        }

        for (Entity entity : entities) {
            generateEntity(context, entity);
        }

        postProcess(context);

        return (Collection) context.getRepresentations();
    }

    private void preProcess(Collection<Entity> entities, EntityRepresentationContext<ClassBean> context) {
        // Override if needed
    }

    protected void postProcess(EntityRepresentationContext<ClassBean> context) {
        // Override if needed
    }

    private void generateEntity(@NotNull EntityRepresentationContext<ClassBean> context, @NotNull Entity entity) {
        ClassBean classBean = context.getRepresentation(entity);

        generateSupertypes(context, classBean, entity);

        generateClassMembers(context, classBean, entity);
    }

    protected abstract void generateSupertypes(EntityRepresentationContext<ClassBean> context, ClassBean classBean, Entity entity);

    protected final void generateSupertypesFromSuperEntities(
            EntityRepresentationContext<ClassBean> context,
            ClassBean classBean,
            Entity entity
    ) {
        for (Entity superEntity : entity.getSuperEntities()) {
            classBean.getSuperInterfaces().add(simpleType(context.getRepresentation(superEntity)));
        }
    }

    protected abstract void generateClassMembers(EntityRepresentationContext<ClassBean> context, ClassBean bean, Entity entity);

    public abstract String getEntityRepresentationName(@NotNull Entity entity);

    public static String getGetterName(Relation relation) {
        return getGetterPrefix(relation.getTarget()) + relation.getName();
    }

    public static String getSetterName(Relation relation) {
        return "set" + relation.getName();
    }

    public static String getFieldName(Relation relation) {
        return StringUtil.decapitalize(relation.getName());
    }

    private static <T> String getGetterPrefix(T target) {
        return target == Boolean.TYPE ? "is" : "get";
    }

    protected static TypeData simpleType(@NotNull final ClassModel classModel) {
        return new TypeData() {
            @Override
            public <E> E create(@NotNull TypeFactory<E> f) {
                return TypeUtil.constructedType(f, classModel.getPackageFqName(), classModel.getName());
            }
        };
    }

    protected static TypeTransformer types(EntityRepresentationContext<ClassBean> context) {
        return new TypeTransformer(context);
    }

    protected static class TypeTransformer {

        protected enum Variance {
            NONE,
            IN,
            OUT
        }

        private final EntityRepresentationContext<ClassBean> context;

        public TypeTransformer(EntityRepresentationContext<ClassBean> context) {
            this.context = context;
        }

        protected <T> TypeData relationToType(@NotNull Relation<T> relation) {
            return targetToType(relation.getTarget(), relation.getMultiplicity(), Variance.NONE);
        }

        protected <T> TypeData relationToVariantType(@NotNull Relation<T> relation, @NotNull Variance variance) {
            return targetToType(relation.getTarget(), relation.getMultiplicity(), variance);
        }

        protected <T> TypeData targetToType(T target, Multiplicity multiplicity) {
            return targetToType(target, multiplicity, Variance.NONE);
        }

        protected <T> TypeData targetToType(T target, Multiplicity multiplicity, Variance variance) {
            if (target instanceof Entity) {
                Entity entity = (Entity) target;
                return typeWithMultiplicity(multiplicity, simpleType(context.getRepresentation(entity)), variance);
            }
            else if (target instanceof Type) {
                Type type = (Type) target;
                return typeWithMultiplicity(multiplicity, reflectionType(type), variance);
            }
            throw new IllegalArgumentException("Unsupported target type:" + target);
        }

        protected static TypeData typeWithMultiplicity(Multiplicity multiplicity, TypeData elementType, Variance variance) {
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

        protected static TypeData collectionType(final Class<? extends Collection> aClass, final Variance variance, final TypeData type) {
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

        private static <E> E wildcard(TypeFactory<E> f, Variance variance, E e) {
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

        protected static TypeData reflectionType(@NotNull Type type) {
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
}
