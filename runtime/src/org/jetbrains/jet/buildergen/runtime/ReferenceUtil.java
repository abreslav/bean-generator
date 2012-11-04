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
import org.jetbrains.annotations.Nullable;

public class ReferenceUtil {

    @SuppressWarnings("unchecked")
    public static <T> T checkClass(@NotNull Class<T> targetClass, @Nullable Object value) {
        if (value == null) return null;
        if (targetClass.isInstance(value)) {
            return (T) value;
        }
        throw new IllegalArgumentException("Target '" + value + "' : " + value.getClass() + " is not an instance of " + targetClass);
    }
}
