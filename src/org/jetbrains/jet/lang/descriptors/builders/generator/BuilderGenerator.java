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

import com.google.common.collect.Sets;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.Annotated;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.All;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.ClassPrinter;
import org.jetbrains.jet.utils.Printer;

import java.util.Collection;
import java.util.Set;

/**
 * @author abreslav
 */
public class BuilderGenerator {

    public static void main(String[] args) {
        Set<Class<?>> classesWithBuilders = Sets.<Class<?>>newHashSet(
                Named.class,
                Annotated.class,
                DeclarationDescriptor.class,
                DeclarationDescriptorNonRoot.class,
                CallableDescriptor.class,
                CallableMemberDescriptor.class,
                MemberDescriptor.class,
                DeclarationDescriptorWithVisibility.class,
                VariableDescriptor.class,
                ValueParameterDescriptor.class,
                ClassifierDescriptor.class,
                TypeParameterDescriptor.class,
                FunctionDescriptor.class
        );

        Collection<Entity> entities = EntityBuilder.javaClassesToEntities(classesWithBuilders);

        StringBuilder out = new StringBuilder();
        Printer p = new Printer(out);

        Collection<All.ClassModel> classes = new MutableBeanGenerator(entities, "p").generate();
        //Collection<All.ClassModel> classes = new ReadOnlyBeanGenerator(entities, "p").generate();

        for (All.ClassModel classModel : classes) {
            ClassPrinter.printClass(classModel, p);
            p.println();
            p.println("==============");
            p.println();
        }


        System.err.flush();
        System.out.println(out);
    }
}
