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

public abstract class LiteralReference implements BeanReference {
    private final Object data;

    public LiteralReference(Object data) {
        this.data = data;
    }

    @Override
    public <T> T resolveTo(@NotNull Class<T> targetClass) {
        Object value = data;
        return ReferenceUtil.checkClass(targetClass, value);
    }
}
