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

package org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.beans;

import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.DataHolder;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.DataHolderKey;

import java.util.Map;

/**
* @author abreslav
*/
public abstract class DataHolderImpl<T extends DataHolderImpl<T>> implements DataHolder {
    private Map<DataHolderKey<?>, Object> map;

    private Map<DataHolderKey<?>, Object> getMap() {
        if (map == null) {
            map = Maps.newHashMap();
        }
        return map;
    }

    @Override
    public <V> V getData(@NotNull DataHolderKey<V> key) {
        //noinspection unchecked
        return (V) getMap().get(key);
    }

    @NotNull
    public <V> T put(@NotNull DataHolderKey<V> key, @NotNull V value) {
        getMap().put(key, value);
        return (T) this;
    }

    @NotNull
    public T copyDataFrom(@NotNull DataHolder other) {
        getMap().putAll(((DataHolderImpl<?>) other).getMap());
        return (T) this;
    }
}
