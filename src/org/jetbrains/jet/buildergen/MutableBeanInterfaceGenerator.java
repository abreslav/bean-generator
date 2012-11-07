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
import org.jetbrains.jet.buildergen.entities.Entity;
import org.jetbrains.jet.buildergen.entities.Multiplicity;
import org.jetbrains.jet.buildergen.entities.Relation;
import org.jetbrains.jet.buildergen.java.declarations.*;
import org.jetbrains.jet.buildergen.java.declarations.beans.ClassBean;
import org.jetbrains.jet.buildergen.dataholder.DataHolderKeyImpl;
import org.jetbrains.jet.buildergen.java.declarations.beans.MethodBean;
import org.jetbrains.jet.buildergen.java.declarations.beans.ParameterBean;
import org.jetbrains.jet.buildergen.java.types.TypeData;
import org.jetbrains.jet.buildergen.java.types.TypeUtil;

import java.util.Collection;

import static org.jetbrains.jet.buildergen.EntityRepresentationGeneratorUtil.*;

/**
 * @author abreslav
 */
public class MutableBeanInterfaceGenerator {

    public static DataHolderKey<MethodModel, Relation<?>> GETTER = DataHolderKeyImpl.create("GETTER");
    public static DataHolderKey<MethodModel, Relation<?>> SETTER = DataHolderKeyImpl.create("SETTER");
    public static DataHolderKey<MethodModel, Relation<?>> ADDER = DataHolderKeyImpl.create("ADDER");
    public static DataHolderKey<MethodModel, Relation<?>> ALL_ADDER = DataHolderKeyImpl.create("ALL_ADDER");

    @NotNull
    public static Collection<ClassModel> generate(
            @NotNull final BeanGenerationContextImpl context,
            @NotNull String packageFqName
    ) {
        return generateEntityRepresentations(
                context.getEntities(),
                ClassKind.INTERFACE,
                context.getBeanInterfaces(),
                packageFqName,
                new EntityBeanGenerationStrategy() {
                    @NotNull
                    @Override
                    public String getEntityRepresentationName(@NotNull Entity entity) {
                        return entity.getName() + "Bean";
                    }

                    @Override
                    public void generateEntity(@NotNull Entity entity, @NotNull ClassBean classBean) {
                        classBean.getSuperInterfaces().addAll(
                                entitiesToTypes(context.getBeanInterfaces(), entity.getSuperEntities())
                        );

                        EntityContext c = new EntityContext(context, entity, classBean);
                        createGetters(c);
                        createSettersAndAdders(c);
                    }
                }
        );
    }

    private static void createGetters(EntityContext context) {
        for (Relation<?> relation : context.entity.getRelations()) {
            context.classBean.getMethods().add(new MethodBean()
                                                       .setVisibility(Visibility.PUBLIC)
                                                       .setAbstract(true)
                                                       .setReturnType(
                                                               context.types.relationToType(relation, TypeTransformer.Variance.OUT))
                                                       .setName(getGetterName(relation))
                                                       .put(GETTER, relation)
            );
        }
    }

    private static void createSettersAndAdders(EntityContext context) {
        for (Relation<?> relation : context.entity.getRelations()) {
            if (!relation.getMultiplicity().isCollection()) {
                context.classBean.getMethods().add(createSetter(context, relation));
            }
            else {
                context.classBean.getMethods().add(createSingleElementAdder(context, relation));
                context.classBean.getMethods().add(createAllElementAdder(context, relation));
            }
        }
    }

    private static MethodBean createSetter(EntityContext context, Relation<?> relation) {
        return createSelfReturningMethod(context.classBean)
                .setName(getSetterName(relation))
                .addParameter(createSetterParameter(context.types, relation))
                .put(SETTER, relation);
    }

    private static MethodModel createSingleElementAdder(EntityContext context, Relation<?> relation) {
        assert relation.getMultiplicity().isCollection();

        return createSelfReturningMethod(context.classBean)
                .setName(getSingleElementAdderName(relation))
                .addParameter(createSetterParameter(context.types, relation))
                .put(ADDER, relation);
    }

    private static MethodModel createAllElementAdder(EntityContext context, Relation<?> relation) {
        assert relation.getMultiplicity().isCollection();

        return createSelfReturningMethod(context.classBean)
                .setName(getAllElementAdderName(relation))
                .addParameter(createAllAdderParameter(context.types, relation))
                .put(ALL_ADDER, relation);
    }

    private static ParameterBean createSetterParameter(TypeTransformer types, Relation<?> relation) {
        TypeData setterParameterType = types.relationToType(relation, Multiplicity.ONE);
        return new ParameterBean()
                //.addAnnotation(NOT_NULL)
                .setType(setterParameterType)
                .setName("value");
    }

    private static ParameterBean createAllAdderParameter(TypeTransformer types, Relation<?> relation) {
        assert relation.getMultiplicity().isCollection();
        TypeData type = types.relationToType(relation, Multiplicity.COLLECTION, TypeTransformer.Variance.OUT);
        return new ParameterBean().addAnnotation(CommonAnnotations.NOT_NULL).setType(type).setName("values");
    }

    public static String getAllElementAdderName(Relation<?> relation) {
        return "addAllTo" + relation.getName();
    }

    public static String getSingleElementAdderName(Relation<?> relation) {
        return "addTo" + relation.getName();
    }

    private static MethodBean createSelfReturningMethod(ClassBean classBean) {
        return new MethodBean()
                        .addAnnotation(CommonAnnotations.NOT_NULL)
                        .setVisibility(Visibility.PUBLIC)
                        .setAbstract(true)
                        .setReturnType(TypeUtil.type(classBean));
    }

    private static class EntityContext {
        private final Entity entity;
        private final ClassBean classBean;
        private final TypeTransformer types;

        private EntityContext(BeanGenerationContext beanGenerationContext, Entity entity, ClassBean classBean) {
            this.types = new TypeTransformer(beanGenerationContext);
            this.entity = entity;
            this.classBean = classBean;
        }
    }
}
