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

package org.jetbrains.jet.buildergen.runtime;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public abstract class ProxyReference implements BeanReference {

    private final Object key;
    private final Map<?, ?> map;
    private final Object _default;

    protected ProxyReference(@NotNull Map<?, ?> map, Object key, Object _default) {
        this.key = key;
        this.map = map;
        this._default = _default;
    }

    @Override
    public <T> T resolveTo(@NotNull Class<T> targetClass) {
        Object value = map.containsKey(key) ? map.get(key) : _default;

        return ReferenceUtil.checkClass(targetClass, value);
    }
}
