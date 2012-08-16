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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.ClassPrinter;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.code.CodeFactory;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.code.PieceOfCode;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.ClassKind;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.ClassModel;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.Visibility;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.beans.ClassBean;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.beans.MethodBean;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.beans.ParameterBean;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.types.TypeData;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.types.TypeUtil;

import java.util.Collection;
import java.util.List;

import static org.jetbrains.jet.lang.descriptors.builders.generator.EntityRepresentationGenerator.getGetterName;
import static org.jetbrains.jet.lang.descriptors.builders.generator.EntityRepresentationGenerator.getSetterName;
import static org.jetbrains.jet.lang.descriptors.builders.generator.java.code.CodeUtil.*;

/**
 * @author abreslav
 */
@SuppressWarnings("unchecked")
public class BuilderUtilGenerator {
    private static final String RESULT = "result";
    private static final String ORIGINAL = "original";
    private static final String SHALLOW_COPY = "shallowCopy";
    private static final String DEEP_COPY = "deepCopy";
    private static final String LOOP_INDEX = "item";
    private static final String BUILDER = "builder";
    private static final String ENTITY = "entity";

    public static ClassModel generate(
            String packageName,
            String className,
            EntityRepresentationContext<ClassBean> beans,
            EntityRepresentationContext<ClassBean> builders
    ) {
        ClassBean utilClass = new ClassBean()
                .setVisibility(Visibility.PUBLIC)
                .setKind(ClassKind.CLASS)
                .setPackageFqName(packageName)
                .setName(className);
        utilClass.getMethods().addAll(generateBeanToBuilderMethods(beans, builders));
        return utilClass;
    }

    private static Collection<MethodBean> generateBeanToBuilderMethods(
            EntityRepresentationContext<ClassBean> beans,
            final EntityRepresentationContext<ClassBean> builders
    ) {
        Collection<MethodBean> result = Lists.newArrayList();
        for (final Entity entity : beans.getEntities()) {
            ClassBean beanInterface = beans.getRepresentation(entity);
            final TypeData beanInterfaceType = TypeUtil.simpleType(beanInterface);
            ClassBean builderClass = builders.getRepresentation(entity);
            TypeData builderType = TypeUtil.simpleType(builderClass);
            result.add(new MethodBean()
                               .setVisibility(Visibility.PUBLIC)
                               .setStatic(true)
                               .setReturnType(TypeUtil._void())
                               .setName(getBuilderMethodName(entity))
                               .addParameter(
                                       new ParameterBean()
                                               .addAnnotation(EntityRepresentationGenerator.NOT_NULL)
                                               .setType(beanInterfaceType)
                                               .setName(ENTITY)
                               )
                               .addParameter(
                                       new ParameterBean()
                                               .addAnnotation(EntityRepresentationGenerator.NOT_NULL)
                                               .setType(builderType)
                                               .setName(BUILDER)
                               )
                               .put(
                                       ClassPrinter.METHOD_BODY,
                                       new PieceOfCode() {
                                           @NotNull
                                           @Override
                                           public <E> E create(@NotNull CodeFactory<E> f) {
                                               List<E> statements = Lists.newArrayList();
                                               // builder.open(entity.getFoo(), entity.isBar())
                                               statements.add(f.statement(methodCall(f, f.variableReference(BUILDER), BuilderClassGenerator.OPEN)));

                                               for (Relation<?> relation : EntityUtil.getAllRelations(entity)) {
                                                   Object target = relation.getTarget();
                                                   if (target instanceof Entity) {
                                                       Entity subEntity = (Entity) target;
                                                       E statement;
                                                       if (!relation.getMultiplicity().isCollection()) {
                                                           // buildSubEntity(entity.getSubEntity(), builder.addSubEntity())
                                                           statement = buildEntityStatement(f, relation, subEntity);
                                                       }
                                                       else {
                                                           // for (SubEntity sub : entity.getSubEntities()) {
                                                           //     buildSubEntity(sub, builder.addSubEntity())
                                                           // }
                                                           statement = f.statement(f._null());
                                                       }
                                                       statements.add(statement);
                                                   }
                                               }

                                               // builder.close()
                                               statements.add(f.statement(methodCall(f, f.variableReference(BUILDER), BuilderClassGenerator.CLOSE)));

                                               return f.block(statements);
                                           }
                                       }
                               )
            );
        }
        return result;
    }

    private static String getBuilderMethodName(Entity entity) {
        return "build" + entity.getName();
    }

    private static <E> E buildEntityStatement(CodeFactory<E> f, Relation<?> relation, Entity target) {
        // buildEntity(entity.getTargetEntity(), builder.addTargetEntity())
        return f.statement(
                methodCall(f, null, getBuilderMethodName(target),
                           methodCall(f, f.variableReference(ENTITY), MutableBeanInterfaceGenerator.getGetterName(relation)),
                           methodCall(f, f.variableReference(BUILDER), BuilderClassGenerator.getBuilderMethodName(relation))
                )
        );
    }

    private static <E> E resultVariableDeclarationStatement(CodeFactory<E> f, TypeData type, ClassBean classBean) {
        return f.statement(
                f.variableDeclaration(type, RESULT, constructorCall(f, classBean))
        );
    }

    private static <E> E directCopyStatement(CodeFactory<E> f, Relation<?> relation) {
        String setterName = getSetterName(relation);
        String getterName = getGetterName(relation);
        return methodCallStatement(f, f.variableReference(RESULT), setterName,
                                   methodCall(f, f.variableReference(ORIGINAL), getterName));
    }

    private static <E> E shallowCopyCollectionStatement(CodeFactory<E> f, Relation<?> relation) {
        String allAdderName = MutableBeanInterfaceGenerator.getAllElementAdderName(relation);
        String getterName = getGetterName(relation);
        return methodCallStatement(f, f.variableReference(RESULT), allAdderName,
                                   methodCall(f, f.variableReference(ORIGINAL), getterName));
    }

    private static <E> E deepCopyCollectionStatement(CodeFactory<E> f, Relation<?> relation, EntityRepresentationContext<ClassBean> context) {
        TypeTransformer typeTransformer = new TypeTransformer(context);
        TypeData elementType = typeTransformer.targetToType(relation.getTarget(), Multiplicity.ONE);
        String getterName = getGetterName(relation);
        return _for(f, elementType, LOOP_INDEX, methodCall(f, f.variableReference(ORIGINAL), getterName),
                    copyCollectionElementStatement(f, relation)
        );
    }

    private static <E> E copyCollectionElementStatement(CodeFactory<E> f, Relation<?> relation) {
        String oneElementAdderName = MutableBeanInterfaceGenerator.getSingleElementAdderName(relation);
        if (relation.getTarget() instanceof Entity) {
            return methodCallStatement(f, f.variableReference(RESULT), oneElementAdderName,
                                            methodCall(f, null, DEEP_COPY, f.variableReference(LOOP_INDEX)));
        }
        else {
            return methodCallStatement(f, f.variableReference(RESULT), oneElementAdderName,
                                            f.variableReference(LOOP_INDEX));
        }
    }
}
