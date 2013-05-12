package book;

import com.sun.istack.internal.NotNull;

import java.io.Serializable;

/**
 * This class represents book chapter
 *
 * @author Osipov Stanislav
 */
public final class Chapter implements Serializable {

    @NotNull
    private ChapterHeader header;
    @NotNull
    private byte[] data;

    /**
     * Creates new chapter
     *
     * @param header chapter header
     * @param data   data
     */
    public Chapter(@NotNull ChapterHeader header, @NotNull byte[] data) {
        this.header = header;
        this.data = data;
    }

    /**
     * Chapter header getter
     *
     * @return chapter header
     */
    @NotNull
    public ChapterHeader getHeader() {
        return header;
    }

    /**
     * Data getter
     *
     * @return chapter data
     */
    @NotNull
    public byte[] getData() {
        return data;
    }
}
