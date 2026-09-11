package com.duo.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.duo.tv.R;
import com.duo.tv.api.config.VodConfig;
import com.duo.tv.bean.Site;
import com.duo.tv.databinding.AdapterSiteBinding;

import java.util.ArrayList;
import java.util.List;

public class SiteAdapter extends RecyclerView.Adapter<SiteAdapter.ViewHolder> {

    private final OnClickListener listener;
    private final List<Site> mAll;
    private final List<Site> mItems;
    private boolean search;
    private boolean change;

    public SiteAdapter(OnClickListener listener) {
        this.listener = listener;
        this.mAll = new ArrayList<>();
        this.mItems = new ArrayList<>();
        addAll();
    }

    public interface OnClickListener {

        void onTextClick(Site item);

        void onSearchClick(int position, Site item);

        void onChangeClick(int position, Site item);

        boolean onSearchLongClick(Site item);

        boolean onChangeLongClick(Site item);
    }

    public SiteAdapter search(boolean search) {
        this.search = search;
        return this;
    }

    public SiteAdapter change(boolean change) {
        this.change = change;
        return this;
    }

    private void addAll() {
        for (Site site : VodConfig.get().getSites()) if (!site.isHide()) mAll.add(site);
        mItems.addAll(mAll);
    }

    /** Filters the site list by name or key; empty query restores all. */
    public void filter(String query) {
        query = query == null ? "" : query.trim().toLowerCase();
        mItems.clear();
        for (Site site : mAll) {
            if (query.isEmpty() || site.getName().toLowerCase().contains(query) || site.getKey().toLowerCase().contains(query)) mItems.add(site);
        }
        notifyDataSetChanged();
    }

    public List<Site> getItems() {
        return mItems;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterSiteBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Site item = mItems.get(position);
        boolean on = !search || change;
        holder.binding.text.setText(item.getName());
        holder.binding.text.setEnabled(on);
        holder.binding.text.setFocusable(on);
        holder.binding.text.setSelected(on && item.isActivated());
        holder.binding.text.setActivated(on && item.isActivated());
        holder.binding.search.setImageResource(getSearchIcon(item));
        holder.binding.change.setImageResource(getChangeIcon(item));
        holder.binding.search.setVisibility(search ? View.VISIBLE : View.GONE);
        holder.binding.change.setVisibility(change ? View.VISIBLE : View.GONE);
        holder.binding.text.setOnClickListener(v -> listener.onTextClick(item));
        holder.binding.search.setOnClickListener(v -> listener.onSearchClick(position, item));
        holder.binding.change.setOnClickListener(v -> listener.onChangeClick(position, item));
        holder.binding.search.setOnLongClickListener(v -> listener.onSearchLongClick(item));
        holder.binding.change.setOnLongClickListener(v -> listener.onChangeLongClick(item));
    }

    private int getSearchIcon(Site item) {
        return item.isSearchable() ? R.drawable.ic_site_search : R.drawable.ic_site_block;
    }

    private int getChangeIcon(Site item) {
        return item.isChangeable() ? R.drawable.ic_site_change : R.drawable.ic_site_block;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterSiteBinding binding;

        ViewHolder(@NonNull AdapterSiteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
