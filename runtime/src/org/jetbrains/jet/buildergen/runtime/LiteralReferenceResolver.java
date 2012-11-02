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

public class LiteralReferenceResolver implements ReferenceResolver {
    public static final ReferenceResolver INSTANCE = new LiteralReferenceResolver();

    private LiteralReferenceResolver() {}

    @Override
    public <T> T resolve(@NotNull BeanReference<T> reference) {
        if (reference instanceof LiteralReference) {
            LiteralReference literalReference = (LiteralReference) reference;
            //noinspection unchecked
            return (T) literalReference.getData();
        }
        throw new IllegalArgumentException("Unresolved reference: " + reference);
    }
}
