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

import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.builders.generator.dataholder.DataHolderKey;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.*;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.beans.ClassBean;
import org.jetbrains.jet.lang.descriptors.builders.generator.dataholder.DataHolderKeyImpl;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.types.TypeData;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.types.TypeFactory;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.types.TypeUtil;

import java.util.*;

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
            classBean.getSuperInterfaces().add(TypeUtil.simpleType(context.getRepresentation(superEntity)));
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

    protected static TypeTransformer types(EntityRepresentationContext<ClassBean> context) {
        return new TypeTransformer(context);
    }
}
