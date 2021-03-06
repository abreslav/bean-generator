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

package org.jetbrains.jet.buildergen.java.declarations.beans;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.buildergen.java.declarations.AnnotatedModel;
import org.jetbrains.jet.buildergen.java.types.TypeData;

import java.util.Collection;
import java.util.List;

/**
* @author abreslav
*/
public abstract class AnnotatedBean<T extends AnnotatedBean<T>> extends NamedBean<T> implements AnnotatedModel {
    private final List<TypeData> annotations = Lists.newArrayList();

    @NotNull
    @Override
    public List<TypeData> getAnnotations() {
        return annotations;
    }

    @NotNull
    public T addAnnotation(@NotNull TypeData typeData) {
        annotations.add(typeData);
        return (T) this;
    }

    @NotNull
    public T addAnnotations(@NotNull Collection<? extends TypeData> annotations) {
        this.annotations.addAll(annotations);
        return (T) this;
    }

}
