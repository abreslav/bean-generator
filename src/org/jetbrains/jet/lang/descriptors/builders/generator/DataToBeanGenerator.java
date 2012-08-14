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
public class DataToBeanGenerator {
    private static final String RESULT = "result";
    private static final String ORIGINAL = "original";
    private static final String DATA_TO_BEAN = "toBean";
    private static final String LOOP_INDEX = "item";

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
        utilClass.getMethods().addAll(generateDeepCopyMethods(interfaces, implementations));
        return utilClass;
    }

    private static Collection<MethodBean> generateDeepCopyMethods(
            final EntityRepresentationContext<ClassBean> interfaces,
            final EntityRepresentationContext<ClassBean> implementations
    ) {
        Collection<MethodBean> result = Lists.newArrayList();
        for (final Entity entity : interfaces.getEntities()) {
            ClassBean beanInterface = interfaces.getRepresentation(entity);
            final TypeData beanInterfaceType = TypeUtil.simpleType(beanInterface);
            TypeData dataType = getDataType(entity);
            result.add(new MethodBean()
                               .setVisibility(Visibility.PUBLIC)
                               .setStatic(true)
                               .addAnnotation(EntityRepresentationGenerator.NOT_NULL)
                               .setReturnType(beanInterfaceType)
                               .setName(DATA_TO_BEAN)
                               .addParameter(
                                       new ParameterBean()
                                               .addAnnotation(EntityRepresentationGenerator.NOT_NULL)
                                               .setType(dataType)
                                               .setName(ORIGINAL)
                               )
                               .put(
                                       ClassPrinter.METHOD_BODY,
                                       new PieceOfCode() {
                                           @NotNull
                                           @Override
                                           public <E> E create(@NotNull CodeFactory<E> f) {
                                               List<E> statements = Lists.newArrayList();
                                               statements.add(resultVariableDeclarationStatement(f,
                                                                                                 beanInterfaceType, implementations
                                                       .getRepresentation(entity)));
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
                                                       statement = deepCopyCollectionStatement(f, relation, interfaces);
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

    private static TypeData getDataType(Entity entity) {
        EntityBuilder.ClassName dataClassName = entity.getData(EntityBuilder.DATA_CLASS);
        return TypeUtil.simpleType(dataClassName.getPackageFqName(), dataClassName.getClassName());
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

    private static <E> E deepCopyStatement(CodeFactory<E> f, Relation<?> relation) {
        String getterName = getGetterName(relation);
        String setterName = getSetterName(relation);
        return methodCallStatement(f, f.variableReference(RESULT),
                                   setterName,
                                   methodCall(f, null, DATA_TO_BEAN,
                                              methodCall(f, f.variableReference(ORIGINAL), getterName))
        );
    }

    private static <E> E deepCopyCollectionStatement(CodeFactory<E> f, Relation<?> relation, EntityRepresentationContext<ClassBean> context) {
        TypeData elementType;
        Object target = relation.getTarget();
        if (target instanceof Entity) {
            Entity targetEntity = (Entity) target;
            elementType = getDataType(targetEntity);
        }
        else {
            elementType = new TypeTransformer(context).targetToType(target, Multiplicity.ONE);
        }
        String getterName = getGetterName(relation);
        return _for(f, elementType, LOOP_INDEX, methodCall(f, f.variableReference(ORIGINAL), getterName),
                    copyCollectionElementStatement(f, relation)
        );
    }

    private static <E> E copyCollectionElementStatement(CodeFactory<E> f, Relation<?> relation) {
        String oneElementAdderName = MutableBeanInterfaceGenerator.getSingleElementAdderName(relation);
        if (relation.getTarget() instanceof Entity) {
            return methodCallStatement(f, f.variableReference(RESULT), oneElementAdderName,
                                            methodCall(f, null, DATA_TO_BEAN, f.variableReference(LOOP_INDEX)));
        }
        else {
            return methodCallStatement(f, f.variableReference(RESULT), oneElementAdderName,
                                            f.variableReference(LOOP_INDEX));
        }
    }
}
