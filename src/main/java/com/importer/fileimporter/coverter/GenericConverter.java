package com.importer.fileimporter.coverter;

import java.util.List;

public interface GenericConverter<D, E> {

    D createFrom(E e);

    E createTo(D d);

    List<D> createFromEntities(List<E> es);
    List<E> createToEntities(List<D> ds);
}
