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

import gnu.trove.THashSet;
import gnu.trove.TObjectHashingStrategy;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.*;
import org.jetbrains.jet.utils.Printer;

import java.util.Iterator;
import java.util.Set;

/**
 * @author abreslav
 */
public class ClassPrinter {



    public static void printClass(ClassModel classModel, Printer p) {
        if (!classModel.getPackageFqName().isEmpty()) {
            p.println("package ", classModel.getPackageFqName(), ";");
        }
        ClassPrinter classPrinter = new ClassPrinter();
        classPrinter.printClass(classModel);

        p.println();
        for (TypeModel importedType : classPrinter.importedTypes) {
            String packageFqName = importedType.getPackageFqName();
            if (!packageFqName.isEmpty()
                    && !packageFqName.equals("java.lang")
                    && !packageFqName.equals(classModel.getPackageFqName())) {
                p.println("import ", packageFqName, ".", importedType.getClassName(), ";");
            }
        }
        p.println();

        p.printWithNoIndent(classPrinter.body);
    }

    private final Printer p;
    private Set<TypeModel> importedTypes = new THashSet<TypeModel>(new TObjectHashingStrategy<TypeModel>() {
        @Override
        public int computeHashCode(TypeModel model) {
            return model.getPackageFqName().hashCode() + 13 * model.getClassName().hashCode();
        }

        @Override
        public boolean equals(TypeModel model, TypeModel model1) {
            return model.getPackageFqName().equals(model1.getPackageFqName())
                   && model.getClassName().equals(model1.getClassName());
        }
    });
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
            for (Iterator<TypeModel> iterator = classModel.getSuperInterfaces().iterator(); iterator.hasNext(); ) {
                TypeModel typeModel = iterator.next();
                p.printWithNoIndent(renderType(typeModel));
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
            p.print(model.getVisibility().getRepresentation());
            if (model.isAbstract()) {
                p.printWithNoIndent(" abstract");
            }
        }
        else {
            p.print();
        }
        p.printWithNoIndent(" ", renderType(model.getReturnType()), " ", model.getName(), "(");
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
            // Body
            p.popIndent();
            p.println("}");
        }
    }

    private void printParameter(ParameterModel model) {
        printAnnotations(model, false);

        p.printWithNoIndent(renderType(model.getType()), " ", model.getName());
    }

    private String renderType(TypeModel type) {
        StringBuilder sb = new StringBuilder();
        doRenderType(type, sb);
        return sb.toString();
    }

    private void doRenderType(TypeModel type, StringBuilder sb) {
        importedTypes.add(type);
        sb.append(type.getClassName());
        if (!type.getArguments().isEmpty()) {
            sb.append("<");
            for (Iterator<TypeModel> iterator = type.getArguments().iterator(); iterator.hasNext(); ) {
                TypeModel arg = iterator.next();
                doRenderType(arg, sb);
                if (iterator.hasNext()) {
                    sb.append(", ");
                }
            }
            sb.append(">");
        }
    }

    private void printAnnotations(AnnotatedModel annotatedModel, boolean eachOnANewLine) {
        for (TypeModel annotationType : annotatedModel.getAnnotations()) {
            p.print("@", renderType(annotationType));
            if (eachOnANewLine) {
                p.printlnWithNoIndent();
            }
            else {
                p.printWithNoIndent(" ");
            }
        }
    }
}
