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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.ClassPrinter;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.code.CodeFactory;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.code.CodeUtil;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.code.PieceOfCode;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.*;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.beans.ClassBean;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.beans.FieldBean;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.beans.JavaDeclarationUtil;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.beans.MethodBean;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.types.TypeData;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.types.TypeUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Queue;

import static org.jetbrains.jet.lang.descriptors.builders.generator.MutableBeanInterfaceGenerator.*;

/**
* @author abreslav
*/
public class MutableBeanImplementationGenerator extends EntityRepresentationGenerator {

    private static final ClassModel ARRAY_LIST = new ClassBean()
            .setPackageFqName("java.util")
            .setName("ArrayList");

    private static final ClassModel HASH_SET = new ClassBean()
            .setPackageFqName("java.util")
            .setName("HashSet");

    private final EntityRepresentationContext<ClassBean> mutableBeanInterfaces;

    public MutableBeanImplementationGenerator(EntityRepresentationContext<ClassBean> mutableBeanInterfaces) {
        this.mutableBeanInterfaces = mutableBeanInterfaces;
    }

    @NotNull
    @Override
    protected ClassKind getClassKind() {
        return ClassKind.CLASS;
    }

    @Override
    public String getEntityRepresentationName(@NotNull Entity entity) {
        return entity.getName() + "BeanImpl";
    }

    @Override
    protected void generateSupertypes(EntityRepresentationContext<ClassBean> context, ClassBean classBean, Entity entity) {
        classBean.getSuperInterfaces().add(TypeUtil.simpleType(mutableBeanInterfaces.getRepresentation(entity)));
    }

    @Override
    protected void generateClassMembers(EntityRepresentationContext<ClassBean> context, ClassBean classBean, Entity entity) {
        ClassBean interfaceBean = mutableBeanInterfaces.getRepresentation(entity);
        EntityContext c = new EntityContext(mutableBeanInterfaces, entity, classBean);
        Map<String, MethodModel> methodsToImplement = Maps.newLinkedHashMap();
        collectAllMethodsToImplement(methodsToImplement, entity, mutableBeanInterfaces, IMPLS.keySet());
        for (MethodModel method : methodsToImplement.values()) {
            for (Map.Entry<DataHolderKey<Relation<?>>, MethodImplementation> entry : IMPLS.entrySet()) {
                Relation<?> relation = method.getData(entry.getKey());
                if (relation != null) {
                    if (c.fields.get(relation) == null) {
                        createField(c, relation);
                    }
                    MethodBean implementation = implement(method, entry.getValue().createBody(c, method));
                    if (entry.getKey() != GETTER) {
                        implementation.setReturnType(TypeUtil.simpleType(interfaceBean));
                    }
                    classBean.getMethods().add(implementation);
                }
            }
        }

    }

    private static void collectAllMethodsToImplement(
            Map<String, MethodModel> result,
            Entity start,
            EntityRepresentationContext<ClassBean> context,
            Collection<DataHolderKey<Relation<?>>> keys
    ) {
        Queue<Entity> queue = Lists.newLinkedList();
        queue.offer(start);
        while (!queue.isEmpty()) {
            Entity entity = queue.remove();
            ClassBean classBean = context.getRepresentation(entity);
            for (MethodModel method : classBean.getMethods()) {
                for (DataHolderKey<Relation<?>> key : keys) {
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
                    TypeData elementType = context.types.targetToType(relation.getTarget(), Multiplicity.ONE);
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
                .addAnnotation(OVERRIDE)
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
                   Relation<?> relation = method.getData(GETTER);
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
                    Relation<?> relation = method.getData(SETTER);
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
                    Relation<?> relation = method.getData(ADDER);
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
                    Relation<?> relation = method.getData(ALL_ADDER);
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

    private static Map<DataHolderKey<Relation<?>>, MethodImplementation> IMPLS = ImmutableMap.<DataHolderKey<Relation<?>>, MethodImplementation>builder()
            .put(GETTER, GETTER_IMPL)
            .put(SETTER, SETTER_IMPL)
            .put(ADDER, ADDER_IMPL)
            .put(ALL_ADDER, ALL_ADDER_IMPL)
            .build();

    private static class EntityContext {
        private final Entity entity;
        private final ClassBean classBean;
        private final EntityRepresentationContext<ClassBean> context;
        private final TypeTransformer types;
        private final Map<Relation<?>, FieldModel> fields = Maps.newHashMap();

        private EntityContext(EntityRepresentationContext<ClassBean> context, Entity entity, ClassBean classBean) {
            this.context = context;
            this.types = types(context);
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
