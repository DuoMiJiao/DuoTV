package androidx.media3.common;

/**
 * Minimal stand-in for the media3 fork's MediaTitle. The public media3
 * release has no multi-edition API, so the app builds against this shim and
 * the title picker degrades to "no titles available".
 */
public final class MediaTitle {

    public final String label;
    public final long durationUs;
    public final int index;
    public boolean selected;

    public MediaTitle(String label, long durationUs, int index, boolean selected) {
        this.label = label;
        this.durationUs = durationUs;
        this.index = index;
        this.selected = selected;
    }
}
