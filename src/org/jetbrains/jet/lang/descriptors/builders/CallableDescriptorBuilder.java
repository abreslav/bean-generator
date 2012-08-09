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

import com.google.common.collect.Lists;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetType;

import java.util.List;

/**
 * @author abreslav
 */
public abstract class CallableDescriptorBuilder<D extends CallableDescriptor> extends AbstractDescriptorBuilder<D> {
    private DescriptorBuilder<? extends DeclarationDescriptor> original;
    private Visibility visibility;
    private boolean inline;

    private final List<DescriptorBuilder<? extends TypeParameterDescriptor>> typeParameters = Lists.newArrayList();
    private Builder<? extends ReceiverDescriptor> expectedThisObject;
    private Builder<? extends ReceiverDescriptor> receiverParameter;
    private final List<DescriptorBuilder<? extends ValueParameterDescriptor>> valueParameters = Lists.newArrayList();
    private Builder<? extends JetType> type;

    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public boolean isInline() {
        return inline;
    }

    public void setInline(boolean inline) {
        this.inline = inline;
    }

    public List<DescriptorBuilder<? extends TypeParameterDescriptor>> getTypeParameters() {
        return typeParameters;
    }

    public List<DescriptorBuilder<? extends ValueParameterDescriptor>> getValueParameters() {
        return valueParameters;
    }

    public DescriptorBuilder<? extends DeclarationDescriptor> getOriginal() {
        return original;
    }

    public void setOriginal(DescriptorBuilder<? extends DeclarationDescriptor> original) {
        this.original = original;
    }

    public Builder<? extends ReceiverDescriptor> getExpectedThisObject() {
        return expectedThisObject;
    }

    public void setExpectedThisObject(Builder<? extends ReceiverDescriptor> expectedThisObject) {
        this.expectedThisObject = expectedThisObject;
    }

    public Builder<? extends ReceiverDescriptor> getReceiverParameter() {
        return receiverParameter;
    }

    public void setReceiverParameter(Builder<? extends ReceiverDescriptor> receiverParameter) {
        this.receiverParameter = receiverParameter;
    }

    public Builder<? extends JetType> getType() {
        return type;
    }

    public void setType(Builder<? extends JetType> type) {
        this.type = type;
    }
}
