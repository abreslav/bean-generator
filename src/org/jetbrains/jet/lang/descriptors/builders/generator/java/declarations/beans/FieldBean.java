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
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.FieldModel;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.Visibility;

/**
* @author abreslav
*/
public final class FieldBean extends VariableBean<FieldBean> implements FieldModel {
    private Visibility visibility;
    private boolean _final;

    @NotNull
    @Override
    public Visibility getVisibility() {
        return visibility;
    }

    @NotNull
    public FieldBean setVisibility(Visibility visibility) {
        this.visibility = visibility;
        return this;
    }

    public boolean isFinal() {
        return _final;
    }

    @NotNull
    public FieldBean setFinal(boolean _final) {
        this._final = _final;
        return this;
    }
}
