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

package org.jetbrains.jet.buildergen.entities;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.buildergen.dataholder.DataHolderImpl;

import java.util.Collection;
import java.util.List;

/**
* @author abreslav
*/
public class RelationWithTarget<T> extends DataHolderImpl<Relation<T>> implements Relation<T> {
    private final List<Relation<?>> overriddenRelations = Lists.newArrayList();
    private final T target;
    private final String name;
    private final Multiplicity multiplicity;

    public RelationWithTarget(@NotNull Multiplicity multiplicity, @NotNull String name, @NotNull T target) {
        this.target = target;
        this.name = name;
        this.multiplicity = multiplicity;
    }

    @NotNull
    @Override
    public Multiplicity getMultiplicity() {
        return multiplicity;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @NotNull
    @Override
    public T getTarget() {
        return target;
    }

    @NotNull
    @Override
    public Collection<Relation<?>> getOverriddenRelations() {
        return overriddenRelations;
    }

    @Override
    public String toString() {
        return getName() + ": [" + getMultiplicity() + "] " + target;
    }
}
