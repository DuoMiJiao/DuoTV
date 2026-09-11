package com.duo.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.duo.tv.bean.Filter;
import com.duo.tv.bean.Value;
import com.duo.tv.databinding.AdapterValueBinding;
import com.duo.tv.impl.FilterCallback;

import java.util.List;

public class ValueAdapter extends RecyclerView.Adapter<ValueAdapter.ViewHolder> {

    private static final android.view.ViewOutlineProvider ROUND_OUTLINE = new android.view.ViewOutlineProvider() {
        @Override
        public void getOutline(android.view.View view, android.graphics.Outline outline) {
            android.graphics.drawable.Drawable bg = view.getBackground();
            if (bg != null) bg.getOutline(outline);
        }
    };

    private final FilterCallback listener;
    private final List<Value> mItems;
    private final String mKey;

    public ValueAdapter(FilterCallback listener, Filter filter) {
        this.listener = listener;
        this.mItems = filter.getValue();
        this.mKey = filter.getKey();
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterValueBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Value item = mItems.get(position);
        holder.binding.text.setText(item.getN());
        holder.binding.text.setActivated(item.isActivated());
        holder.binding.text.setOnClickListener(v -> onItemClick(item));
        holder.binding.text.setClipToOutline(true);
        holder.binding.text.setOutlineProvider(ROUND_OUTLINE);
    }

    private void onItemClick(Value value) {
        for (Value item : mItems) item.setActivated(value);
        notifyItemRangeChanged(0, getItemCount());
        listener.setFilter(mKey, value);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterValueBinding binding;

        ViewHolder(@NonNull AdapterValueBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}