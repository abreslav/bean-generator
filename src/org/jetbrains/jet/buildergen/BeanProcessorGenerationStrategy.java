package org.jetbrains.jet.buildergen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.buildergen.entities.Entity;
import org.jetbrains.jet.buildergen.entities.Relation;
import org.jetbrains.jet.buildergen.java.code.CodeFactory;
import org.jetbrains.jet.buildergen.java.declarations.ClassModel;
import org.jetbrains.jet.buildergen.java.types.TypeData;

import java.util.List;

interface BeanProcessorGenerationStrategy {
    @NotNull
    TypeData getInType(@NotNull Entity entity);

    @NotNull
    TypeData getOutType(@NotNull Entity entity);

    @NotNull
    TypeData getInType(@NotNull Relation<?> relation);

    @NotNull
    TypeData getOutType(@NotNull Relation<?> relation);

    void defineContextFields(@NotNull ClassModel generatorClass);
    void defineAdditionalMethods(@NotNull ClassModel generatorClass);

    <E> E expressionToAssignToOut(@NotNull CodeFactory<E> f, @NotNull Entity entity);

    <E> void traceMethodBody(@NotNull CodeFactory<E> f, @NotNull Entity entity);

    <E> void assignRelation(
            @NotNull CodeFactory<E> f,
            @NotNull Entity out,
            @NotNull Relation<?> relation,
            E outExpression,
            E convertedInExpression,
            @NotNull List<E> statements
    );

    <E> void convertDataRelationMethodBody(@NotNull Relation<?> relation, @NotNull List<E> statements);
    <E> void convertEntityRelationMethodBody(@NotNull Relation<?> relation, @NotNull Entity target, @NotNull List<E> statements);
    <E> void convertReferenceRelationMethodBody(@NotNull Relation<?> relation, @NotNull Entity target, @NotNull List<E> statements);
}
