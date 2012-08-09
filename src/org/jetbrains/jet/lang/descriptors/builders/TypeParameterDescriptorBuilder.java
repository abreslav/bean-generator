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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptorImpl;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.Variance;

import java.util.Collection;

/**
 * @author abreslav
 */
public class TypeParameterDescriptorBuilder extends AbstractDescriptorBuilder<TypeParameterDescriptor> {

    private Boolean reified;
    private Variance variance;
    private Integer index;
    private final Collection<Builder<? extends JetType>> classObjectBounds = Lists.newArrayList();
    private final Collection<Builder<? extends JetType>> upperBounds = Lists.newArrayList();

    public Boolean getReified() {
        return reified;
    }

    public void setReified(Boolean reified) {
        this.reified = reified;
    }

    public Variance getVariance() {
        return variance;
    }

    public void setVariance(Variance variance) {
        this.variance = variance;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    @NotNull
    @Override
    public TypeParameterDescriptor build(DeclarationDescriptor containingDeclaration) {
        TypeParameterDescriptorImpl result = TypeParameterDescriptorImpl.createForFurtherModification(
                containingDeclaration,
                buildList(getAnnotations()),
                getReified(),
                getVariance(),
                getName(),
                getIndex()
        );
        for (Builder<? extends JetType> bound : classObjectBounds) {
            result.addClassObjectBound(bound.build());
        }
        for (Builder<? extends  JetType> bound : upperBounds) {
            result.addUpperBound(bound.build());
        }
        if (upperBounds.isEmpty()) {
            result.addDefaultUpperBound();
        }
        result.setInitialized();
        return result;
    }
}
