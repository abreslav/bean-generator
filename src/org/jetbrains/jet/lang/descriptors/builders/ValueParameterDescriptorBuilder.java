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

package org.jetbrains.jet.lang.descriptors.builders;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptorImpl;
import org.jetbrains.jet.lang.types.JetType;

/**
 * @author abreslav
 */
public class ValueParameterDescriptorBuilder extends CallableDescriptorBuilder<ValueParameterDescriptor> {

    private Integer index;
    private Boolean isVar;
    private Boolean declaresDefaultValue;
    private Builder<? extends JetType> varargElementType;

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public Boolean isVar() {
        return isVar;
    }

    public void setVar(Boolean var) {
        isVar = var;
    }

    public Boolean getDeclaresDefaultValue() {
        return declaresDefaultValue;
    }

    public void setDeclaresDefaultValue(Boolean declaresDefaultValue) {
        this.declaresDefaultValue = declaresDefaultValue;
    }

    public Builder<? extends JetType> getVarargElementType() {
        return varargElementType;
    }

    public void setVarargElementType(Builder<? extends JetType> varargElementType) {
        this.varargElementType = varargElementType;
    }

    @NotNull
    @Override
    public ValueParameterDescriptor build(DeclarationDescriptor containingDeclaration) {
        return new ValueParameterDescriptorImpl(
                containingDeclaration,
                getIndex(),
                buildList(getAnnotations()),
                getName(),
                isVar(),
                getType().build(),
                getDeclaresDefaultValue(),
                getVarargElementType().build()
        );
    }
}
