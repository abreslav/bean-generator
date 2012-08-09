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

package org.jetbrains.jet.lang.descriptors.builders.generator;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
* @author abreslav
*/
public abstract class TypeRenderer {

    public <T> String renderType(Relation<T> relation) {
        T target = relation.getTarget();
        if (target instanceof Entity) {
            Entity entity = (Entity) target;
            return renderCollectionType(relation.getMultiplicity(), getEntityRepresentationName(entity));
        }
        else if (target instanceof Type) {
            Type type = (Type) target;
            return renderReflectionType(type);
        }
        throw new IllegalArgumentException("Unsupported target type:" + target);
    }

    protected String renderCollectionType(Multiplicity multiplicity, String name) {
        switch (multiplicity) {
            case ZERO_OR_ONE:
            case ONE:
                return name;
            case LIST:
                return "List<" + name + ">";
            case SET:
                return "Set<" + name + ">";
            case COLLECTION:
                return "Collection<" + name + ">";
        }
        throw new IllegalStateException("Unknown multiplicity: " + multiplicity);
    }

    protected abstract String getEntityRepresentationName(@NotNull Entity entity);

    protected static String renderReflectionType(Type type) {
        if (type instanceof Class<?>) {
            Class<?> theClass = (Class<?>) type;
            return theClass.getSimpleName();
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            StringBuilder builder = new StringBuilder(renderReflectionType(parameterizedType.getRawType()));
            Type[] arguments = parameterizedType.getActualTypeArguments();
            if (arguments.length > 0) {
                builder.append("<");
                for (int i = 0, length = arguments.length; i < length; i++) {
                    Type arg = arguments[i];
                    builder.append(renderReflectionType(arg));
                    if (i < length - 1) {
                        builder.append(", ");
                    }
                }
                builder.append(">");
            }
            return builder.toString();
        }
        throw new IllegalArgumentException("Unsupported reflection type: " + type);
    }
}
