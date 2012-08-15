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

import com.google.common.collect.Lists;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.ClassPrinter;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.code.CodeFactory;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.code.PieceOfCode;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.ClassKind;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.MethodModel;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.Visibility;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.beans.ClassBean;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.beans.FieldBean;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.beans.MethodBean;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.beans.ParameterBean;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.types.TypeData;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.types.TypeUtil;

import java.util.List;

import static org.jetbrains.jet.lang.descriptors.builders.generator.java.code.CodeUtil.methodCall;

/**
 * @author abreslav
 */
public class BuilderClassGenerator extends EntityRepresentationGenerator {

    private static final String DELEGATE = "delegate";

    @NotNull
    @Override
    protected ClassKind getClassKind() {
        return ClassKind.CLASS;
    }

    @Override
    public String getEntityRepresentationName(@NotNull Entity entity) {
        return entity.getName() + "Builder";
    }

    @Override
    protected void generateSupertypes(EntityRepresentationContext<ClassBean> context, ClassBean classBean, Entity entity) {
        // No supertypes
    }

    @Override
    protected void generateClassMembers(EntityRepresentationContext<ClassBean> context, ClassBean classBean, Entity entity) {
        classBean.setAbstract(true);

        createDelegateFieldAndConstructors(classBean);

        List<Relation<?>> relationsToNonEntities = Lists.newArrayList();
        TypeTransformer types = new TypeTransformer(context);
        for (Relation<?> relation : EntityUtil.getAllRelations(entity)) {
            Object target = relation.getTarget();
            if (target instanceof Entity) {
                Entity targetEntity = (Entity) target;
                classBean.getMethods().add(createRelationBuilderMethod(types, relation, targetEntity));
            }
            else {
                relationsToNonEntities.add(relation);
            }
        }
        classBean.getMethods().add(0, createOpeningBuilderMethod(types, relationsToNonEntities));
        classBean.getMethods().add(createClosingBuilderMethod());
    }

    private static void createDelegateFieldAndConstructors(ClassBean classBean) {
        TypeData delegateType = TypeUtil.simpleType(classBean);
        classBean.getFields().add(new FieldBean()
                                  .setVisibility(Visibility.PRIVATE)
                                  .setFinal(true)
                                  .setType(delegateType)
                                  .setName("delegate")
        );

        classBean.getConstructors().add(
                new MethodBean()
                    .setVisibility(Visibility.PUBLIC)
                    .setName("<init>")
                    .addParameter(new ParameterBean()
                                          .addAnnotation(NULLABLE)
                                          .setType(delegateType)
                                          .setName(DELEGATE)
                    )
                    .put(ClassPrinter.METHOD_BODY,
                         new PieceOfCode() {
                             @NotNull
                             @Override
                             public <E> E create(@NotNull CodeFactory<E> f) {
                                 return f.assignment(
                                         f.fieldReference(f._this(), DELEGATE),
                                         f.variableReference(DELEGATE));
                             }
                         })
        );

        classBean.getConstructors().add(
                new MethodBean()
                        .setVisibility(Visibility.PUBLIC)
                        .setName("<init>")
                        .put(ClassPrinter.METHOD_BODY,
                             new PieceOfCode() {
                                 @NotNull
                                 @Override
                                 public <E> E create(@NotNull CodeFactory<E> f) {
                                     return f.statement(methodCall(f, null, "this", f._null()));
                                 }
                             })
        );
    }

    private static MethodModel createRelationBuilderMethod(
            TypeTransformer types,
            Relation<?> relation,
            Entity targetEntity
    ) {
        return new MethodBean()
                .addAnnotation(NOT_NULL)
                .setVisibility(Visibility.PUBLIC)
                .setAbstract(true)
                .setReturnType(types.targetToType(targetEntity, Multiplicity.ONE))
                .setName(getBuilderMethodName(relation));
    }

    private static MethodModel createClosingBuilderMethod() {
        return new MethodBean()
                .setVisibility(Visibility.PUBLIC)
                .setAbstract(true)
                .setReturnType(TypeUtil._void())
                .setName("close");
    }

    private static MethodModel createOpeningBuilderMethod(TypeTransformer types, List<Relation<?>> relations) {
        MethodBean open = new MethodBean()
                .setVisibility(Visibility.PUBLIC)
                .setAbstract(true)
                .setReturnType(TypeUtil._void())
                .setName("open");
        for (Relation<?> relation : relations) {
            open.addParameter(new ParameterBean()
                                      .setType(types.relationToType(relation))
                                      .setName(getParameterName(relation))
            );
        }
        return open;
    }

    private static String getParameterName(Relation<?> relation) {
        return StringUtil.decapitalize(relation.getName());
    }

    private static String getBuilderMethodName(Relation<?> relation) {
        return "add" + singularize(relation.getName());
    }

    private static String singularize(String name) {
        return name.endsWith("s") ? name.substring(0, name.length() - 1) : name;
    }
}