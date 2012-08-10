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

import com.google.common.collect.Sets;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.code.CodePrinter;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.code.PieceOfCode;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.*;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.beans.DataHolderKeyImpl;
import org.jetbrains.jet.utils.Printer;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author abreslav
 */
public class ClassPrinter {

    public static final DataHolderKey<PieceOfCode> METHOD_BODY = DataHolderKeyImpl.create("METHOD_BODY");

    public static void printClass(ClassModel classModel, Printer p) {
        if (!classModel.getPackageFqName().isEmpty()) {
            p.println("package ", classModel.getPackageFqName(), ";");
        }
        ClassPrinter classPrinter = new ClassPrinter();
        classPrinter.printClass(classModel);

        p.println();
        for (Pair<String, String> importedType : classPrinter.importedTypes) {
            String packageFqName = importedType.getFirst();
            if (packageFqName != null
                    && !packageFqName.isEmpty()
                    && !packageFqName.equals("java.lang")
                    && !packageFqName.equals(classModel.getPackageFqName())) {
                p.println("import ", importedType.getFirst(), ".", importedType.getSecond(), ";");
            }
        }
        p.println();

        p.printWithNoIndent(classPrinter.body);
    }

    private final Printer p;

    private final Set<Pair<String, String>> importedTypes = Sets.newHashSet();

    private final StringBuilder body = new StringBuilder();

    private ClassPrinter() {
        this.p = new Printer(body);
    }

    private void printClass(ClassModel classModel) {
        printAnnotations(classModel, true);

        p.print(classModel.getVisibility().getRepresentation());
        if (classModel.isAbstract()) {
            p.printWithNoIndent(" abstract");
        }

        p.printWithNoIndent(" ", classModel.getKind().getRepresentation(), " ", classModel.getName());

        if (classModel.getSuperClass() != null) {
            assert classModel.getKind() != ClassKind.INTERFACE : "Interfaces can't have superclasses: " + classModel.getName();
            p.printWithNoIndent(" extends ", renderType(classModel.getSuperClass()));
        }

        if (!classModel.getSuperInterfaces().isEmpty()) {
            if (classModel.getKind() == ClassKind.CLASS) {
                p.printWithNoIndent(" implements ");
            }
            else {
                p.printWithNoIndent(" extends ");
            }
            for (Iterator<TypeData> iterator = classModel.getSuperInterfaces().iterator(); iterator.hasNext(); ) {
                TypeData typeData = iterator.next();
                p.printWithNoIndent(renderType(typeData));
                if (iterator.hasNext()) {
                    p.printWithNoIndent(", ");
                }
            }
        }

        p.printlnWithNoIndent(" {");
        p.pushIndent();

        for (FieldModel fieldModel : classModel.getFields()) {
            printField(fieldModel, classModel.getKind() == ClassKind.INTERFACE);
        }
        p.println();

        for (MethodModel methodModel : classModel.getMethods()) {
            printMethod(methodModel, classModel.getKind() == ClassKind.INTERFACE);
            p.println();
        }

        p.popIndent();
        p.println("}");
    }

    private void printField(FieldModel model, boolean inInterface) {
        printAnnotations(model, true);
        if (!inInterface) {
            p.print(model.getVisibility().getRepresentation());
            if (model.isFinal()) {
                p.printWithNoIndent(" final");
            }
        }
        else {
            p.print();
        }
        p.printlnWithNoIndent(" ", renderType(model.getType()), " ", model.getName(), ";");
    }

    private void printMethod(MethodModel model, boolean inInterface) {
        printAnnotations(model, true);
        if (!inInterface) {
            p.print(model.getVisibility().getRepresentation(), " ");
            if (model.isAbstract()) {
                p.printWithNoIndent("abstract ");
            }
        }
        else {
            p.print();
        }
        p.printWithNoIndent(renderType(model.getReturnType()), " ", model.getName(), "(");
        for (Iterator<ParameterModel> iterator = model.getParameters().iterator(); iterator.hasNext(); ) {
            ParameterModel parameterModel = iterator.next();
            printParameter(parameterModel);
            if (iterator.hasNext()) {
                p.printWithNoIndent(", ");
            }
        }
        if (model.isAbstract()) {
            p.printlnWithNoIndent(");");
        }
        else {
            p.printlnWithNoIndent(") {");
            p.pushIndent();

            PieceOfCode methodBody = model.getData(METHOD_BODY);
            if (methodBody != null) {
                methodBody.create(new CodePrinter()).print(p);
            }
            else {
                throw new IllegalStateException("No body for method " + model.getName());
            }

            p.popIndent();
            p.println("}");
        }
    }

    private void printParameter(ParameterModel model) {
        printAnnotations(model, false);

        p.printWithNoIndent(renderType(model.getType()), " ", model.getName());
    }

    private String renderType(TypeData type) {
        type.create(new TypeFactory<Runnable>() {
            @Override
            public Runnable constructedType(@NotNull final String packageName, @NotNull final String className, @NotNull List<Runnable> arguments) {
                return new Runnable() {
                    @Override
                    public void run() {
                        importedTypes.add(Pair.create(packageName, className));
                    }
                };
            }

            @Override
            public Runnable wildcardType(@NotNull WildcardKind kind, @Nullable Runnable bound) {
                return new Runnable() {
                    @Override
                    public void run() {

                    }
                };
            }
        }).run();
        return type.create(new TypeFactory<String>() {
            @Override
            public String constructedType(@NotNull String packageName, @NotNull String className, @NotNull List<String> arguments) {
                StringBuilder sb = new StringBuilder();
                sb.append(className);
                if (!arguments.isEmpty()) {
                    sb.append("<");
                    for (Iterator<String> iterator = arguments.iterator(); iterator.hasNext(); ) {
                        String argument = iterator.next();
                        sb.append(argument);
                        if (iterator.hasNext()) {
                            sb.append(", ");
                        }
                    }
                    sb.append(">");
                }
                return sb.toString();
            }

            @Override
            public String wildcardType(@NotNull WildcardKind kind, @Nullable String bound) {
                StringBuilder sb = new StringBuilder("?");
                switch (kind) {
                    case BARE:
                        assert bound == null;
                        break;
                    case EXTENDS:
                        sb.append(" extends ").append(bound);
                        break;
                    case SUPER:
                        sb.append(" super ").append(bound);
                        break;
                }
                return sb.toString();
            }
        });
    }

    private void printAnnotations(AnnotatedModel annotatedModel, boolean eachOnANewLine) {
        for (TypeData annotationType : annotatedModel.getAnnotations()) {
            if (eachOnANewLine) {
                p.println("@", renderType(annotationType));
            }
            else {
                p.printWithNoIndent("@", renderType(annotationType), " ");
            }
        }
    }
}
