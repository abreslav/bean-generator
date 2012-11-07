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
import org.jetbrains.jet.buildergen.dataholder.DataHolderKey;
import org.jetbrains.jet.buildergen.dataholder.DataHolderKeyImpl;
import org.jetbrains.jet.buildergen.entities.Entity;
import org.jetbrains.jet.buildergen.entities.EntityUtil;
import org.jetbrains.jet.buildergen.entities.Relation;
import org.jetbrains.jet.buildergen.java.ClassPrinter;
import org.jetbrains.jet.buildergen.java.code.BinaryOperation;
import org.jetbrains.jet.buildergen.java.code.CodeFactory;
import org.jetbrains.jet.buildergen.java.code.PieceOfCode;
import org.jetbrains.jet.buildergen.java.declarations.*;
import org.jetbrains.jet.buildergen.java.declarations.beans.*;
import org.jetbrains.jet.buildergen.java.types.TypeData;
import org.jetbrains.jet.buildergen.java.types.TypeUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.buildergen.EntityRepresentationGeneratorUtil.*;
import static org.jetbrains.jet.buildergen.java.code.CodeUtil.*;
import static org.jetbrains.jet.buildergen.java.types.TypeUtil._void;

/**
 * @author abreslav
 */
public class BuilderClassGenerator {

    public static final DataHolderKey<ParameterModel, Relation<?>> RELATION_FOR_PARAMETER = DataHolderKeyImpl.create("RELATION_FOR_PARAMETER");
    public static final DataHolderKey<MethodModel, Relation<?>> RELATION_FOR_METHOD = DataHolderKeyImpl.create("RELATION_FOR_METHOD");

    public static final String DELEGATE = "delegate";
    public static final String CLOSE = "close";
    public static final String OPEN = "open";
    public static final String ENTITY = "entity";

    @NotNull
    public static Collection<ClassModel> generate(
            @NotNull final BeanGenerationContext context,
            @NotNull final EntityRepresentationContextImpl trace,
            @NotNull String packageFqName
    ) {
        return EntityRepresentationGeneratorUtil.generateEntityRepresentations(
                context.getEntities(),
                ClassKind.CLASS,
                trace,
                packageFqName,
                new EntityBeanGenerationStrategy() {
                    @NotNull
                    @Override
                    public String getEntityRepresentationName(@NotNull Entity entity) {
                        return entity.getName() + "Builder";
                    }

                    @Override
                    public void generateEntity(@NotNull Entity entity, @NotNull ClassBean classBean) {
                        generateClassMembers(context, trace, classBean, entity);
                    }
                }
        );
    }


    protected static void generateClassMembers(
            BeanGenerationContext beanGenerationContext,
            EntityRepresentationContext<? extends ClassModel> context,
            ClassBean classBean,
            Entity entity
    ) {
        classBean.setAbstract(true);

        createDelegateFieldAndConstructors(classBean);

        List<Relation<?>> relationsToNonEntities = Lists.newArrayList();
        TypeTransformer types = new TypeTransformer(beanGenerationContext);
        for (Relation<?> relation : EntityUtil.getAllRelations(entity)) {
            Object target = relation.getTarget();
            if (target instanceof Entity) {
                Entity targetEntity = (Entity) target;
                if (EntityUtil.isReference(relation)) {
                    classBean.getMethods().add(createSetterMethod(relation, targetEntity));
                }
                else {
                    classBean.getMethods().add(createRelationBuilderMethod(context, relation, targetEntity));
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
        TypeData delegateType = TypeUtil.type(classBean);
        classBean.getFields().add(new FieldBean()
                                  .setVisibility(Visibility.PRIVATE)
                                  .setFinal(true)
                                  .setType(delegateType)
                                  .setName("delegate")
        );

        classBean.getConstructors().add(
                constructorDeclaration(classBean)
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
                defaultConstructor()
        );
    }

    public static MethodBean defaultConstructor() {
        return JavaDeclarationUtil.publicConstructor()
                .put(ClassPrinter.METHOD_BODY,
                     new PieceOfCode() {
                         @Override
                         public <E> E create(@NotNull CodeFactory<E> f) {
                             return f.statement(methodCall(f, null, "this", f._null()));
                         }
                     });
    }

    public static MethodBean constructorDeclaration(ClassModel classBean) {
        return JavaDeclarationUtil.publicConstructor()
            .addParameter(new ParameterBean()
                                  .addAnnotation(CommonAnnotations.NULLABLE)
                                  .setType(TypeUtil.type(classBean))
                                  .setName(DELEGATE)
            );
    }

    private static MethodModel createSetterMethod(Relation<?> relation, Entity targetEntity) {
        final String name = getSetterName(relation);
        return new MethodBean()
                .setVisibility(Visibility.PUBLIC)
                .setReturnType(_void())
                .setName(name)
                .addParameter(JavaDeclarationUtil.notNullParameter(TypeUtil.getDataType(targetEntity), ENTITY))
                .put(ClassPrinter.METHOD_BODY,
                     new PieceOfCode() {
                         @NotNull
                         @Override
                         public <E> E create(@NotNull CodeFactory<E> f) {
                             return _if(f, delegateNullCheck(f),
                                        f.statement(delegateCall(f, name, Collections.singletonList(f.variableReference(ENTITY))))
                             );
                         }
                     })
                .put(RELATION_FOR_METHOD, relation);
    }

    private static MethodModel createRelationBuilderMethod(
            EntityRepresentationContext<? extends ClassModel> builders,
            Relation<?> relation,
            Entity targetEntity
    ) {
        final String name = getBuilderMethodName(relation);
        return new MethodBean()
                .addAnnotation(CommonAnnotations.NOT_NULL)
                .setVisibility(Visibility.PUBLIC)
                .setReturnType(TypeUtil.type(builders.getRepresentation(targetEntity)))
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
                     })
                .put(RELATION_FOR_METHOD, relation);
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
                                      .setType(types.relationToType(relation, TypeTransformer.Variance.OUT))
                                      .setName(getParameterName(relation))
                                      .put(RELATION_FOR_PARAMETER, relation)
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

    private static <E> E delegateCall(CodeFactory<E> f, String name) {
        return delegateCall(f, name, Collections.<E>emptyList());
    }

    private static <E> E delegateCall(CodeFactory<E> f, String name, List<E> arguments) {
        return f.methodCall(f.fieldReference(f._this(), DELEGATE), name, Collections.<TypeData>emptyList(), arguments);
    }

    private static <E> E delegateNullCheck(CodeFactory<E> f) {
        return f.binary(f.fieldReference(f._this(), DELEGATE), BinaryOperation.NEQ, f._null());
    }

    private static String getParameterName(Relation<?> relation) {
        return GeneratorUtil.variableNameByRelation(relation);
    }

    public static String getBuilderMethodName(Relation<?> relation) {
        return "add" + singularize(relation.getName());
    }

    private static String singularize(String name) {
        return name.endsWith("s") ? name.substring(0, name.length() - 1) : name;
    }

    private BuilderClassGenerator() {}
}
