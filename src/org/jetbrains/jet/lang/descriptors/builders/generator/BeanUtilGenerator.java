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
public class BeanUtilGenerator {
    public static ClassModel generate(
            String packageName,
            String className,
            EntityRepresentationContext<ClassBean> interfaces,
            EntityRepresentationContext<ClassBean> implementations
    ) {
        ClassBean utilClass = new ClassBean()
                .setVisibility(Visibility.PUBLIC)
                .setKind(ClassKind.CLASS)
                .setPackageFqName(packageName)
                .setName(className);
        utilClass.getMethods().addAll(generateShallowCopyMethods(interfaces, implementations));
        utilClass.getMethods().addAll(generateDeepCopyMethods(interfaces, implementations));
        return utilClass;
    }

    @NotNull
    private static MethodBean createCopyMethod(TypeData beanInterfaceType, String methodName) {
        return new MethodBean()
                .setVisibility(Visibility.PUBLIC)
                .setStatic(true)
                .addAnnotation(EntityRepresentationGenerator.NOT_NULL)
                .setReturnType(beanInterfaceType)
                .setName(methodName)
                .addParameter(
                        new ParameterBean()
                                .addAnnotation(EntityRepresentationGenerator.NOT_NULL)
                                .setType(beanInterfaceType)
                                .setName(ORIGINAL)
                );
    }

    private static Collection<MethodBean> generateShallowCopyMethods(
            EntityRepresentationContext<ClassBean> interfaces,
            final EntityRepresentationContext<ClassBean> implementations
    ) {
        Collection<MethodBean> result = Lists.newArrayList();
        for (final Entity entity : interfaces.getEntities()) {
            ClassBean beanInterface = interfaces.getRepresentation(entity);
            final TypeData beanInterfaceType = TypeUtil.simpleType(beanInterface);
            result.add(createCopyMethod(beanInterfaceType, SHALLOW_COPY)
                    .put(
                            ClassPrinter.METHOD_BODY,
                            new PieceOfCode() {
                                @NotNull
                                @Override
                                public <E> E create(@NotNull CodeFactory<E> f) {
                                    List<E> statements = Lists.newArrayList();
                                    ClassBean implementationClass = implementations.getRepresentation(entity);
                                    statements
                                            .add(f.variableDeclaration(beanInterfaceType, RESULT, constructorCall(f, implementationClass)));
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
            EntityRepresentationContext<ClassBean> interfaces,
            final EntityRepresentationContext<ClassBean> implementations
    ) {
        Collection<MethodBean> result = Lists.newArrayList();
        for (final Entity entity : interfaces.getEntities()) {
            ClassBean beanInterface = interfaces.getRepresentation(entity);
            final TypeData beanInterfaceType = TypeUtil.simpleType(beanInterface);
            result.add(createCopyMethod(beanInterfaceType, DEEP_COPY)
                               .put(
                                       ClassPrinter.METHOD_BODY,
                                       new PieceOfCode() {
                                           @NotNull
                                           @Override
                                           public <E> E create(@NotNull CodeFactory<E> f) {
                                               List<E> statements = Lists.newArrayList();
                                               statements.add(f.variableDeclaration(
                                                       beanInterfaceType,
                                                       RESULT,
                                                       constructorCall(f, implementations.getRepresentation(entity))));
                                               for (Relation<?> relation : EntityUtil.getAllRelations(entity)) {
                                                   E statement;
                                                   if (!relation.getMultiplicity().isCollection()) {
                                                       if (relation.getTarget() instanceof Entity) {
                                                           statement = deepCopyStatement(f, relation);
                                                       }
                                                       else {
                                                           statement = directCopyStatement(f, relation);
                                                       }
                                                   }
                                                   else {
                                                       statement = deepCopyCollectionStatement(f, relation);
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

    private static <E> E directCopyStatement(CodeFactory<E> f, Relation<?> relation) {
        String setterName = getSetterName(relation);
        String getterName = getGetterName(relation);
        return methodCallStatement(f, f.variableReference(RESULT), setterName,
                                   methodCall(f, f.variableReference(ORIGINAL), getterName));
    }

    private static <E> E deepCopyStatement(CodeFactory<E> f, Relation<?> relation) {
        String getterName = getGetterName(relation);
        String setterName = getSetterName(relation);
        return methodCallStatement(f, f.variableReference(RESULT),
                                   setterName,
                                   methodCall(f, null, DEEP_COPY,
                                              methodCall(f, f.variableReference(ORIGINAL), getterName))
        );
    }

    private static <E> E shallowCopyCollectionStatement(CodeFactory<E> f, Relation<?> relation) {
        String allAdderName = MutableBeanInterfaceGenerator.getAllElementAdderName(relation);
        String getterName = getGetterName(relation);
        return methodCallStatement(f, f.variableReference(RESULT), allAdderName,
                                   methodCall(f, f.variableReference(ORIGINAL), getterName));
    }

    private static <E> E deepCopyCollectionStatement(CodeFactory<E> f, Relation<?> relation) {
        return methodCallStatement(f, f.variableReference(RESULT),
                            MutableBeanInterfaceGenerator
                                    .getAllElementAdderName(relation),
                            methodCall(f, f.variableReference(
                                    ORIGINAL),
                                       getGetterName(
                                               relation)));
    }

    private static final String RESULT = "result";
    private static final String ORIGINAL = "original";
    private static final String SHALLOW_COPY = "shallowCopy";
    private static final String DEEP_COPY = "deepCopy";

}