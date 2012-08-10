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
import org.jetbrains.jet.lang.descriptors.builders.generator.java.code.CodeUtil;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.code.PieceOfCode;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.FieldModel;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.MethodModel;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.Visibility;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.beans.ClassBean;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.beans.FieldBean;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.beans.MethodBean;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.beans.ParameterBean;

import java.util.Map;

/**
* @author abreslav
*/
public class MutableBeanClassesGenerator extends EntityRepresentationGenerator {

    @Override
    protected void generateSupertypes(EntityRepresentationContext<ClassBean> context, ClassBean classBean, Entity entity) {

    }

    @Override
    protected void generateClassMembers(EntityRepresentationContext<ClassBean> context, ClassBean bean, Entity entity) {
        Context c = new Context(context, entity, bean);
        createFields(c);
        createGetters(c);
        createSettersAndAdders(c);
    }

    private void createSettersAndAdders(Context context) {
        for (final Relation<?> relation : context.entity.getRelations()) {
            if (!relation.getMultiplicity().isCollection()) {
                context.classBean.getMethods().add(createSetter(context, relation));
            }
            else {
                context.classBean.getMethods().add(createSingleElementAdder(context, relation));
                context.classBean.getMethods().add(createAllElementAdder(context, relation));
            }
        }
    }

    private MethodModel createAllElementAdder(final Context context, final Relation<?> relation) {
        assert relation.getMultiplicity().isCollection();

        return createSelfReturningMethod(context.classBean)
                .setName(getAllElementAdderName(relation))
                .addParameter(createAllAdderParameter(context.types, relation))
                .put(
                        ClassPrinter.METHOD_BODY,
                        new PieceOfCode() {
                            @NotNull
                            @Override
                            public <E> E create(@NotNull CodeFactory<E> f) {
                                return CodeUtil.block(f,
                                                      CodeUtil.methodCallStatement(f,
                                                                                   f.fieldReference(f._this(),
                                                                                                    context.fields.get(relation).getName()),
                                                                                   "add",
                                                                                   f.variableReference("value")),
                                                      f._return(f._this())
                                );
                            }
                        }
                );
    }

    private MethodModel createSingleElementAdder(final Context context, final Relation<?> relation) {
        assert relation.getMultiplicity().isCollection();

        return createSelfReturningMethod(context.classBean)
                .setName(getSingleElementAdderName(relation))
                .addParameter(createSetterParameter(context.types, relation))
                .put(
                        ClassPrinter.METHOD_BODY,
                        new PieceOfCode() {
                            @NotNull
                            @Override
                            public <E> E create(@NotNull CodeFactory<E> f) {
                                return CodeUtil.block(f,
                                                      CodeUtil.methodCallStatement(f,
                                                                                   f.fieldReference(f._this(),
                                                                                                    context.fields.get(relation).getName()),
                                                                                   "add",
                                                                                   f.variableReference("value")),
                                                      f._return(f._this())
                                );
                            }
                        }
                );
    }

    private MethodBean createSetter(final Context context, final Relation<?> relation) {
        return createSelfReturningMethod(context.classBean)
                .setName(getSetterName(relation))
                .addParameter(createSetterParameter(context.types, relation))
                .put(
                        ClassPrinter.METHOD_BODY,
                        new PieceOfCode() {
                            @NotNull
                            @Override
                            public <E> E create(@NotNull CodeFactory<E> f) {
                                return CodeUtil.block(f,
                                                      f.assignment(
                                                              f.fieldReference(f._this(), context.getField(relation).getName()),
                                                              f.variableReference("value")),
                                                      f._return(f._this())
                                );
                            }
                        }
                );
    }

    private ParameterBean createSetterParameter(TypeTransformer types, Relation<?> relation) {
        return new ParameterBean().addAnnotation(NOT_NULL).setType(types.targetToType(relation.getTarget(), Multiplicity.ONE)).setName("values");
    }

    private ParameterBean createAllAdderParameter(TypeTransformer types, Relation<?> relation) {
        assert relation.getMultiplicity().isCollection();
        return new ParameterBean().addAnnotation(NOT_NULL).setType(
                types.targetToType(relation.getTarget(), Multiplicity.COLLECTION, TypeTransformer.Variance.OUT)).setName("value");
    }

    private static String getAllElementAdderName(Relation<?> relation) {
        return "addAllTo" + relation.getName();
    }

    private static String getSingleElementAdderName(Relation<?> relation) {
        return "addTo" + relation.getName();
    }

    private static MethodBean createSelfReturningMethod(ClassBean classBean) {
        return new MethodBean()
                        .addAnnotation(NOT_NULL)
                        .setVisibility(Visibility.PUBLIC)
                        .setReturnType(simpleType(classBean));
    }

    private void createGetters(final Context context) {
        for (final Relation<?> relation : context.entity.getRelations()) {
            context.classBean.getMethods().add(new MethodBean()
                                          .addAnnotation(OVERRIDE)
                                          .setVisibility(Visibility.PUBLIC)
                                          .setReturnType(context.types.relationToType(relation))
                                          .setName(getGetterName(relation))
                                          .put(
                                              ClassPrinter.METHOD_BODY,
                                              new PieceOfCode() {
                                                  @NotNull
                                                  @Override
                                                  public <E> E create(@NotNull CodeFactory<E> f) {
                                                      return f._return(
                                                          f.fieldReference(f._this(), context.getField(relation).getName())
                                                      );
                                                  }
                                              }
                                          )
            );
        }
    }

    private void createFields(Context context) {
        for (Relation<?> relation : context.entity.getRelations()) {
            FieldBean field = new FieldBean()
                    .setVisibility(Visibility.PRIVATE)
                    .setType(context.types.relationToType(relation))
                    .setName(getFieldName(relation));
            context.fields.put(relation, field);
            context.classBean.getFields().add(field);
        }
    }

    @Override
    public String getEntityRepresentationName(@NotNull Entity entity) {
        return entity.getName() + "Bean";
    }

    private static class Context {
        private final Entity entity;
        private final ClassBean classBean;
        private final EntityRepresentationContext<ClassBean> context;
        private final TypeTransformer types;
        private final Map<Relation<?>, FieldModel> fields = Maps.newHashMap();

        private Context(EntityRepresentationContext<ClassBean> context, Entity entity, ClassBean classBean) {
            this.context = context;
            this.types = types(context);
            this.entity = entity;
            this.classBean = classBean;
        }

        public FieldModel getField(Relation<?> relation) {
            return fields.get(relation);
        }
    }

}
