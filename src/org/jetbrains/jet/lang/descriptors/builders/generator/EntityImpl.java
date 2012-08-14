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
import org.jetbrains.jet.lang.descriptors.builders.generator.dataholder.DataHolderImpl;

import java.util.Collection;
import java.util.List;

/**
* @author abreslav
*/
public class EntityImpl extends DataHolderImpl<Entity> implements Entity {
    private final String name;
    private final List<Relation<?>> relations = Lists.newArrayList();
    private final List<Entity> superEntities = Lists.newArrayList();

    public EntityImpl(String name) {
        this.name = name;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @NotNull
    @Override
    public List<Relation<?>> getRelations() {
        return relations;
    }

    @NotNull
    @Override
    public Collection<Entity> getSuperEntities() {
        return superEntities;
    }

    @Override
    public String toString() {
        return name;
    }
}
