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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.buildergen.entities.Entity;
import org.jetbrains.jet.buildergen.entities.EntityUtil;
import org.jetbrains.jet.buildergen.entities.Multiplicity;
import org.jetbrains.jet.buildergen.entities.Relation;
import org.jetbrains.jet.buildergen.java.ClassPrinter;
import org.jetbrains.jet.buildergen.java.code.CodeFactory;
import org.jetbrains.jet.buildergen.java.code.PieceOfCode;
import org.jetbrains.jet.buildergen.java.declarations.ClassKind;
import org.jetbrains.jet.buildergen.java.declarations.ClassModel;
import org.jetbrains.jet.buildergen.java.declarations.Visibility;
import org.jetbrains.jet.buildergen.java.declarations.beans.ClassBean;
import org.jetbrains.jet.buildergen.java.declarations.beans.MethodBean;
import org.jetbrains.jet.buildergen.java.declarations.beans.ParameterBean;
import org.jetbrains.jet.buildergen.java.types.TypeData;
import org.jetbrains.jet.buildergen.java.types.TypeUtil;

import java.util.Collection;
import java.util.List;

import static org.jetbrains.jet.buildergen.java.code.CodeUtil.*;

/**
 * @author abreslav
 */
@SuppressWarnings("unchecked")
public class BeanUtilGenerator {
    private static final String RESULT = "result";
    private static final String ORIGINAL = "original";
    private static final String SHALLOW_COPY = "shallowCopy";
    private static final String DEEP_COPY = "deepCopy";
    private static final String LOOP_INDEX = "item";

    public static ClassModel generate(
            @NotNull String packageName,
            @NotNull String className,
            @NotNull BeanGenerationContext context
    ) {
        ClassBean utilClass = new ClassBean()
                .setVisibility(Visibility.PUBLIC)
                .setKind(ClassKind.CLASS)
                .setPackageFqName(packageName)
                .setName(className);

        utilClass.getMethods().addAll(generateShallowCopyMethods(context));
        utilClass.getMethods().addAll(generateDeepCopyMethods(context));
        return utilClass;
    }

    @NotNull
    private static MethodBean createCopyMethod(TypeData beanInterfaceType, String methodName) {
        return new MethodBean()
                .setVisibility(Visibility.PUBLIC)
                .setStatic(true)
                //.addAnnotation(EntityRepresentationGeneratorUtil.NOT_NULL)
                .setReturnType(beanInterfaceType)
                .setName(methodName)
                .addParameter(
                        new ParameterBean()
                                //.addAnnotation(EntityRepresentationGeneratorUtil.NOT_NULL)
                                .setType(beanInterfaceType)
                                .setName(ORIGINAL)
                );
    }

    private static Collection<MethodBean> generateShallowCopyMethods(
            final BeanGenerationContext context
    ) {
        Collection<MethodBean> result = Lists.newArrayList();
        for (final Entity entity : context.getEntities()) {
            ClassModel beanInterface = context.getBeanInterfaces().getRepresentation(entity);
            final TypeData beanInterfaceType = TypeUtil.type(beanInterface);
            result.add(createCopyMethod(beanInterfaceType, SHALLOW_COPY)
                    .put(
                            ClassPrinter.METHOD_BODY,
                            new PieceOfCode() {
                                @NotNull
                                @Override
                                public <E> E create(@NotNull CodeFactory<E> f) {
                                    List<E> statements = Lists.newArrayList();

                                    statements.add(GeneratorUtil.ifVariableIsNullReturnNullStatement(f, ORIGINAL));
                                    statements.add(resultVariableDeclarationStatement(f,
                                                                                      beanInterfaceType,
                                                                                      context.getBeanImplementations().getRepresentation(entity)));
                                    for (Relation<?> relation : EntityUtil.getAllRelations(entity)) {
                                        E statement;
                                        if (!relation.getMultiplicity().isCollection()) {
                                            statement = directCopyStatement(f, relation);
                                        }
                                        else {
                                            statement = shallowCopyCollectionStatement(f, relation);
                                        }
                                        statements.add(statement);
                                    }
                                    statements.add(f._return(f.variableReference(RESULT)));
                                    return f.block(statements);
                                }
                            }
                    )
            );
        }
        return result;
    }

    private static Collection<MethodBean> generateDeepCopyMethods(
            final BeanGenerationContext context
    ) {
        Collection<MethodBean> result = Lists.newArrayList();
        for (final Entity entity : context.getEntities()) {
            ClassModel beanInterface = context.getBeanInterfaces().getRepresentation(entity);
            final TypeData beanInterfaceType = TypeUtil.type(beanInterface);
            result.add(createCopyMethod(beanInterfaceType, DEEP_COPY)
                               .put(
                                       ClassPrinter.METHOD_BODY,
                                       new PieceOfCode() {
                                           @NotNull
                                           @Override
                                           public <E> E create(@NotNull CodeFactory<E> f) {
                                               List<E> statements = Lists.newArrayList();
                                               statements.add(GeneratorUtil.ifVariableIsNullReturnNullStatement(f, ORIGINAL));

                                               statements.add(resultVariableDeclarationStatement(f,
                                                                     beanInterfaceType, context.getBeanImplementations().getRepresentation(entity)));
                                               for (Relation<?> relation : EntityUtil.getAllRelations(entity)) {
                                                   E statement;
                                                   if (!relation.getMultiplicity().isCollection()) {
                                                       if (!EntityUtil.isReference(relation) && relation.getTarget() instanceof Entity) {
                                                           statement = deepCopyStatement(f, relation);
                                                       }
                                                       else {
                                                           statement = directCopyStatement(f, relation);
                                                       }
                                                   }
                                                   else {
                                                       statement = deepCopyCollectionStatement(f, relation, context);
                                                   }
                                                   statements.add(statement);
                                               }
                                               statements.add(f._return(f.variableReference(RESULT)));
                                               return f.block(statements);
                                           }
                                       }
                               )
            );
        }
        return result;
    }

    private static <E> E resultVariableDeclarationStatement(CodeFactory<E> f, TypeData type, ClassModel classBean) {
        return f.statement(
                f.variableDeclaration(type, RESULT, constructorCall(f, classBean))
        );
    }

    private static <E> E directCopyStatement(CodeFactory<E> f, Relation<?> relation) {
        String setterName = EntityRepresentationGeneratorUtil.getSetterName(relation);
        String getterName = EntityRepresentationGeneratorUtil.getGetterName(relation);
        return methodCallStatement(f, f.variableReference(RESULT), setterName,
                                   methodCall(f, f.variableReference(ORIGINAL), getterName));
    }

    private static <E> E deepCopyStatement(CodeFactory<E> f, Relation<?> relation) {
        String getterName = EntityRepresentationGeneratorUtil.getGetterName(relation);
        String setterName = EntityRepresentationGeneratorUtil.getSetterName(relation);
        return methodCallStatement(f, f.variableReference(RESULT),
                                   setterName,
                                   methodCall(f, null, DEEP_COPY,
                                              methodCall(f, f.variableReference(ORIGINAL), getterName))
        );
    }

    private static <E> E shallowCopyCollectionStatement(CodeFactory<E> f, Relation<?> relation) {
        String allAdderName = MutableBeanInterfaceGenerator.getAllElementAdderName(relation);
        String getterName = EntityRepresentationGeneratorUtil.getGetterName(relation);
        return methodCallStatement(f, f.variableReference(RESULT), allAdderName,
                                   methodCall(f, f.variableReference(ORIGINAL), getterName));
    }

    private static <E> E deepCopyCollectionStatement(CodeFactory<E> f, Relation<?> relation, BeanGenerationContext context) {
        TypeTransformer typeTransformer = new TypeTransformer(context);
        TypeData elementType = typeTransformer.relationToType(relation, Multiplicity.ONE);
        String getterName = EntityRepresentationGeneratorUtil.getGetterName(relation);
        return _for(f, elementType, LOOP_INDEX, methodCall(f, f.variableReference(ORIGINAL), getterName),
                    copyCollectionElementStatement(f, relation)
        );
    }

    private static <E> E copyCollectionElementStatement(CodeFactory<E> f, Relation<?> relation) {
        String oneElementAdderName = MutableBeanInterfaceGenerator.getSingleElementAdderName(relation);
        if (relation.getTarget() instanceof Entity && !EntityUtil.isReference(relation)) {
            return methodCallStatement(f, f.variableReference(RESULT), oneElementAdderName,
                                            methodCall(f, null, DEEP_COPY, f.variableReference(LOOP_INDEX)));
        }
        else {
            return methodCallStatement(f, f.variableReference(RESULT), oneElementAdderName,
                                            f.variableReference(LOOP_INDEX));
        }
    }
}
