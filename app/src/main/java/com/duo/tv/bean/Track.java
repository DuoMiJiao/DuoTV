package com.duo.tv.bean;

import android.text.TextUtils;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.duo.tv.db.AppDatabase;

import java.util.Collections;
import java.util.List;

@Entity(indices = @Index(value = {"key", "type"}, unique = true))
public class Track {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private int type;
    private String key;
    private String name;
    private String format;
    private boolean selected;

    public Track(int type, String name, String format) {
        this.type = type;
        this.name = name;
        this.format = format;
    }

    public static List<Track> find(String key) {
        return TextUtils.isEmpty(key) ? Collections.emptyList() : AppDatabase.get().getTrackDao().find(key);
    }

    public static void delete(String key) {
        if (TextUtils.isEmpty(key)) return;
        AppDatabase.get().getTrackDao().delete(key);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public Track key(String key) {
        setKey(key);
        return this;
    }

    /**
     * 【修复·P0 有声音无画面】防止"取消勾选唯一条选中项"把整类轨道关掉：
     * 当本次操作是"取消选中"，且该 key 下同类型只剩这一条选中记录时，直接忽略该次切换。
     * 这样用户永远不会把某类轨道（尤其视频轨）的全部候选都取消掉。
     */
    public Track toggle() {
        if (isSelected() && isOnlySelectedOfType()) return this;
        setSelected(!isSelected());
        return this;
    }

    /** 该 key 下同类型是否只剩自己一条被选中（含本次尚未落库的情形）。 */
    private boolean isOnlySelectedOfType() {
        if (TextUtils.isEmpty(getKey())) return false;
        long selected = find(getKey()).stream().filter(t -> t.getType() == getType() && t.isSelected()).count();
        return selected <= 1;
    }

    public Track save() {
        if (TextUtils.isEmpty(getKey())) return this;
        AppDatabase.get().getTrackDao().insert(this);
        return this;
    }
}
