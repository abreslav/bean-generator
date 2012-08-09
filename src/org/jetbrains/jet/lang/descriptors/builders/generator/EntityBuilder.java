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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.builders.generator.runtime.Optional;
import org.jetbrains.jet.lang.descriptors.builders.generator.runtime.Skip;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
* @author abreslav
*/
public class EntityBuilder {

    @NotNull
    public static Collection<Entity> javaClassesToEntities(@NotNull Collection<Class<?>> entityClassesCollection) {
        Context c = new Context(entityClassesCollection);

        createEmptyEntities(c);

        for (Class<?> entityClass : c.getEntityClasses()) {

            // Super entities
            createSuperEntities(c, entityClass);

            // Relations
            createRelations(c, entityClass);
        }

        return c.getEntities();
    }

    private static void createEmptyEntities(Context c) {
        for (Class<?> entityClass : c.entityClasses) {
            String name = entityClass.getSimpleName();
            c.entities.put(entityClass, new EntityImpl(name));
        }
    }

    private static void createSuperEntities(Context c, Class<?> entityClass) {
        Entity entity = c.safeGet(entityClass);
        List<Class<?>> superClassifiers = Lists.newArrayList(entityClass.getInterfaces());
        Class<?> superclass = entityClass.getSuperclass();
        if (superclass != null) {
            superClassifiers.add(superclass);
        }

        for (Class<?> classifier : superClassifiers) {
            if (c.isEntityClass(classifier)) {
                entity.getSuperEntities().add(c.safeGet(classifier));
            }
            else {
                warning("Skipping supertype " + classifier.getSimpleName() + " of " + entityClass);
            }
        }
    }

    private static void createRelations(Context c, Class<?> entityClass) {
        Entity entity = c.safeGet(entityClass);
        for (Method method : entityClass.getDeclaredMethods()) {
            if (method.getAnnotation(Skip.class) != null) {
                continue;
            }
            String relationName;
            String methodName = method.getName();
            if (methodName.startsWith("get")) {
                relationName = methodName.substring(3);
            }
            else if (methodName.startsWith("is")) {
                relationName = methodName.substring(2);
            }
            else {
                warning("[Wrong prefix] Skipping method " + method.getName() + " of " + entityClass);
                continue;
            }
            if (method.getParameterTypes().length > 0) {
                warning("[Wrong parameter count] Skipping method " + method.getName() + " of " + entityClass);
                continue;
            }
            Class<?> returnType = method.getReturnType();
            if (returnType == Void.TYPE) {
                warning("[Void return type] Skipping method " + method.getName() + " of " + entityClass);
                continue;
            }

            Relation relation = createRelation(c, method, relationName, returnType);
            entity.getRelations().add(relation);
        }
    }

    private static Relation createRelation(Context c, Method method, String relationName, Class<?> returnClass) {
        if (c.isEntityClass(returnClass)) {
            Multiplicity multiplicity = getMultiplicity(method);
            return new RelationWithTarget<Entity>(multiplicity, relationName, c.safeGet(returnClass));
        }
        Type returnType = method.getGenericReturnType();
        if (Collection.class.isAssignableFrom(returnClass)) {

            if (returnType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) returnType;
                Type[] arguments = parameterizedType.getActualTypeArguments();
                if (arguments.length != 1) {
                    warning("Unsupported number of type arguments for collection in " + method);
                    return createRelationToJavaType(method, relationName, returnType);
                }
                Class elementClass = getClassFromType(arguments[0]);
                if (c.isEntityClass(elementClass)) {
                    return new RelationWithTarget<Entity>(getMultiplicityFromCollectionType(returnClass),
                                                          relationName, c.safeGet(elementClass));
                }
                return createRelationToJavaType(method, relationName, returnType);
            }
            else {
                warning("Collection return type is not parameterized in " + method);
                return createRelationToJavaType(method, relationName, returnType);
            }
        }
        return createRelationToJavaType(method, relationName, returnType);
    }

    private static Class<?> getClassFromType(Type type) {
        if (type instanceof Class) {
            return  (Class) type;
        }
        if (type instanceof WildcardType) {
            return getClassFromType(((WildcardType) type).getUpperBounds()[0]);
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            return (Class<?>) parameterizedType.getRawType();
        }
        throw new IllegalArgumentException("Unsupported reflect.Type: " + type);
    }

    private static Multiplicity getMultiplicityFromCollectionType(Class<?> type) {
        if (List.class.isAssignableFrom(type)) {
            return Multiplicity.LIST;
        }
        if (Set.class.isAssignableFrom(type)) {
            return Multiplicity.SET;
        }
        return Multiplicity.COLLECTION;
    }

    private static RelationWithTarget<Type> createRelationToJavaType(Method method, String relationName, Type type) {
        return new RelationWithTarget<Type>(getMultiplicity(method), relationName, type);
    }

    private static Multiplicity getMultiplicity(Method method) {
        Optional optional = method.getAnnotation(Optional.class);
        return optional == null ? Multiplicity.ONE : Multiplicity.ZERO_OR_ONE;
    }

    private static void warning(String message) {
        System.err.println(message);
    }

    private static class Context {
        private final Set<Class<?>> entityClasses;
        private final Map<Class<?>, Entity> entities = Maps.newHashMap();

        public Context(@NotNull Collection<Class<?>> entityClassesCollection) {
            this.entityClasses = Sets.newHashSet(entityClassesCollection);
        }

        @NotNull
        public Entity safeGet(@NotNull Class<?> entityClass) {
            Entity entity = entities.get(entityClass);
            if (entity == null) {
                throw new IllegalStateException("Entity must have been created already: " + entityClass);
            }
            return entity;
        }

        public boolean isEntityClass(@NotNull Class<?> theClass) {
            return entityClasses.contains(theClass);
        }

        @NotNull
        public Iterable<Class<?>> getEntityClasses() {
            return entityClasses;
        }

        @NotNull
        public Collection<Entity> getEntities() {
            return entities.values();
        }
    }
}
