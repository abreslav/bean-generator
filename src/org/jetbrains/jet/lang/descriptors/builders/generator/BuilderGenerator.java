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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.Annotated;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.ClassPrinter;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.ClassModel;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.beans.ClassBean;
import org.jetbrains.jet.utils.Printer;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author abreslav
 */
public class BuilderGenerator {

    public static void main(String[] args) throws IOException {
        List<Class<?>> classesWithBuilders = Lists.<Class<?>>newArrayList(
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


        String generatedSourceRoot = "bean-generator/generated";
        String readOnlyBeanPackage = "beans";
        String mutableBeanPackage = "beans.mutable";

        Context context = new Context();

        Collection<ClassModel> readOnlyBeans = new ReadOnlyBeanGenerator().generate(
                entities,
                context.readOnlyBeanInterfaces,
                readOnlyBeanPackage
        );

        Collection<ClassModel> mutableBeans = new MutableBeanGenerator().generate(
                entities,
                context.mutableBeanImplementationClasses,
                mutableBeanPackage
        );

        //Collection<ClassModel> mutableBeanImpls = new MutableBeanClassesGenerator(entities, context.mutableBeanImplementationClasses, mutableBeanPackage).generate();

        writeToFiles(generatedSourceRoot, readOnlyBeanPackage, readOnlyBeans);
        writeToFiles(generatedSourceRoot, mutableBeanPackage, mutableBeans);
        //writeToFiles(generatedSourceRoot, mutableBeanPackage, mutableBeanImpls);
    }

    private static void writeToFiles(String generatedSourceRoot, String packageName, Collection<ClassModel> readOnlyBeans)
            throws IOException {
        File sourceRoot = new File(generatedSourceRoot);
        assert sourceRoot.isDirectory();
        File packageDir = new File(sourceRoot, packageToPath(packageName));
        for (ClassModel classModel : readOnlyBeans) {
            StringBuilder out = new StringBuilder();
            Printer p = new Printer(out);
            ClassPrinter.printClass(classModel, p);
            File file = new File(packageDir, classModel.getName() + ".java");
            FileUtil.writeToFile(file, out.toString());
        }
    }

    private static String packageToPath(String packageFqName) {
        return packageFqName.replace('.', '/');
    }

    private static class Context implements BeanGenerationContext {
        RepresentationContext readOnlyBeanInterfaces = new RepresentationContext();
        RepresentationContext mutableOnlyBeanInterfaces = new RepresentationContext();
        RepresentationContext mutableBeanImplementationClasses = new RepresentationContext();

        @Override
        public ClassModel getReadOnlyBeanInterface(@NotNull Entity entity) {
            return readOnlyBeanInterfaces.map.get(entity);
        }

        @Override
        public ClassModel getMutableBeanInterface(@NotNull Entity entity) {
            return mutableOnlyBeanInterfaces.map.get(entity);
        }

        @Override
        public ClassModel getMutableBeanImplementationClass(@NotNull Entity entity) {
            return mutableBeanImplementationClasses.map.get(entity);
        }
    }

    private static class RepresentationContext implements EntityRepresentationContext<ClassBean> {
        Map<Entity, ClassBean> map = Maps.newHashMap();

        @Override
        public void registerRepresentation(@NotNull Entity entity, @NotNull ClassBean representation) {
            map.put(entity, representation);
        }

        @Override
        public ClassBean getRepresentation(@NotNull Entity entity) {
            return map.get(entity);
        }

        @NotNull
        @Override
        public Collection<Entity> getEntities() {
            return map.keySet();
        }

        @NotNull
        @Override
        public Collection<ClassBean> getRepresentations() {
            return map.values();
        }
    }

}
