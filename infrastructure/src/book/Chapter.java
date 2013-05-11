package book;

import com.sun.istack.internal.NotNull;

import java.io.Serializable;

/**
 * @author Osipov Stanislav
 */
public final class Chapter implements Serializable {

    @NotNull
    private ChapterHeader header;
    @NotNull
    private byte[] data;

    public Chapter(@NotNull ChapterHeader header, @NotNull byte[] data) {
        this.header = header;
        this.data = data;
    }

    @NotNull
    public ChapterHeader getHeader() {
        return header;
    }

    @NotNull
    public byte[] getData() {
        return data;
    }
}
