package com.duo.tv.ui.adapter;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import com.duo.tv.impl.Diffable;

public class BaseItemCallback<T extends Diffable<T>> extends DiffUtil.ItemCallback<T> {

    @Override
    public boolean areItemsTheSame(@NonNull T oldItem, @NonNull T newItem) {
        return oldItem.isSameItem(newItem);
    }

    @Override
    public boolean areContentsTheSame(@NonNull T oldItem, @NonNull T newItem) {
        return oldItem.isSameContent(newItem);
    }
}