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

/**
 * @author abreslav
 */
public class MutableBeanInterfaceGenerator extends EntityRepresentationGenerator {

    public static DataHolderKey<MethodModel, Relation<?>> GETTER = DataHolderKeyImpl.create("GETTER");
    public static DataHolderKey<MethodModel, Relation<?>> SETTER = DataHolderKeyImpl.create("SETTER");
    public static DataHolderKey<MethodModel, Relation<?>> ADDER = DataHolderKeyImpl.create("ADDER");
    public static DataHolderKey<MethodModel, Relation<?>> ALL_ADDER = DataHolderKeyImpl.create("ALL_ADDER");

    @NotNull
    @Override
    protected ClassKind getClassKind() {
        return ClassKind.INTERFACE;
    }

    @Override
    public String getEntityRepresentationName(@NotNull Entity entity) {
        return entity.getName() + "Bean";
    }

    @Override
    protected void generateSupertypes(EntityRepresentationContext<ClassBean> context, ClassBean classBean, Entity entity) {
        generateSupertypesFromSuperEntities(context, classBean, entity);
    }

    @Override
    protected void generateClassMembers(EntityRepresentationContext<ClassBean> context, ClassBean classBean, Entity entity) {
        EntityContext c = new EntityContext(context, entity, classBean);

        createGetters(c);
        createSettersAndAdders(c);
    }

    private static void createGetters(EntityContext context) {
        for (Relation<?> relation : context.entity.getRelations()) {
            context.classBean.getMethods().add(new MethodBean()
                                                       .setVisibility(Visibility.PUBLIC)
                                                       .setAbstract(true)
                                                       .setReturnType(
                                                               context.types.relationToVariantType(relation, TypeTransformer.Variance.OUT))
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
        TypeData setterParameterType = types.targetToType(relation.getTarget(), Multiplicity.ONE);
        return new ParameterBean()
                //.addAnnotation(NOT_NULL)
                .setType(setterParameterType)
                .setName("value");
    }

    private static ParameterBean createAllAdderParameter(TypeTransformer types, Relation<?> relation) {
        assert relation.getMultiplicity().isCollection();
        TypeData type = types.targetToType(relation.getTarget(), Multiplicity.COLLECTION, TypeTransformer.Variance.OUT);
        return new ParameterBean().addAnnotation(NOT_NULL).setType(type).setName("values");
    }

    public static String getAllElementAdderName(Relation<?> relation) {
        return "addAllTo" + relation.getName();
    }

    public static String getSingleElementAdderName(Relation<?> relation) {
        return "addTo" + relation.getName();
    }

    private static MethodBean createSelfReturningMethod(ClassBean classBean) {
        return new MethodBean()
                        .addAnnotation(NOT_NULL)
                        .setVisibility(Visibility.PUBLIC)
                        .setAbstract(true)
                        .setReturnType(TypeUtil.simpleType(classBean));
    }

    private static class EntityContext {
        private final Entity entity;
        private final ClassBean classBean;
        private final EntityRepresentationContext<ClassBean> context;
        private final TypeTransformer types;

        private EntityContext(EntityRepresentationContext<ClassBean> context, Entity entity, ClassBean classBean) {
            this.context = context;
            this.types = types(context);
            this.entity = entity;
            this.classBean = classBean;
        }
    }
}
