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
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.TypeModel;

import java.util.List;

/**
* @author abreslav
*/
public final class TypeBean implements TypeModel {
    private String packageFqName;
    private String className;
    private final List<TypeModel> arguments = Lists.newArrayList();

    @NotNull
    @Override
    public String getPackageFqName() {
        return packageFqName;
    }

    @NotNull
    public TypeBean setPackageFqName(@NotNull String packageFqName) {
        this.packageFqName = packageFqName;
        return this;
    }

    @NotNull
    @Override
    public String getClassName() {
        return className;
    }

    @NotNull
    public TypeBean setClassName(String className) {
        this.className = className;
        return this;
    }

    @NotNull
    @Override
    public List<TypeModel> getArguments() {
        return arguments;
    }

    @NotNull
    public TypeBean addArgument(@NotNull TypeModel argument) {
        arguments.add(argument);
        return this;
    }
}
