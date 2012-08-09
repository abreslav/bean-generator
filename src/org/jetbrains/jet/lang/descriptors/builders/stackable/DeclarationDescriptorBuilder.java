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

package org.jetbrains.jet.lang.descriptors.builders.stackable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.resolve.name.Name;

/**
 * @author abreslav
 */
public abstract class DeclarationDescriptorBuilder<B extends DeclarationDescriptorBuilder<?>> {

    @Nullable
    protected final B delegate;

    protected DeclarationDescriptorBuilder(@Nullable B delegate) {
        this.delegate = delegate;
    }

    public void setName(@NotNull Name name) {
        if (delegate != null) {
            delegate.setName(name);
        }
    }

    @NotNull
    public AnnotationDescriptorBuilder addAnnotation() {
        if (delegate != null) {
            return delegate.addAnnotation();
        }
        throw notImplemented();
    }

    public void setOriginal(@NotNull DeclarationDescriptor original) {
        if (delegate != null) {
            delegate.setOriginal(original);
        }
    }

    public void setContainingDeclaration(@NotNull DeclarationDescriptor containingDeclaration) {
        if (delegate != null) {
            delegate.setContainingDeclaration(containingDeclaration);
        }
    }

    protected static RuntimeException notImplemented() {
        return new UnsupportedOperationException("Override to return a proper builder");
    }
}
