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
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.jet.buildergen.entities.Entity;
import org.jetbrains.jet.buildergen.java.ClassPrinter;
import org.jetbrains.jet.buildergen.java.declarations.ClassModel;
import org.jetbrains.jet.buildergen.processors.BeanCopyProcessorGenerator;
import org.jetbrains.jet.buildergen.processors.ToStringProcessorGenerator;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.Annotated;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeConstructor;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.utils.Printer;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
public class BeanGenerator {

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
                ClassOrNamespaceDescriptor.class,
                TypeConstructor.class,
                ClassDescriptor.class,
                FunctionDescriptor.class,
                JetType.class,
                TypeProjection.class,
                ReceiverParameterDescriptor.class,
                ConstructorDescriptor.class,
                PropertyDescriptor.class,
                PropertyGetterDescriptor.class,
                PropertySetterDescriptor.class,
                PropertyAccessorDescriptor.class
        );

        String generatedSourceRoot = "bean-generator/generated";
        String mutableBeanPackage = "beans";
        String mutableBeanClassPackage = "beans.impl";
        String mutableBeanUtilPackage = "beans.util";
        String builderClassPackage = "builders";
        String beanBuilderPackage = "beans.builders";

        generateBeans(
                classesWithBuilders,
                generatedSourceRoot,
                mutableBeanPackage,
                mutableBeanClassPackage,
                mutableBeanUtilPackage,
                builderClassPackage,
                beanBuilderPackage
        );
    }

    public static void generateBeans(
            List<? extends Class<?>> classesWithBuilders,
            String generatedSourceRoot,
            String mutableBeanPackage,
            String mutableBeanClassPackage,
            String mutableBeanUtilPackage,
            String builderClassPackage,
            String beanBuilderPackage
    ) throws IOException {
        Context context = new Context();
        EntityBuilder.javaClassesToEntities(classesWithBuilders, context.dataClasses);
        Collection<Entity> entities = context.dataClasses.getEntities();

        Collection<ClassModel> mutableBeans = new MutableBeanInterfaceGenerator().generate(
                entities,
                context.mutableBeanInterfaces,
                mutableBeanPackage
        );

        Collection<ClassModel> mutableBeanClasses = new MutableBeanImplementationGenerator(context.mutableBeanInterfaces).generate(
                entities,
                context.mutableBeanImplementationClasses,
                mutableBeanClassPackage
        );

        ClassModel beanUtil = BeanUtilGenerator.generate(mutableBeanUtilPackage, "BeanUtil", context.mutableBeanInterfaces,
                                                         context.mutableBeanImplementationClasses);
        ClassModel dataToBeanUtil = DataToBeanGenerator.generate(mutableBeanUtilPackage, "DataToBean", context.mutableBeanInterfaces,
                                                                 context.mutableBeanImplementationClasses);
        ClassModel copyProcessor = BeanCopyProcessorGenerator
                .generate(mutableBeanUtilPackage, "CopyProcessor", context.mutableBeanInterfaces,
                          context.mutableBeanImplementationClasses);
        ClassModel toStringProcessor = ToStringProcessorGenerator.generate(mutableBeanUtilPackage, "ToString", context.mutableBeanInterfaces);

        Collection<ClassModel> builderClasses = new BuilderClassGenerator().generate(
                entities,
                context.builderClasses,
                builderClassPackage
        );

        Collection<ClassModel> beanBuilderClasses = new BeanBuilderClassGenerator(
                context.mutableBeanInterfaces, context.mutableBeanImplementationClasses, context.builderClasses
        ).generate(
                entities,
                context.beanBuilders,
                beanBuilderPackage
        );

        ClassModel builderUtil = DataBuilderGenerator.generate(builderClassPackage, "DataBuilder", context.dataClasses,
                                                               context.builderClasses);

        writeToFiles(generatedSourceRoot, mutableBeanPackage, mutableBeans);
        writeToFiles(generatedSourceRoot, mutableBeanClassPackage, mutableBeanClasses);
        writeToFiles(generatedSourceRoot, mutableBeanUtilPackage, Lists.newArrayList(
                beanUtil,
                dataToBeanUtil,
                copyProcessor,
                toStringProcessor
        ));
        writeToFiles(generatedSourceRoot, builderClassPackage, builderClasses);
        writeToFiles(generatedSourceRoot, beanBuilderPackage, beanBuilderClasses);
        writeToFiles(generatedSourceRoot, builderClassPackage, Collections.singletonList(builderUtil));
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

    private static class Context {
        EntityRepresentationContextImpl mutableBeanInterfaces = new EntityRepresentationContextImpl();
        EntityRepresentationContextImpl mutableBeanImplementationClasses = new EntityRepresentationContextImpl();
        EntityRepresentationContextImpl builderClasses = new EntityRepresentationContextImpl();
        EntityRepresentationContextImpl dataClasses = new EntityRepresentationContextImpl();
        EntityRepresentationContextImpl beanBuilders = new EntityRepresentationContextImpl();
    }
}
