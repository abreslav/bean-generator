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

import com.google.common.collect.*;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.buildergen.dataholder.DataHolderKey;
import org.jetbrains.jet.buildergen.dataholder.DataHolderKeyImpl;
import org.jetbrains.jet.buildergen.entities.*;
import org.jetbrains.jet.buildergen.java.declarations.beans.ClassBean;
import org.jetbrains.jet.buildergen.runtime.Optional;
import org.jetbrains.jet.buildergen.runtime.Reference;
import org.jetbrains.jet.buildergen.runtime.Skip;

import java.lang.annotation.Annotation;
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

    public static final DataHolderKey<Entity, ClassName> DATA_CLASS = DataHolderKeyImpl.create("DATA_CLASS");
    public static final DataHolderKey<Relation<?>, Boolean> REFERENCE = DataHolderKeyImpl.create("REFERENCE");
    private static final DataHolderKey<Relation<?>, Boolean> SKIPPED = DataHolderKeyImpl.create("SKIPPED");
    public static final DataHolderKey<Relation<?>, Boolean> NULLABLE = DataHolderKeyImpl.create("NULLABLE");
    public static final DataHolderKey<Relation<?>, Boolean> NOT_NULL = DataHolderKeyImpl.create("NOT_NULL");

    private static final Map<Type, Class<?>> PRIMITIVE_TO_BOXED = ImmutableMap.<Type, Class<?>>builder()
            .put(byte.class, Byte.class)
            .put(short.class, Short.class)
            .put(int.class, Integer.class)
            .put(long.class, Long.class)
            .put(float.class, Float.class)
            .put(double.class, Double.class)
            .put(char.class, Character.class)
            .put(boolean.class, Boolean.class)
            .build();

    public static void javaClassesToEntities(@NotNull Collection<? extends Class<?>> entityClasses, @NotNull EntityRepresentationContextImpl context) {
        Context c = new Context(entityClasses);

        createEmptyEntities(c, context);

        for (Class<?> entityClass : c.getEntityClasses()) {

            // Super entities
            createSuperEntities(c, entityClass);

            // Relations
            createRelations(c, entityClass);
        }
        bindOverriddenRelations(c.getEntities());

        removeOverriddenRelations(c.getEntities());

        // To make @Skip "inherited"
        removeSkippedRelations(c.getEntities());
    }

    private static void createEmptyEntities(Context c, EntityRepresentationContextImpl context) {
        for (Class<?> entityClass : c.entityClasses) {
            String name = entityClass.getSimpleName();
            EntityImpl entity = new EntityImpl(name);
            entity.put(DATA_CLASS, new ClassName(entityClass.getPackage().getName(), entityClass.getSimpleName()));
            context.registerRepresentation(entity, new ClassBean().setPackageFqName(entityClass.getPackage().getName()).setName(entityClass.getSimpleName()));
            c.entities.put(entityClass, entity);
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

            RelationWithTarget<?> relation = createRelation(c, method, relationName, returnType);

            markIfAnnotationPresent(method, relation, Reference.class, REFERENCE);
            markIfAnnotationPresent(method, relation, Skip.class, SKIPPED);

            // @Nullable and @NotNull have retention policy CLASS, so we'd need ASM to obtain them
            //markIfAnnotationPresent(method, relation, Nullable.class, NULLABLE);
            //markIfAnnotationPresent(method, relation, NotNull.class, NOT_NULL);

            entity.getRelations().add(relation);
        }
    }

    private static void markIfAnnotationPresent(
            Method method,
            RelationWithTarget<?> relation,
            Class<? extends Annotation> annotationClass,
            DataHolderKey<Relation<?>, Boolean> key
    ) {
        if (method.isAnnotationPresent(annotationClass)) {
            relation.put(key, true);
        }
    }

    private static RelationWithTarget<?> createRelation(Context c, Method method, String relationName, Class<?> returnClass) {
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
                return new RelationWithTarget<Type>(getMultiplicityFromCollectionType(returnClass),
                                                    relationName, elementClass);
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
        Class<?> boxed = PRIMITIVE_TO_BOXED.get(type);
        return new RelationWithTarget<Type>(getMultiplicity(method), relationName, boxed == null ? type : boxed);
    }

    private static Multiplicity getMultiplicity(Method method) {
        Optional optional = method.getAnnotation(Optional.class);
        return optional == null ? Multiplicity.ONE : Multiplicity.ZERO_OR_ONE;
    }

    private static void bindOverriddenRelations(Collection<Entity> entities) {
        Set<Entity> alreadyBound = Sets.newHashSet();
        for (Entity entity : entities) {
            bindOverriddenRelations(entity, alreadyBound);
        }
    }

    private static void bindOverriddenRelations(Entity entity, Set<Entity> alreadyBound) {
        if (!alreadyBound.add(entity)) return;
        Multimap<String, Relation<?>> superRelations = HashMultimap.create();
        for (Entity superEntity : entity.getSuperEntities()) {
            bindOverriddenRelations(superEntity, alreadyBound);
            for (Relation<?> relation : superEntity.getRelations()) {
                superRelations.put(relation.getName(), relation);
            }
        }
        Set<String> explicitlyOverridden = Sets.newHashSet();
        for (Relation<?> relation : Lists.newArrayList(entity.getRelations())) {
            relation.getOverriddenRelations().addAll(superRelations.get(relation.getName()));
            explicitlyOverridden.add(relation.getName());
        }

        // "fake overrides"
        for (Map.Entry<String, Collection<Relation<?>>> entry : superRelations.asMap().entrySet()) {
            String relationName = entry.getKey();
            Collection<Relation<?>> overriddenRelations = entry.getValue();
            Relation<?> someOverridden = ContainerUtil.getFirstItem(overriddenRelations);
            RelationWithTarget<Object> fakeOverride =
                    new RelationWithTarget<Object>(someOverridden.getMultiplicity(), relationName, someOverridden.getTarget());
            fakeOverride.getOverriddenRelations().addAll(overriddenRelations);
            entity.getRelations().add(fakeOverride);
        }
    }

    private static void removeOverriddenRelations(Collection<Entity> entities) {
        for (Entity entity : entities) {
            for (Relation<?> relation : Lists.newArrayList(entity.getRelations())) {
                if (!relation.getOverriddenRelations().isEmpty()) {
                    warning("[Overridden relation]: Removing " + entity + "::" + relation);
                    entity.getRelations().remove(relation);
                }
            }
        }
    }

    private static void removeSkippedRelations(Collection<Entity> entities) {
        for (Entity entity : entities) {
            for (Relation<?> relation : Lists.newArrayList(entity.getRelations())) {
                if (relation.getData(SKIPPED) == Boolean.TRUE) {
                    warning("[Skipped relation]: Removing " + entity + "::" + relation);
                    entity.getRelations().remove(relation);
                }
            }
        }
    }

    private static void warning(String message) {
        System.err.println(message);
    }

    private static class Context {
        private final Set<Class<?>> entityClasses;
        private final Map<Class<?>, Entity> entities = Maps.newLinkedHashMap();

        public Context(@NotNull Collection<? extends Class<?>> entityClassesCollection) {
            this.entityClasses = Sets.newLinkedHashSet(entityClassesCollection);
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

    public static class ClassName {
        private final String packageFqName;

        private final String className;

        public ClassName(@NotNull String packageFqName, @NotNull String className) {
            this.packageFqName = packageFqName;
            this.className = className;
        }

        public String getPackageFqName() {
            return packageFqName;
        }
        public String getClassName() {
            return className;
        }

    }
}
