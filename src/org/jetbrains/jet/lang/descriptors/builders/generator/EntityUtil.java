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

import java.util.Collection;
import java.util.List;

/**
 * @author abreslav
 */
public class EntityUtil {
    @NotNull
    public static Collection<Relation<?>> getAllRelations(@NotNull Entity entity) {
        List<Relation<?>> result = Lists.newArrayList();
        collectAllRelations(entity, result);
        return result;
    }

    private static void collectAllRelations(Entity entity, List<Relation<?>> result) {
        result.addAll(entity.getRelations());
        for (Entity superEntity : entity.getSuperEntities()) {
            collectAllRelations(superEntity, result);
        }
    }
}
