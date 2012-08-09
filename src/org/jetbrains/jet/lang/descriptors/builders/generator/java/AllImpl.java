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

package org.jetbrains.jet.lang.descriptors.builders.generator.java;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * @author abreslav
 */
public class AllImpl extends All {
    public static abstract class DataHolderImpl implements DataHolder {
        private static Map<DataHolderKey<?>, Object> map;

        @Override
        public <V> V getData(@NotNull DataHolderKey<V> key) {
            //noinspection unchecked
            return (V) map.get(key);
        }

        public <V> void put(@NotNull DataHolderKey<V> key, @NotNull V value) {
            map.put(key, value);
        }
    }

    public static abstract class NamedBean<T extends NamedBean<T>> extends DataHolderImpl implements NamedModel {
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
    }

    public static abstract class AnnotatedBean<T extends AnnotatedBean<T>> extends NamedBean<T> implements AnnotatedModel {
        private final List<TypeModel> annotations = Lists.newArrayList();

        @NotNull
        @Override
        public List<TypeModel> getAnnotations() {
            return annotations;
        }

        public T addAnnotation(@NotNull TypeModel typeModel) {
            annotations.add(typeModel);
            return (T) this;
        }

    }

    public static abstract class MemberBean<T extends MemberBean<T>> extends AnnotatedBean<T> implements MemberModel {
        private Visibility visibility;

        @NotNull
        @Override
        public Visibility getVisibility() {
            return visibility;
        }

        @NotNull
        public T setVisibility(Visibility visibility) {
            this.visibility = visibility;
            return (T) this;
        }
    }

    public static final class TypeBean implements TypeModel {
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

    public static final class ClassBean extends MemberBean<ClassBean> implements ClassModel {
        private String packageFqName;
        private boolean _abstract;
        private ClassKind kind;
        private TypeModel superClass;
        private final List<TypeModel> superInterfaces = Lists.newArrayList();
        private final List<FieldModel> fields = Lists.newArrayList();
        private final List<MethodModel> methods = Lists.newArrayList();

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
        public TypeModel getSuperClass() {
            return superClass;
        }

        @NotNull
        public ClassBean setSuperClass(@NotNull TypeModel superClass) {
            this.superClass = superClass;
            return this;
        }

        @NotNull
        @Override
        public List<TypeModel> getSuperInterfaces() {
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
    }

    public static abstract class VariableBean<T extends VariableBean<T>> extends AnnotatedBean<T> implements VariableModel {
        private TypeModel type;

        @NotNull
        @Override
        public TypeModel getType() {
            return type;
        }

        @NotNull
        public T setType(@NotNull TypeModel type) {
            this.type = type;
            return (T) this;
        }
    }

    public static final class FieldBean extends VariableBean<FieldBean> implements FieldModel {
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

    public static final class ParameterBean extends VariableBean<ParameterBean> implements ParameterModel {
    }

    public static final class MethodBean extends MemberBean<MethodBean> implements MethodModel {
        private boolean _abstract;
        private TypeModel returnType;
        private final List<ParameterModel> parameters = Lists.newArrayList();

        @NotNull
        @Override
        public TypeModel getReturnType() {
            return returnType;
        }

        @NotNull
        public MethodBean setReturnType(@NotNull TypeModel type) {
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
    }
}
