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
import org.jetbrains.jet.buildergen.entities.Entity;
import org.jetbrains.jet.buildergen.entities.Multiplicity;
import org.jetbrains.jet.buildergen.entities.Relation;
import org.jetbrains.jet.buildergen.java.ClassPrinter;
import org.jetbrains.jet.buildergen.java.code.BinaryOperation;
import org.jetbrains.jet.buildergen.java.code.CodeFactory;
import org.jetbrains.jet.buildergen.java.code.PieceOfCode;
import org.jetbrains.jet.buildergen.java.declarations.ClassKind;
import org.jetbrains.jet.buildergen.java.declarations.MethodModel;
import org.jetbrains.jet.buildergen.java.declarations.Visibility;
import org.jetbrains.jet.buildergen.java.declarations.beans.*;
import org.jetbrains.jet.buildergen.java.types.TypeUtil;

import java.util.Collections;
import java.util.List;

import static java.lang.Boolean.TRUE;
import static org.jetbrains.jet.buildergen.BuilderClassGenerator.*;
import static org.jetbrains.jet.buildergen.EntityBuilder.REFERENCE;
import static org.jetbrains.jet.buildergen.java.ClassPrinter.METHOD_BODY;
import static org.jetbrains.jet.buildergen.java.code.CodeUtil.*;
import static org.jetbrains.jet.buildergen.java.code.CodeUtil.constructorCall;
import static org.jetbrains.jet.buildergen.java.types.TypeUtil._void;
import static org.jetbrains.jet.buildergen.java.types.TypeUtil.simpleType;

/**
 * @author abreslav
 */
public class BeanBuilderClassGenerator extends EntityRepresentationGenerator {

    private static final String BEAN = "bean";

    private final EntityRepresentationContext<ClassBean> builders;
    private final EntityRepresentationContext<ClassBean> beanInterfaces;
    private final EntityRepresentationContext<ClassBean> beanImpls;

    public BeanBuilderClassGenerator(
            @NotNull EntityRepresentationContext<ClassBean> beanInterfaces,
            @NotNull EntityRepresentationContext<ClassBean> beanImpls,
            @NotNull EntityRepresentationContext<ClassBean> builders
    ) {
        this.builders = builders;
        this.beanInterfaces = beanInterfaces;
        this.beanImpls = beanImpls;
    }


    @NotNull
    @Override
    protected ClassKind getClassKind() {
        return ClassKind.CLASS;
    }

    @Override
    public String getEntityRepresentationName(@NotNull Entity entity) {
        return entity.getName() + "BeanBuilder";
    }

    @Override
    protected void generateSupertypes(EntityRepresentationContext<ClassBean> context, ClassBean classBean, Entity entity) {
        classBean.setSuperClass(TypeUtil.simpleType(builders.getRepresentation(entity)));
    }

    @Override
    protected void generateClassMembers(EntityRepresentationContext<ClassBean> context, ClassBean classBean, Entity entity) {

        ClassBean builderClass = builders.getRepresentation(entity);
        ClassBean beanInterface = beanInterfaces.getRepresentation(entity);
        ClassBean beanImpl = beanImpls.getRepresentation(entity);

        classBean.getFields().add(beanField(beanInterface, beanImpl));
        classBean.getConstructors().add(implementConstructor(builderClass));
        classBean.getMethods().add(beanGetter(beanInterface));

        for (MethodModel method : builderClass.getMethods()) {
            MethodBean impl = JavaDeclarationUtil.copy(method)
                                    .addAnnotation(OVERRIDE)
                                    .setAbstract(false);
            final Relation<?> relation = method.getData(BuilderClassGenerator.RELATION_FOR_METHOD);

            PieceOfCode body;
            if (relation == null) {
                 // either open() or close()
                 body = new PieceOfCode() {
                     @Override
                     public <E> E create(@NotNull CodeFactory<E> f) {
                         return  f._throw(constructorCall(f, "java.lang", "UnsupportedOperationException"));
                     }
                 };
            }
            else {
                body = new PieceOfCode() {
                     @Override
                     public <E> E create(@NotNull CodeFactory<E> f) {
                         // This must be an entity, everything else is taken care of in open()
                         Entity targetEntity = (Entity) relation.getTarget();
                         if (relation.getData(REFERENCE) == TRUE) {
                             // this.bean.setTargetEntity(entity);
                             return f._throw(constructorCall(f, "java.lang", "UnsupportedOperationException"));
                         }
                         else {
                             // TargetEntityBeanBuilder subBuilder = new TargetEntityBeanBuilder();
                             // this.bean.addTargetEntity(subBuilder.getBean());
                             // return subBuilder
                             return f._throw(constructorCall(f, "java.lang", "UnsupportedOperationException"));
                         }
                     }
                 };
            }

            impl.put(METHOD_BODY, body);
            classBean.getMethods().add(impl);
        }

        //List<Relation<?>> relationsToNonEntities = Lists.newArrayList();
        //TypeTransformer types = new TypeTransformer(context);
        //for (Relation<?> relation : EntityUtil.getAllRelations(entity)) {
        //    Object target = relation.getTarget();
        //    if (target instanceof Entity) {
        //        Entity targetEntity = (Entity) target;
        //        if (relation.getData(EntityBuilder.REFERENCE) == Boolean.TRUE) {
        //            classBean.getMethods().add(createSetterMethod(types, relation, targetEntity));
        //        }
        //        else {
        //            classBean.getMethods().add(createRelationBuilderMethod(types, relation, targetEntity));
        //        }
        //    }
        //    else {
        //        relationsToNonEntities.add(relation);
        //    }
        //}
        //classBean.getMethods().add(0, createOpeningBuilderMethod(types, relationsToNonEntities));
        //classBean.getMethods().add(createClosingBuilderMethod());
    }

    private static <E> E bean(CodeFactory<E> f) {
        return f.fieldReference(f._this(), BEAN);
    }

    private static MethodBean beanGetter(ClassBean beanInterface) {
        return new MethodBean()
            .addAnnotation(NOT_NULL)
            .setVisibility(Visibility.PUBLIC)
            .setReturnType(simpleType(beanInterface))
            .setName("buildResult")
            .put(METHOD_BODY,
                 new PieceOfCode() {
                     @Override
                     public <E> E create(@NotNull CodeFactory<E> f) {
                         return f._return(bean(f));
                     }
                 });
    }

    private static FieldBean beanField(ClassBean beanInterface, final ClassBean beanImpl) {
        return new FieldBean()
                .setVisibility(Visibility.PRIVATE)
                .setFinal(true)
                .setType(simpleType(beanInterface))
                .setName(BEAN)
                .put(ClassPrinter.FIELD_INITIALIZER,
                     new PieceOfCode() {
                         @Override
                         public <E> E create(@NotNull CodeFactory<E> f) {
                             return constructorCall(f, beanImpl);
                         }
                     });
    }

    private static MethodBean implementConstructor(ClassBean builderClass) {
        return BuilderClassGenerator.constructorDeclaration(builderClass)
                .put(METHOD_BODY,
                     new PieceOfCode() {
                         @Override
                         public <E> E create(@NotNull CodeFactory<E> f) {
                             return f.statement(methodCall(f, null, "super", f.variableReference(DELEGATE)));
                         }
                     });
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
                .put(METHOD_BODY,
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
                .put(METHOD_BODY,
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
                .put(METHOD_BODY,
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
            );
        }
        open.put(METHOD_BODY,
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
