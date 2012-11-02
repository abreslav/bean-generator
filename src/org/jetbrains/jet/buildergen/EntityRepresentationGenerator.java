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
import org.jetbrains.jet.buildergen.dataholder.DataHolderKey;
import org.jetbrains.jet.buildergen.dataholder.DataHolderKeyImpl;
import org.jetbrains.jet.buildergen.entities.Entity;
import org.jetbrains.jet.buildergen.entities.Relation;
import org.jetbrains.jet.buildergen.java.declarations.ClassKind;
import org.jetbrains.jet.buildergen.java.declarations.ClassModel;
import org.jetbrains.jet.buildergen.java.declarations.Visibility;
import org.jetbrains.jet.buildergen.java.declarations.beans.ClassBean;
import org.jetbrains.jet.buildergen.java.types.TypeData;
import org.jetbrains.jet.buildergen.java.types.TypeFactory;
import org.jetbrains.jet.buildergen.java.types.TypeUtil;

import java.util.Collection;

/**
 * @author abreslav
 */
public abstract class EntityRepresentationGenerator {
    protected static DataHolderKey<ClassModel, Entity> ENTITY = DataHolderKeyImpl.create("ENTITY");

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
            classBean.getSuperInterfaces().add(TypeUtil.type(context.getRepresentation(superEntity)));
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
        return GeneratorUtil.variableNameByRelation(relation);
    }

    private static <T> String getGetterPrefix(T target) {
        return target == Boolean.TYPE || target == Boolean.class ? "is" : "get";
    }

    protected static TypeTransformer types(EntityRepresentationContext<ClassBean> context) {
        return new TypeTransformer(context);
    }
}
