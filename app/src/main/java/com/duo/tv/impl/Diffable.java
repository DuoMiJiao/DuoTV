package com.duo.tv.impl;

public interface Diffable<T> {

    boolean isSameItem(T other);

    boolean isSameContent(T other);
}
