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
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.FieldModel;
import org.jetbrains.jet.lang.descriptors.builders.generator.java.declarations.MethodModel;
import org.jetbrains.jet.utils.Printer;

import java.util.Iterator;
import java.util.List;

/**
 * @author abreslav
 */
public class Code {

    public enum BinaryOperation {
        PLUS("+"),
        MINUS("-"),
        TIMES("*"),
        DIV("/");

        private final String representation;

        private BinaryOperation(String representation) {
            this.representation = representation;
        }

        public String getRepresentation() {
            return representation;
        }
    }

    public interface CodeFactory<E> {
        @NotNull
        E statement(@NotNull E expression);

        @NotNull
        E fieldReference(@Nullable E receiver, @NotNull FieldModel field);

        @NotNull
        E methodCallReference(@Nullable E receiver, @NotNull MethodModel method, @NotNull List<E> arguments);

        @NotNull
        E assignment(@NotNull E lhs, @NotNull E rhs);

        @NotNull
        E _return(@Nullable E subj);

        @NotNull
        E string(@NotNull String s);

        @NotNull
        E integer(int i);

        @NotNull
        E binary(@NotNull E lhs, @NotNull BinaryOperation op, @NotNull E rhs);
    }

    public interface PieceOfCode {
        @NotNull
        <E> E create(@NotNull CodeFactory<E> f);
    }

    public interface PrintAction {
        void print(Printer p);
    }

    public static class CodePrinter implements CodeFactory<PrintAction> {
        private final Printer p;

        public CodePrinter(Printer p) {
            this.p = p;
        }

        @Override
        public PrintAction statement(@NotNull final PrintAction expression) {
            return new PrintAction() {
                @Override
                public void print(Printer p) {
                    p.print();
                    expression.print(p);
                    p.printlnWithNoIndent();
                }
            };
        }

        @Override
        public PrintAction fieldReference(@Nullable final PrintAction receiver, @NotNull final FieldModel field) {
            return new PrintAction() {
                @Override
                public void print(Printer p) {
                    if (receiver != null) {
                        receiver.print(p);
                    }
                    p.printWithNoIndent(".", field.getName());
                }
            };
        }

        @Override
        public PrintAction methodCallReference(
                @Nullable final PrintAction receiver,
                @NotNull final MethodModel method,
                @NotNull final List<PrintAction> arguments
        ) {
            return new PrintAction() {
                @Override
                public void print(Printer p) {
                    if (receiver != null) {
                        receiver.print(p);
                    }
                    p.printWithNoIndent(".", method.getName(), "(");
                    for (Iterator<PrintAction> iterator = arguments.iterator(); iterator.hasNext(); ) {
                        PrintAction argument = iterator.next();
                        argument.print(p);
                        if (iterator.hasNext()) {
                            p.printWithNoIndent(", ");
                        }
                    }
                }
            };
        }

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

        @Override
        public PrintAction string(@NotNull final String s) {
            return new PrintAction() {
                @Override
                public void print(Printer p) {
                    p.printWithNoIndent("\"", s, "\"");
                }
            };
        }

        @Override
        public PrintAction integer(final int i) {
            return new PrintAction() {
                @Override
                public void print(Printer p) {
                    p.printWithNoIndent(i);
                }
            };
        }

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
    }

}
