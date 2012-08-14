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

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.MethodModel;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.ParameterModel;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.types.TypeData;

import java.util.List;

/**
* @author abreslav
*/
public final class MethodBean extends MemberBean<MethodBean> implements MethodModel {
    private boolean _abstract;
    private TypeData returnType;
    private final List<ParameterModel> parameters = Lists.newArrayList();

    @NotNull
    @Override
    public TypeData getReturnType() {
        return returnType;
    }

    @NotNull
    public MethodBean setReturnType(@NotNull TypeData type) {
        this.returnType = type;
        return this;
    }

    @Override
    public boolean isAbstract() {
        return _abstract;
    }

    @NotNull
    public MethodBean setAbstract(boolean _abstract) {
        this._abstract = _abstract;
        return this;
    }

    @NotNull
    @Override
    public List<ParameterModel> getParameters() {
        return parameters;
    }

    @NotNull
    public MethodBean addParameter(@NotNull ParameterModel parameter) {
        parameters.add(parameter);
        return this;
    }

    @NotNull
    public MethodBean addParameters(@NotNull List<? extends  ParameterModel> parameters) {
        this.parameters.addAll(parameters);
        return this;
    }


}
