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

package org.jetbrains.jet.buildergen.java.declarations.beans;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.buildergen.java.declarations.NamedModel;

/**
* @author abreslav
*/
public abstract class NamedBean<T extends NamedBean<T>> implements NamedModel {
    private String name;

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @NotNull
    public T setName(@NotNull String name) {
        this.name = name;
        return (T) this;
    }

    @Override
    public String toString() {
        return getName();
    }
}
