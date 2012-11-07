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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.buildergen.dataholder.DataHolderKey;
import org.jetbrains.jet.buildergen.entities.Entity;
import org.jetbrains.jet.buildergen.entities.Multiplicity;
import org.jetbrains.jet.buildergen.entities.Relation;
import org.jetbrains.jet.buildergen.java.ClassPrinter;
import org.jetbrains.jet.buildergen.java.code.CodeFactory;
import org.jetbrains.jet.buildergen.java.code.CodeUtil;
import org.jetbrains.jet.buildergen.java.code.PieceOfCode;
import org.jetbrains.jet.buildergen.java.declarations.*;
import org.jetbrains.jet.buildergen.java.declarations.beans.ClassBean;
import org.jetbrains.jet.buildergen.java.declarations.beans.FieldBean;
import org.jetbrains.jet.buildergen.java.declarations.beans.JavaDeclarationUtil;
import org.jetbrains.jet.buildergen.java.declarations.beans.MethodBean;
import org.jetbrains.jet.buildergen.java.types.TypeData;
import org.jetbrains.jet.buildergen.java.types.TypeUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Queue;

import static org.jetbrains.jet.buildergen.EntityRepresentationGeneratorUtil.*;

/**
* @author abreslav
*/
public class MutableBeanImplementationGenerator {

    private static final ClassModel ARRAY_LIST = new ClassBean()
            .setPackageFqName("java.util")
            .setName("ArrayList");

    private static final ClassModel HASH_SET = new ClassBean()
            .setPackageFqName("java.util")
            .setName("HashSet");

    @NotNull
    public static Collection<ClassModel> generate(
            @NotNull final BeanGenerationContextImpl context,
            @NotNull String packageFqName
    ) {
        return generateEntityRepresentations(
                context.getEntities(),
                ClassKind.CLASS,
                context.getBeanImplementations(),
                packageFqName,
                new EntityBeanGenerationStrategy() {
                    @NotNull
                    @Override
                    public String getEntityRepresentationName(@NotNull Entity entity) {
                        return entity.getName() + "BeanImpl";
                    }

                    @Override
                    public void generateEntity(@NotNull Entity entity, @NotNull ClassBean classBean) {
                        classBean.getSuperInterfaces().add(TypeUtil.type(context.getBeanInterfaces().getRepresentation(entity)));

                        generateClassMembers(context, classBean, entity);
                    }
                }
        );
    }

    private static void generateClassMembers(BeanGenerationContext context, ClassBean classBean, Entity entity) {
        EntityRepresentationContext<ClassModel> mutableBeanInterfaces = context.getBeanInterfaces();
        ClassModel interfaceBean = mutableBeanInterfaces.getRepresentation(entity);
        EntityContext c = new EntityContext(mutableBeanInterfaces, entity, classBean);
        Map<String, MethodModel> methodsToImplement = Maps.newLinkedHashMap();
        collectAllMethodsToImplement(methodsToImplement, entity, mutableBeanInterfaces, IMPLS.keySet());
        for (MethodModel method : methodsToImplement.values()) {
            for (Map.Entry<DataHolderKey<? super MethodModel, Relation<?>>, MethodImplementation> entry : IMPLS.entrySet()) {
                Relation<?> relation = method.getData(entry.getKey());
                if (relation != null) {
                    if (c.fields.get(relation) == null) {
                        createField(c, relation);
                    }
                    MethodBean implementation = implement(method, entry.getValue().createBody(c, method));
                    if (entry.getKey() != (Object) MutableBeanInterfaceGenerator.GETTER) {
                        implementation.setReturnType(TypeUtil.type(classBean));
                    }
                    classBean.getMethods().add(implementation);
                }
            }
        }

    }

    private static void collectAllMethodsToImplement(
            Map<String, MethodModel> result,
            Entity start,
            EntityRepresentationContext<ClassModel> context,
            Collection<DataHolderKey<? super MethodModel, Relation<?>>> keys
    ) {
        Queue<Entity> queue = Lists.newLinkedList();
        queue.offer(start);
        while (!queue.isEmpty()) {
            Entity entity = queue.remove();
            ClassModel classBean = context.getRepresentation(entity);
            for (MethodModel method : classBean.getMethods()) {
                for (DataHolderKey<? super MethodModel, Relation<?>> key : keys) {
                    Relation<?> relation = method.getData(key);
                    String name = method.getName();
                    if (relation != null && !result.containsKey(name)) {
                        result.put(name, method);
                    }
                }
            }
            for (Entity superEntity : entity.getSuperEntities()) {
                queue.offer(superEntity);
            }
        }
    }

    private static void createField(final EntityContext context, final Relation<?> relation) {
        final FieldBean field = new FieldBean()
                .setVisibility(Visibility.PRIVATE)
                .setType(context.types.relationToType(relation))
                .setName(getFieldName(relation));
        if (relation.getMultiplicity().isCollection()) {
            field.setFinal(true);
            field.put(ClassPrinter.FIELD_INITIALIZER, new PieceOfCode() {
                @NotNull
                @Override
                public <E> E create(@NotNull CodeFactory<E> f) {
                    ClassModel collectionClass;
                    switch (relation.getMultiplicity()) {
                        case LIST:
                        case COLLECTION:
                            collectionClass = ARRAY_LIST;
                            break;
                        case SET:
                            collectionClass = HASH_SET;
                            break;
                        default:
                            throw new IllegalStateException("Unknown collection multiplicity: " + relation.getMultiplicity());
                    }
                    TypeData elementType = context.types.relationToType(relation, Multiplicity.ONE);
                    return f.constructorCall(collectionClass,
                                             Collections.singletonList(elementType),
                                             Collections.<E>singletonList(f.integer(0)));
                }
            });
        }
        context.fields.put(relation, field);
        context.classBean.getFields().add(field);
    }

    private static MethodBean implement(MethodModel method, PieceOfCode body) {
        return JavaDeclarationUtil.copy(method)
                .addAnnotation(CommonAnnotations.OVERRIDE)
                .setAbstract(false)
                .put(ClassPrinter.METHOD_BODY, body);
    }

    private interface MethodImplementation {
        PieceOfCode createBody(EntityContext context, MethodModel method);
    }

    private static MethodImplementation GETTER_IMPL = new MethodImplementation() {
        @Override
        public PieceOfCode createBody(final EntityContext context, final MethodModel method) {
            return new PieceOfCode() {
               @NotNull
               @Override
               public <E> E create(@NotNull CodeFactory<E> f) {
                   Relation<?> relation = method.getData(MutableBeanInterfaceGenerator.GETTER);
                   return f._return(
                           f.fieldReference(f._this(), context.getField(relation).getName())
                   );
               }
            };
        }
    };

    private static MethodImplementation SETTER_IMPL = new MethodImplementation() {
        @Override
        public PieceOfCode createBody(final EntityContext context, final MethodModel method) {
            return new PieceOfCode() {
                @NotNull
                @Override
                public <E> E create(@NotNull CodeFactory<E> f) {
                    Relation<?> relation = method.getData(MutableBeanInterfaceGenerator.SETTER);
                    return CodeUtil.block(f,
                                          f.assignment(
                                                  f.fieldReference(f._this(), context.getField(relation).getName()),
                                                  f.variableReference("value")),
                                          f._return(f._this())
                    );
                }
            };
        }
    };

    private static MethodImplementation ADDER_IMPL = new MethodImplementation() {
        @Override
        public PieceOfCode createBody(final EntityContext context, final MethodModel method) {
            return new PieceOfCode() {
                @NotNull
                @Override
                public <E> E create(@NotNull CodeFactory<E> f) {
                    Relation<?> relation = method.getData(MutableBeanInterfaceGenerator.ADDER);
                    return CodeUtil.block(f,
                                          CodeUtil.methodCallStatement(f,
                                                                       f.fieldReference(f._this(),
                                                                                        context.fields.get(relation).getName()),
                                                                       "add",
                                                                       f.variableReference("value")),
                                          f._return(f._this())
                    );
                }
            };
        }
    };

    private static MethodImplementation ALL_ADDER_IMPL = new MethodImplementation() {
        @Override
        public PieceOfCode createBody(final EntityContext context, final MethodModel method) {
            return new PieceOfCode() {
                @NotNull
                @Override
                public <E> E create(@NotNull CodeFactory<E> f) {
                    Relation<?> relation = method.getData(MutableBeanInterfaceGenerator.ALL_ADDER);
                    return CodeUtil.block(f,
                                          CodeUtil.methodCallStatement(f,
                                                                       f.fieldReference(f._this(),
                                                                                        context.getField(relation).getName()),
                                                                       "addAll",
                                                                       f.variableReference("values")),
                                          f._return(f._this())
                    );
                }
            };
        }
    };

    private static Map<DataHolderKey<? super MethodModel, Relation<?>>, MethodImplementation> IMPLS = ImmutableMap.<DataHolderKey<? super MethodModel, Relation<?>>, MethodImplementation>builder()
            .put(MutableBeanInterfaceGenerator.GETTER, GETTER_IMPL)
            .put(MutableBeanInterfaceGenerator.SETTER, SETTER_IMPL)
            .put(MutableBeanInterfaceGenerator.ADDER, ADDER_IMPL)
            .put(MutableBeanInterfaceGenerator.ALL_ADDER, ALL_ADDER_IMPL)
            .build();

    private static class EntityContext {
        private final Entity entity;
        private final ClassBean classBean;
        private final EntityRepresentationContext<? extends ClassModel> context;
        private final TypeTransformer types;
        private final Map<Relation<?>, FieldModel> fields = Maps.newHashMap();

        private EntityContext(EntityRepresentationContext<? extends ClassModel> context, Entity entity, ClassBean classBean) {
            this.context = context;
            this.types = new TypeTransformer(context);
            this.entity = entity;
            this.classBean = classBean;
        }

        public FieldModel getField(@NotNull Relation<?> relation) {
            FieldModel field = fields.get(relation);
            if (field == null) {
                throw new IllegalArgumentException("No field for relation " + relation.getName());
            }
            return field;
        }
    }

}
