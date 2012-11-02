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

import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.buildergen.entities.Entity;
import org.jetbrains.jet.buildergen.java.declarations.beans.ClassBean;

import java.util.Collection;
import java.util.Map;

public class EntityRepresentationContextImpl implements EntityRepresentationContext<ClassBean> {
    private final Map<Entity, ClassBean> map = Maps.newLinkedHashMap();

    @Override
    public void registerRepresentation(@NotNull Entity entity, @NotNull ClassBean representation) {
        map.put(entity, representation);
    }

    @Override
    public ClassBean getRepresentation(@NotNull Entity entity) {
        return map.get(entity);
    }

    @NotNull
    @Override
    public Collection<Entity> getEntities() {
        return map.keySet();
    }

    @NotNull
    @Override
    public Collection<ClassBean> getRepresentations() {
        return map.values();
    }
}
