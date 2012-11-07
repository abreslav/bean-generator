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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
import org.jetbrains.jet.buildergen.java.types.TypeUtil;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author abreslav
 */
@SuppressWarnings("unchecked")
public class EntityRepresentationGeneratorUtil {
    public static final DataHolderKey<ClassModel, Entity> ENTITY = DataHolderKeyImpl.create("ENTITY");

    private EntityRepresentationGeneratorUtil() {
    }

    public static Collection<ClassModel> generateEntityRepresentations(
            @NotNull Collection<Entity> entities,
            @NotNull ClassKind generatedClassKind,
            @NotNull EntityRepresentationTrace<? super ClassBean> trace,
            @NotNull String targetPackageFqName,
            @NotNull EntityBeanGenerationStrategy strategy
    ) {
        Map<Entity, ClassBean> beans = Maps.newLinkedHashMap();

        for (Entity entity : entities) {
            String readableBeanClassName = strategy.getEntityRepresentationName(entity);
            ClassBean classBean = new ClassBean()
                    .setPackageFqName(targetPackageFqName)
                    .setVisibility(Visibility.PUBLIC)
                    .setKind(generatedClassKind)
                    .setName(readableBeanClassName)
                    .put(ENTITY, entity);
            trace.registerRepresentation(entity, classBean);
            beans.put(entity, classBean);
        }

        for (Entity entity : entities) {
            strategy.generateEntity(entity, beans.get(entity));
        }

        //noinspection unchecked
        return (Collection) beans.values();
    }

    public static List<TypeData> entitiesToTypes(
            EntityRepresentationContext<? extends ClassModel> context,
            Collection<Entity> entities
    ) {
        List<TypeData> result = Lists.newArrayList();
        for (Entity superEntity : entities) {
            result.add(TypeUtil.type(context.getRepresentation(superEntity)));
        }
        return result;
    }

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
}
