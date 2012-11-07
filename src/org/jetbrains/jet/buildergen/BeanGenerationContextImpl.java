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

package org.jetbrains.jet.buildergen;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.buildergen.entities.Entity;

import java.util.Collection;

public class BeanGenerationContextImpl implements BeanGenerationContext {
    private final Collection<Entity> entities = Lists.newArrayList();

    private final EntityRepresentationContextImpl beanInterfaces = new EntityRepresentationContextImpl();
    private final EntityRepresentationContextImpl beanImplementations = new EntityRepresentationContextImpl();
    private final EntityRepresentationContextImpl referenceInterfaces = new EntityRepresentationContextImpl();
    private final EntityRepresentationContextImpl literalReferenceClasses = new EntityRepresentationContextImpl();
    private final EntityRepresentationContextImpl proxyReferenceClasses = new EntityRepresentationContextImpl();

    public BeanGenerationContextImpl(Collection<Entity> entities) {
        this.entities.addAll(entities);
    }

    @Override
    @NotNull
    public Collection<Entity> getEntities() {
        return entities;
    }

    @Override
    @NotNull
    public EntityRepresentationContextImpl getBeanInterfaces() {
        return beanInterfaces;
    }

    @Override
    @NotNull
    public EntityRepresentationContextImpl getBeanImplementations() {
        return beanImplementations;
    }

    @Override
    @NotNull
    public EntityRepresentationContextImpl getReferenceInterfaces() {
        return referenceInterfaces;
    }

    @Override
    @NotNull
    public EntityRepresentationContextImpl getLiteralReferenceClasses() {
        return literalReferenceClasses;
    }

    @Override
    @NotNull
    public EntityRepresentationContextImpl getProxyReferenceClasses() {
        return proxyReferenceClasses;
    }
}
