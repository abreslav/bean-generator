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

import com.google.common.collect.Maps;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.*;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.beans.ClassBean;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.beans.TypeBean;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author abreslav
 */
public abstract class EntityRepresentationGenerator {

    public static TypeModel OVERRIDE = new TypeBean()
            .setPackageFqName("java.lang")
            .setClassName("Override");

    public static TypeModel NULLABLE = new TypeBean()
            .setPackageFqName("org.jetbrains.annotations")
            .setClassName("Nullable");

    public static TypeModel NOT_NULL = new TypeBean()
            .setPackageFqName("org.jetbrains.annotations")
            .setClassName("NotNull");

    private final Map<Entity, ClassBean> map = Maps.newIdentityHashMap();

    protected EntityRepresentationGenerator(@NotNull Collection<Entity> entities, @NotNull String targetPackageFqName) {
        for (Entity entity : entities) {
            String readableBeanClassName = getEntityRepresentationName(entity);
            ClassBean classBean = new ClassBean()
                    .setPackageFqName(targetPackageFqName)
                    .setVisibility(Visibility.PUBLIC)
                    .setKind(getClassKind())
                    .setName(readableBeanClassName);
            map.put(entity, classBean);
        }
    }

    @NotNull
    protected ClassKind getClassKind() {
        return ClassKind.CLASS;
    }

    public Collection<ClassModel> generate() {
        for (Entity entity : map.keySet()) {
            generateEntity(entity);
        }
        return (Collection) map.values();
    }

    public void generateEntity(@NotNull Entity entity) {
        ClassBean classBean = map.get(entity);

        generateSupertypes(classBean, entity);

        generateClassMembers(classBean, entity);
    }

    protected void generateSupertypes(ClassBean classBean, Entity entity) {
        for (Entity superEntity : entity.getSuperEntities()) {
            classBean.getSuperInterfaces().add(simpleType(map.get(superEntity)));
        }
    }

    protected abstract void generateClassMembers(ClassBean bean, Entity entity);

    public abstract String getEntityRepresentationName(@NotNull Entity entity);

    protected <T> TypeModel relationToType(@NotNull Relation<T> relation) {
        T target = relation.getTarget();
        if (target instanceof Entity) {
            Entity entity = (Entity) target;
            return typeWithMultiplicity(relation.getMultiplicity(), simpleType(map.get(entity)));
        }
        else if (target instanceof Type) {
            Type type = (Type) target;
            return typeWithMultiplicity(relation.getMultiplicity(), reflectionType(type));
        }
        throw new IllegalArgumentException("Unsupported target type:" + target);
    }

    protected TypeModel typeWithMultiplicity(Multiplicity multiplicity, TypeModel elementType) {
        switch (multiplicity) {
            case ZERO_OR_ONE:
            case ONE:
                return elementType;
            case LIST:
                return reflectionTypeBean(List.class).addArgument(elementType);
            case SET:
                return reflectionTypeBean(Set.class).addArgument(elementType);
            case COLLECTION:
                return reflectionTypeBean(Collection.class).addArgument(elementType);
        }
        throw new IllegalStateException("Unknown multiplicity: " + multiplicity);
    }

    protected static TypeModel reflectionType(@NotNull Type type) {
        return reflectionTypeBean(type);
    }

    protected static TypeBean reflectionTypeBean(Type type) {
        if (type instanceof Class<?>) {
            Class<?> theClass = (Class<?>) type;
            return classToTypeBean(theClass);
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            TypeBean typeBean = classToTypeBean((Class<?>) parameterizedType.getRawType());
            Type[] arguments = parameterizedType.getActualTypeArguments();
            for (Type arg : arguments) {
                typeBean.getArguments().add(reflectionType(arg));
            }
            return typeBean;
        }
        throw new IllegalArgumentException("Unsupported reflection type: " + type);
    }

    private static TypeBean classToTypeBean(Class<?> theClass) {
        Package aPackage = theClass.getPackage();
        return new TypeBean()
                    .setPackageFqName(aPackage == null ? "" : aPackage.getName())
                    .setClassName(theClass.getSimpleName());
    }

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

    protected static TypeModel simpleType(@NotNull ClassModel classModel) {
        return new TypeBean()
                .setClassName(classModel.getName())
                .setPackageFqName(classModel.getPackageFqName());
    }
}
