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
import org.jetbrains.jet.lang.descriptors.builders.generator.entities.Entity;
import org.jetbrains.jet.lang.descriptors.builders.generator.entities.Multiplicity;
import org.jetbrains.jet.lang.descriptors.builders.generator.entities.Relation;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.ClassKind;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.Visibility;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.beans.ClassBean;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.beans.MethodBean;

/**
* @author abreslav
*/
public class ReadOnlyBeanGenerator extends EntityRepresentationGenerator {

    @Override
    public String getEntityRepresentationName(@NotNull Entity entity) {
        return getReadOnlyBeanInterfaceName(entity);
    }

    public static String getReadOnlyBeanInterfaceName(Entity entity) {
        return entity.getName() + "ReadableBean";
    }

    @NotNull
    @Override
    protected ClassKind getClassKind() {
        return ClassKind.INTERFACE;
    }

    @Override
    protected void generateSupertypes(EntityRepresentationContext<ClassBean> context, ClassBean classBean, Entity entity)  {
        generateSupertypesFromSuperEntities(context, classBean, entity);
    }

    @Override
    protected void generateClassMembers(EntityRepresentationContext<ClassBean> context, @NotNull ClassBean bean, @NotNull Entity entity) {
        TypeTransformer types = types(context);
        for (Relation<?> relation : entity.getRelations()) {
            bean.getMethods().add(new MethodBean()
                                          .addAnnotation(relation.getMultiplicity() == Multiplicity.ZERO_OR_ONE ? NULLABLE : NOT_NULL)
                                          .setVisibility(Visibility.PUBLIC)
                                          .setAbstract(true)
                                          .setReturnType(types.relationToVariantType(relation, TypeTransformer.Variance.OUT))
                                          .setName(getGetterName(relation))
            );
        }
    }
}
