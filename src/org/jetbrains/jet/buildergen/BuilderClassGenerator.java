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
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.buildergen.dataholder.DataHolderKey;
import org.jetbrains.jet.buildergen.dataholder.DataHolderKeyImpl;
import org.jetbrains.jet.buildergen.entities.Entity;
import org.jetbrains.jet.buildergen.entities.EntityUtil;
import org.jetbrains.jet.buildergen.entities.Multiplicity;
import org.jetbrains.jet.buildergen.entities.Relation;
import org.jetbrains.jet.buildergen.java.ClassPrinter;
import org.jetbrains.jet.buildergen.java.code.BinaryOperation;
import org.jetbrains.jet.buildergen.java.code.CodeFactory;
import org.jetbrains.jet.buildergen.java.code.PieceOfCode;
import org.jetbrains.jet.buildergen.java.declarations.ClassKind;
import org.jetbrains.jet.buildergen.java.declarations.MethodModel;
import org.jetbrains.jet.buildergen.java.declarations.ParameterModel;
import org.jetbrains.jet.buildergen.java.declarations.Visibility;
import org.jetbrains.jet.buildergen.java.declarations.beans.*;
import org.jetbrains.jet.buildergen.java.types.TypeData;
import org.jetbrains.jet.buildergen.java.types.TypeUtil;

import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.buildergen.java.code.CodeUtil.*;
import static org.jetbrains.jet.buildergen.java.types.TypeUtil._void;

/**
 * @author abreslav
 */
public class BuilderClassGenerator extends EntityRepresentationGenerator {

    public static final DataHolderKey<ParameterModel, Relation<?>> RELATION = DataHolderKeyImpl.create("RELATION");

    public static final String DELEGATE = "delegate";
    public static final String CLOSE = "close";
    public static final String OPEN = "open";

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
                if (relation.getData(EntityBuilder.REFERENCE) == Boolean.TRUE) {
                    classBean.getMethods().add(createSetterMethod(types, relation, targetEntity));
                }
                else {
                    classBean.getMethods().add(createRelationBuilderMethod(types, relation, targetEntity));
                }
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
                createConstructor()
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
                createConstructor()
                        .put(ClassPrinter.METHOD_BODY,
                             new PieceOfCode() {
                                 @Override
                                 public <E> E create(@NotNull CodeFactory<E> f) {
                                     return f.statement(methodCall(f, null, "this", f._null()));
                                 }
                             })
        );
    }

    private static MethodModel createSetterMethod(TypeTransformer types, Relation<?> relation, Entity targetEntity) {
        final String name = getSetterName(relation);
        final String parameterName = "entity";
        return new MethodBean()
                .addAnnotation(NOT_NULL)
                .setVisibility(Visibility.PUBLIC)
                .setReturnType(_void())
                .setName(name)
                .addParameter(JavaDeclarationUtil.notNullParameter(TypeUtil.getDataType(targetEntity), parameterName))
                .put(ClassPrinter.METHOD_BODY,
                     new PieceOfCode() {
                         @NotNull
                         @Override
                         public <E> E create(@NotNull CodeFactory<E> f) {
                             return _if(f, delegateNullCheck(f),
                                        f.statement(delegateCall(f, name, Collections.singletonList(f.variableReference(parameterName))))
                             );
                         }
                     });
    }

    private static MethodModel createRelationBuilderMethod(
            TypeTransformer types,
            Relation<?> relation,
            Entity targetEntity
    ) {
        final String name = getBuilderMethodName(relation);
        return new MethodBean()
                .addAnnotation(NOT_NULL)
                .setVisibility(Visibility.PUBLIC)
                .setReturnType(types.targetToType(targetEntity, Multiplicity.ONE))
                .setName(name)
                .put(ClassPrinter.METHOD_BODY,
                     new PieceOfCode() {
                         @NotNull
                         @Override
                         public <E> E create(@NotNull CodeFactory<E> f) {
                             return block(f,
                                          _if(f, delegateNullCheck(f),
                                              f._return(delegateCall(f, name))
                                          ),
                                          f._throw(constructorCall(f, "java.lang", "IllegalStateException", f.string("No delegate")))
                             );
                         }
                     });
    }

    private static MethodModel createClosingBuilderMethod() {
        return new MethodBean()
                .setVisibility(Visibility.PUBLIC)
                .setReturnType(TypeUtil._void())
                .setName(CLOSE)
                .put(ClassPrinter.METHOD_BODY,
                     new PieceOfCode() {
                         @NotNull
                         @Override
                         public <E> E create(@NotNull CodeFactory<E> f) {
                             return _if(f, delegateNullCheck(f),
                                        f.statement(delegateCall(f, CLOSE))
                             );
                         }
                     });
    }

    private static MethodModel createOpeningBuilderMethod(TypeTransformer types, final List<Relation<?>> relations) {
        MethodBean open = new MethodBean()
                .setVisibility(Visibility.PUBLIC)
                .setReturnType(TypeUtil._void())
                .setName(OPEN);
        for (Relation<?> relation : relations) {
            open.addParameter(new ParameterBean()
                                      .setType(types.targetToType(relation.getTarget(), relation.getMultiplicity(), TypeTransformer.Variance.OUT))
                                      .setName(getParameterName(relation))
                                      .put(RELATION, relation)
            );
        }
        open.put(ClassPrinter.METHOD_BODY,
                 new PieceOfCode() {
                     @NotNull
                     @Override
                     public <E> E create(@NotNull CodeFactory<E> f) {
                         List<E> arguments = Lists.newArrayList();
                         for (Relation<?> relation : relations) {
                             arguments.add(f.variableReference(getParameterName(relation)));
                         }
                         return _if(f, delegateNullCheck(f),
                                    f.statement(delegateCall(f, OPEN, arguments))
                         );
                     }
                 });
        return open;
    }

    private static MethodBean createConstructor() {
        return new MethodBean()
                .setVisibility(Visibility.PUBLIC)
                .setName("<init>");
    }

    private static <E> E delegateCall(CodeFactory<E> f, String name) {
        return delegateCall(f, name, Collections.<E>emptyList());
    }

    private static <E> E delegateCall(CodeFactory<E> f, String name, List<E> arguments) {
        return f.methodCall(f.fieldReference(f._this(), DELEGATE), name, arguments);
    }

    private static <E> E delegateNullCheck(CodeFactory<E> f) {
        return f.binary(f.fieldReference(f._this(), DELEGATE), BinaryOperation.NEQ, f._null());
    }

    private static String getParameterName(Relation<?> relation) {
        return StringUtil.decapitalize(relation.getName());
    }

    public static String getBuilderMethodName(Relation<?> relation) {
        return "add" + singularize(relation.getName());
    }

    private static String singularize(String name) {
        return name.endsWith("s") ? name.substring(0, name.length() - 1) : name;
    }
}
