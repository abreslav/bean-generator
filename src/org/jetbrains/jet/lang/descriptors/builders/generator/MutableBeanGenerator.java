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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.All;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.AllImpl;

import java.util.Collection;

/**
* @author abreslav
*/
public class MutableBeanGenerator extends EntityRepresentationGenerator {

    public MutableBeanGenerator(@NotNull Collection<Entity> entities, @NotNull String targetPackageFqName) {
        super(entities, targetPackageFqName);
    }

    @Override
    protected void generateClassMembers(AllImpl.ClassBean bean, Entity entity) {
        for (Relation<?> relation : entity.getRelations()) {
            bean.getFields().add(new AllImpl.FieldBean()
                                         .setVisibility(All.Visibility.PRIVATE)
                                         .setType(relationToType(relation))
                                         .setName(getFieldName(relation)));
        }
        for (Relation<?> relation : entity.getRelations()) {
            bean.getMethods().add(new AllImpl.MethodBean()
                                          .addAnnotation(OVERRIDE)
                                          .setVisibility(All.Visibility.PUBLIC)
                                          .setReturnType(relationToType(relation))
                                          .setName(getGetterName(relation))
            );
        }
    }

    @Override
    public String getEntityRepresentationName(@NotNull Entity entity) {
        return entity.getName() + "Bean";
    }
}
