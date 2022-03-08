package com.fiill.fiillplayer.application;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * Option is data for ikjplayer.setOption();
 */

public class Option implements Serializable, Cloneable {
    private final int mCategory;
    private final String mName;
    private final Object mValue;

    private Option(int category, String name, Object value) {
        this.mCategory = category;
        this.mName = name;
        this.mValue = value;
    }

    public static Option create(int category, String name, String value) {
        return new Option(category, name, value);
    }

    public static Option create(int category, String name, Long value) {
        return new Option(category, name, value);
    }

    public int getCategory() {
        return mCategory;
    }

    public String getName() {
        return mName;
    }

    public Object getValue() {
        return mValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Option option = (Option) o;

        if (mCategory != option.mCategory) return false;
        if (!Objects.equals(mName, option.mName)) return false;
        return Objects.equals(mValue, option.mValue);

    }

    @Override
    public int hashCode() {
        int result = mCategory;
        result = 31 * result + (mName != null ? mName.hashCode() : 0);
        result = 31 * result + (mValue != null ? mValue.hashCode() : 0);
        return result;
    }

    @NonNull
    @Override
    public Option clone() throws CloneNotSupportedException {
        return (Option) super.clone();
    }

    /**
     * preset for realtime stream
     * @return the setting options
     */
    public static List<Option> presetForRealtime() {
        List<Option> options = new ArrayList<>();
        options.add(create(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 0L));
        options.add(create(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "http-detect-range-support", 0L));
        options.add(create(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 8L));


        options.add(create(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzemaxduration", 100L));
        options.add(create(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "probesize", 1024L));
        options.add(create(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "flush_packets", 1L));
        options.add(create(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "packet-buffering", 0L));
        options.add(create(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "framedrop", 1L));
        return options;
    }
}
