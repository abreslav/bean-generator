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

package org.jetbrains.jet.lang.descriptors.builders;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Collection;
import java.util.List;

/**
 * @author abreslav
 */
public abstract class AbstractDescriptorBuilder<D extends DeclarationDescriptor> implements DescriptorBuilder<D> {

    private Name name;
    private final List<Builder<? extends AnnotationDescriptor>> annotations = Lists.newArrayList();

    public void setName(@NotNull Name name) {
        this.name = name;
    }

    @NotNull
    public Name getName() {
        return name;
    }

    @NotNull
    public List<Builder<? extends AnnotationDescriptor>> getAnnotations() {
        return annotations;
    }

    public void addAnnotation(@NotNull Builder<? extends  AnnotationDescriptor> annotationDescriptor) {
        annotations.add(annotationDescriptor);
    }

    public void addAnnotationBuilders(@NotNull Collection<? extends Builder<? extends  AnnotationDescriptor>> builders) {
        annotations.addAll(builders);
    }

    @NotNull
    public static <E> List<E> buildList(@NotNull Collection<? extends DescriptorBuilder<? extends E>> builders, DeclarationDescriptor containingDeclaration) {
        List<E> result = Lists.newArrayList();
        for (DescriptorBuilder<? extends E> builder : builders) {
            result.add(builder.build(containingDeclaration));
        }
        return result;
    }

    @NotNull
    public static <E> List<E> buildList(@NotNull Collection<? extends Builder<? extends E>> builders) {
        List<E> result = Lists.newArrayList();
        for (Builder<? extends E> builder : builders) {
            result.add(builder.build());
        }
        return result;
    }

    @NotNull
    public static <T> List<DescriptorBuilder<T>> dummyBuilders(@NotNull Collection<? extends T> ts) {
        List<DescriptorBuilder<T>> result = Lists.newArrayList();
        for (T t : ts) {
            result.add(dummyBuilder(t));
        }
        return result;
    }

    @NotNull
    public static <T> DescriptorBuilder<T> dummyBuilder(@NotNull final T t) {
        return new DescriptorBuilder<T>() {
            @NotNull
            @Override
            public T build(DeclarationDescriptor containingDeclaration) {
                return t;
            }

            @Override
            public String toString() {
                return "dummyBuilder(" + t + ")";
            }
        };
    }
}
