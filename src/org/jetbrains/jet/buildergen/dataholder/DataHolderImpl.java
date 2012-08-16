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

package org.jetbrains.jet.buildergen.dataholder;

import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
* @author abreslav
*/
public class DataHolderImpl<T extends DataHolder<T>> implements WritableDataHolder<T>, DataHolder<T> {
    private Map<DataHolderKey<? super T, ?>, Object> map;

    private Map<DataHolderKey<? super T, ?>, Object> getMap() {
        if (map == null) {
            map = Maps.newHashMap();
        }
        return map;
    }

    @Override
    public <V> V getData(@NotNull DataHolderKey<? super T, V> key) {
        //noinspection unchecked
        return (V) getMap().get(key);
    }

    @Override
    @NotNull
    public <V> T put(@NotNull DataHolderKey<? super T, V> key, @NotNull V value) {
        getMap().put(key, value);
        return (T) this;
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public T copyDataFrom(@NotNull DataHolder<? extends T> other) {
        getMap().putAll(((DataHolderImpl) other).getMap());
        return (T) this;
    }
}
