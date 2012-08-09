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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.ClassPrinter;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.code.CodeFactory;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.code.PieceOfCode;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.FieldModel;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.Visibility;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.beans.ClassBean;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.beans.FieldBean;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.beans.MethodBean;

import java.util.Collection;
import java.util.Map;

/**
* @author abreslav
*/
public class MutableBeanGenerator extends EntityRepresentationGenerator {

    public MutableBeanGenerator(@NotNull Collection<Entity> entities, @NotNull String targetPackageFqName) {
        super(entities, targetPackageFqName);
    }

    @Override
    protected void generateClassMembers(ClassBean bean, Entity entity) {
        final Map<Relation<?>, FieldModel> fields = Maps.newHashMap();
        for (Relation<?> relation : entity.getRelations()) {
            FieldBean field = new FieldBean()
                    .setVisibility(Visibility.PRIVATE)
                    .setType(relationToType(relation))
                    .setName(getFieldName(relation));
            fields.put(relation, field);
            bean.getFields().add(field);
        }
        for (final Relation<?> relation : entity.getRelations()) {
            bean.getMethods().add(new MethodBean()
                                          .addAnnotation(OVERRIDE)
                                          .setVisibility(Visibility.PUBLIC)
                                          .setReturnType(relationToType(relation))
                                          .setName(getGetterName(relation))
                                          .put(
                                              ClassPrinter.METHOD_BODY,
                                              new PieceOfCode() {
                                                  @NotNull
                                                  @Override
                                                  public <E> E create(@NotNull CodeFactory<E> f) {
                                                      return f._return(
                                                          f.fieldReference(f._this(), fields.get(relation))
                                                      );
                                                  }
                                              }
                                          )
            );
        }
    }

    @Override
    public String getEntityRepresentationName(@NotNull Entity entity) {
        return entity.getName() + "Bean";
    }
}
