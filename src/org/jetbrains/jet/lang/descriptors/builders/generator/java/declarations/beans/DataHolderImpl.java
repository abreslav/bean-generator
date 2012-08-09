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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.DataHolder;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.DataHolderKey;

import java.util.Map;

/**
* @author abreslav
*/
public abstract class DataHolderImpl implements DataHolder {
    private static Map<DataHolderKey<?>, Object> map;

    @Override
    public <V> V getData(@NotNull DataHolderKey<V> key) {
        //noinspection unchecked
        return (V) map.get(key);
    }

    public <V> void put(@NotNull DataHolderKey<V> key, @NotNull V value) {
        map.put(key, value);
    }
}
