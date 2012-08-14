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
import org.jetbrains.jet.lang.descriptors.builders.generator.java.code.CodeUtil;
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
        return utilClass;
    }

    private static Collection<MethodBean> generateShallowCopyMethods(
            EntityRepresentationContext<ClassBean> interfaces,
            final EntityRepresentationContext<ClassBean> implementations
    ) {
        Collection<MethodBean> result = Lists.newArrayList();
        for (final Entity entity : interfaces.getEntities()) {
            ClassBean beanInterface = interfaces.getRepresentation(entity);
            final TypeData beanInterfaceType = TypeUtil.simpleType(beanInterface);
            final String original = "original";
            result.add(new MethodBean()
                    .setVisibility(Visibility.PUBLIC)
                    .setStatic(true)
                    .addAnnotation(EntityRepresentationGenerator.NOT_NULL)
                    .setReturnType(beanInterfaceType)
                    .setName("shallowCopy")
                    .addParameter(
                        new ParameterBean()
                                .addAnnotation(EntityRepresentationGenerator.NOT_NULL)
                                .setType(beanInterfaceType)
                                .setName(original)
                    )
                    .put(
                            ClassPrinter.METHOD_BODY,
                            new PieceOfCode() {
                                @NotNull
                                @Override
                                public <E> E create(@NotNull CodeFactory<E> f) {
                                    String result = "result";
                                    List<E> statements = Lists.newArrayList();
                                    statements.add(f.variableDeclaration(beanInterfaceType, result,
                                                        CodeUtil.constructorCall(f, implementations.getRepresentation(entity))));
                                    for (Relation<?> relation : EntityUtil.getAllRelations(entity)) {
                                        if (!relation.getMultiplicity().isCollection()) {
                                            statements.add(
                                                    CodeUtil.methodCallStatement(f, f.variableReference(result),
                                                             MutableBeanInterfaceGenerator.getSetterName(relation),
                                                             CodeUtil.methodCall(f, f.variableReference(original), MutableBeanInterfaceGenerator.getGetterName(relation)))
                                            );
                                        }
                                        else {
                                            statements.add(
                                                    CodeUtil.methodCallStatement(f, f.variableReference(result),
                                                             MutableBeanInterfaceGenerator.getAllElementAdderName(relation),
                                                             CodeUtil.methodCall(f, f.variableReference(original), MutableBeanInterfaceGenerator.getGetterName(relation)))
                                            );
                                        }
                                    }
                                    statements.add(f._return(f.variableReference(result)));
                                    return f.block(statements);
                                }
                            }
                    )
            );
        }
        return result;
    }
}
