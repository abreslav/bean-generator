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
import org.jetbrains.jet.lang.descriptors.builders.generator.dataholder.DataHolder;
import org.jetbrains.jet.lang.descriptors.builders.generator.dataholder.DataHolderImpl;
import org.jetbrains.jet.lang.descriptors.builders.generator.dataholder.DataHolderKey;
import org.jetbrains.jet.lang.descriptors.builders.generator.dataholder.WritableDataHolder;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.*;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.types.TypeData;

import java.util.List;

/**
* @author abreslav
*/
public final class ClassBean extends MemberBean<ClassBean> implements ClassModel, WritableDataHolder<ClassModel> {
    private String packageFqName;
    private boolean _abstract;
    private ClassKind kind;
    private TypeData superClass;
    private final List<TypeData> superInterfaces = Lists.newArrayList();
    private final List<FieldModel> fields = Lists.newArrayList();
    private final List<MethodModel> methods = Lists.newArrayList();
    private final List<MethodModel> constructors = Lists.newArrayList();

    private final DataHolderImpl<ClassModel> dataHolder = new DataHolderImpl<ClassModel>();

    @NotNull
    @Override
    public String getPackageFqName() {
        return packageFqName;
    }

    @NotNull
    public ClassBean setPackageFqName(@NotNull String packageFqName) {
        this.packageFqName = packageFqName;
        return this;
    }

    @Override
    public boolean isAbstract() {
        return _abstract;
    }

    @NotNull
    public ClassBean setAbstract(boolean _abstract) {
        this._abstract = _abstract;
        return this;
    }

    @NotNull
    @Override
    public ClassKind getKind() {
        return kind;
    }

    @NotNull
    public ClassBean setKind(@NotNull ClassKind kind) {
        this.kind = kind;
        return this;
    }

    @Override
    public TypeData getSuperClass() {
        return superClass;
    }

    @NotNull
    public ClassBean setSuperClass(@NotNull TypeData superClass) {
        this.superClass = superClass;
        return this;
    }

    @NotNull
    @Override
    public List<TypeData> getSuperInterfaces() {
        return superInterfaces;
    }

    @NotNull
    @Override
    public List<FieldModel> getFields() {
        return fields;
    }

    @NotNull
    @Override
    public List<MethodModel> getMethods() {
        return methods;
    }

    @NotNull
    @Override
    public List<MethodModel> getConstructors() {
        return constructors;
    }

    @Override
    public <V> V getData(@NotNull DataHolderKey<? super ClassModel, V> key) {
        return dataHolder.getData(key);
    }

    @Override
    @NotNull
    public <V> ClassBean put(@NotNull DataHolderKey<? super ClassModel, V> key, @NotNull V value) {
        dataHolder.put(key, value);
        return this;
    }

    @Override
    @NotNull
    public ClassBean copyDataFrom(@NotNull DataHolder<? extends ClassModel> other) {
        dataHolder.copyDataFrom(((ClassBean) other).dataHolder);
        return this;
    }
}
