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

package org.jetbrains.jet.lang.descriptors.builders.generator.java.code;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.ClassModel;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.types.TypeData;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.types.TypeFactory;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.types.TypeRenderer;
import org.jetbrains.jet.utils.Printer;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
* @author abreslav
*/
public class CodePrinter implements CodeFactory<PrintAction> {

    private final TypeRenderer typeRenderer;

    public CodePrinter(@NotNull TypeRenderer typeRenderer) {
        this.typeRenderer = typeRenderer;
    }

    @NotNull
    @Override
    public PrintAction statement(@NotNull final PrintAction expression) {
        return new PrintAction() {
            @Override
            public void print(Printer p) {
                p.print();
                expression.print(p);
                p.printlnWithNoIndent(";");
            }
        };
    }

    @NotNull
    @Override
    public PrintAction block(@NotNull final List<PrintAction> block) {
        return new PrintAction() {
            @Override
            public void print(Printer p) {
                for (PrintAction action : block) {
                    action.print(p);
                }
            }
        };
    }

    @NotNull
    @Override
    public PrintAction fieldReference(@NotNull final PrintAction receiver, @NotNull final String field) {
        return new PrintAction() {
            @Override
            public void print(Printer p) {
                receiver.print(p);
                p.printWithNoIndent(".", field);
            }
        };
    }

    @NotNull
    @Override
    public PrintAction variableDeclaration(@NotNull final TypeData type, @NotNull final String name, @Nullable final PrintAction initializer) {
        return new PrintAction() {
            @Override
            public void print(Printer p) {
                p.print(typeRenderer.renderType(type), " ", name);
                if (initializer != null) {
                    p.printWithNoIndent(" = ");
                    initializer.print(p);
                }
                p.printlnWithNoIndent(";");
            }
        };
    }

    @NotNull
    @Override
    public PrintAction variableReference(@NotNull final String name) {
        return new PrintAction() {
            @Override
            public void print(Printer p) {
                p.printWithNoIndent(name);
            }
        };
    }

    @NotNull
    @Override
    public PrintAction methodCall(
            @Nullable final PrintAction receiver,
            @NotNull final String method,
            @NotNull final List<PrintAction> arguments
    ) {
        return new PrintAction() {
            @Override
            public void print(Printer p) {
                if (receiver != null) {
                    receiver.print(p);
                    p.printWithNoIndent(".");
                }
                p.printWithNoIndent(method, "(");
                for (Iterator<PrintAction> iterator = arguments.iterator(); iterator.hasNext(); ) {
                    PrintAction argument = iterator.next();
                    argument.print(p);
                    if (iterator.hasNext()) {
                        p.printWithNoIndent(", ");
                    }
                }
                p.printWithNoIndent(")");
            }
        };
    }

    @NotNull
    @Override
    public PrintAction constructorCall(
            @NotNull final ClassModel classBeingInstantiated, @NotNull final List<TypeData> typeArguments, @NotNull final List<PrintAction> arguments
    ) {
        return new PrintAction() {
            @Override
            public void print(Printer p) {
                p.printWithNoIndent("new ",
                                    typeRenderer.renderType(new TypeData() {
                                        @Override
                                        public <E> E create(@NotNull TypeFactory<E> f) {
                                            return f.constructedType(classBeingInstantiated.getPackageFqName(),
                                                                     classBeingInstantiated.getName(),
                                                                     Collections.<E>emptyList());
                                        }
                                    })
                );
                if (!typeArguments.isEmpty()) {
                    p.printWithNoIndent("<");
                    for (Iterator<TypeData> iterator = typeArguments.iterator(); iterator.hasNext(); ) {
                        TypeData argument = iterator.next();
                        p.printWithNoIndent(typeRenderer.renderType(argument));
                        if (iterator.hasNext()) {
                            p.printWithNoIndent(", ");
                        }
                    }
                    p.printWithNoIndent(">");
                }
                p.printWithNoIndent("(");
                for (Iterator<PrintAction> iterator = arguments.iterator(); iterator.hasNext(); ) {
                    PrintAction argument = iterator.next();
                    argument.print(p);
                    if (iterator.hasNext()) {
                        p.printWithNoIndent(", ");
                    }
                }
                p.printWithNoIndent(")");
            }
        };
    }

    @NotNull
    @Override
    public PrintAction assignment(@NotNull final PrintAction lhs, @NotNull final PrintAction rhs) {
        return new PrintAction() {
            @Override
            public void print(Printer p) {
                p.print();
                lhs.print(p);
                p.printWithNoIndent(" = ");
                rhs.print(p);
                p.printlnWithNoIndent(";");
            }
        };
    }

    @NotNull
    @Override
    public PrintAction _return(@Nullable final PrintAction subj) {
        return new PrintAction() {
            @Override
            public void print(Printer p) {
                p.print("return");
                if (subj != null) {
                    p.printWithNoIndent(" ");
                    subj.print(p);
                    p.printlnWithNoIndent(";");
                }
            }
        };
    }

    @NotNull
    @Override
    public PrintAction string(@NotNull final String s) {
        return new PrintAction() {
            @Override
            public void print(Printer p) {
                p.printWithNoIndent("\"", s, "\"");
            }
        };
    }

    @NotNull
    @Override
    public PrintAction integer(final int i) {
        return new PrintAction() {
            @Override
            public void print(Printer p) {
                p.printWithNoIndent(i);
            }
        };
    }

    @NotNull
    @Override
    public PrintAction binary(@NotNull final PrintAction lhs, @NotNull final BinaryOperation op, @NotNull final PrintAction rhs) {
        return new PrintAction() {
            @Override
            public void print(Printer p) {
                p.print();
                lhs.print(p);
                p.printWithNoIndent(" ", op.getRepresentation(), " ");
                rhs.print(p);
                p.printlnWithNoIndent(";");
            }
        };
    }

    @NotNull
    @Override
    public PrintAction _this() {
        return new PrintAction() {
            @Override
            public void print(Printer p) {
                p.printWithNoIndent("this");
            }
        };
    }

    @NotNull
    @Override
    public PrintAction _for(@NotNull final PrintAction variableDeclaration, @NotNull final PrintAction rangeExpression, @NotNull final PrintAction body) {
        return new PrintAction() {
            @Override
            public void print(Printer p) {
                p.print("for (");
                variableDeclaration.print(p);
                p.printWithNoIndent(" : ");
                rangeExpression.print(p);
                p.printlnWithNoIndent(") {");
                body.print(p);
                p.println("}");
            }
        };
    }
}
