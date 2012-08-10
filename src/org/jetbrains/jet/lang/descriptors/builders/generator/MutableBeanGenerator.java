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
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.ClassKind;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.MethodModel;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.Visibility;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.beans.ClassBean;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.beans.MethodBean;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.beans.ParameterBean;

/**
 * @author abreslav
 */
public class MutableBeanGenerator extends EntityRepresentationGenerator {

    @Override
    protected void generateSupertypes(EntityRepresentationContext<ClassBean> context, ClassBean classBean, Entity entity) {
        generateSupertypesFromSuperEntities(context, classBean, entity);
    }

    @Override
    protected void generateClassMembers(EntityRepresentationContext<ClassBean> context, ClassBean bean, Entity entity) {
        Context c = new Context(context, entity, bean);
        createGetters(c);
        createSettersAndAdders(c);
    }

    @Override
    public String getEntityRepresentationName(@NotNull Entity entity) {
        return entity.getName() + "Bean";
    }

    @NotNull
    @Override
    protected ClassKind getClassKind() {
        return ClassKind.INTERFACE;
    }

    private static void createSettersAndAdders(Context context) {
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

    private static MethodModel createAllElementAdder(final Context context, final Relation<?> relation) {
        assert relation.getMultiplicity().isCollection();

        return createSelfReturningMethod(context.classBean)
                .setName(getAllElementAdderName(relation))
                .addParameter(createAllAdderParameter(context.types, relation));
    }

    private static MethodModel createSingleElementAdder(final Context context, final Relation<?> relation) {
        assert relation.getMultiplicity().isCollection();

        return createSelfReturningMethod(context.classBean)
                .setName(getSingleElementAdderName(relation))
                .addParameter(createSetterParameter(context.types, relation));
    }

    private static MethodBean createSetter(final Context context, final Relation<?> relation) {
        return createSelfReturningMethod(context.classBean)
                .setName(getSetterName(relation))
                .addParameter(createSetterParameter(context.types, relation));
    }

    private static ParameterBean createSetterParameter(TypeTransformer types, Relation<?> relation) {
        return new ParameterBean().addAnnotation(NOT_NULL).setType(types.targetToType(relation.getTarget(), Multiplicity.ONE)).setName("values");
    }

    private static ParameterBean createAllAdderParameter(TypeTransformer types, Relation<?> relation) {
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
                        .setAbstract(true)
                        .setReturnType(simpleType(classBean));
    }

    private static void createGetters(final Context context) {
        for (final Relation<?> relation : context.entity.getRelations()) {
            context.classBean.getMethods().add(new MethodBean()
                                          .setVisibility(Visibility.PUBLIC)
                                          .setAbstract(true)
                                          .setReturnType(context.types.relationToVariantType(relation, TypeTransformer.Variance.OUT))
                                          .setName(getGetterName(relation))
            );
        }
    }

    private static class Context {
        private final Entity entity;
        private final ClassBean classBean;
        private final EntityRepresentationContext<ClassBean> context;
        private final TypeTransformer types;

        private Context(EntityRepresentationContext<ClassBean> context, Entity entity, ClassBean classBean) {
            this.context = context;
            this.types = types(context);
            this.entity = entity;
            this.classBean = classBean;
        }
    }
}
