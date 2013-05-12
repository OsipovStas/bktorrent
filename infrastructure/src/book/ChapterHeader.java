package book;

import java.io.Serializable;

/**
 * This class represents chapter header
 *
 * @author Osipov Stanislav
 */
public final class ChapterHeader implements Serializable {

    private long bookId;
    private int chapterNumber;

    /**
     * Creates chapter header
     *
     * @param bookId        book which this chapter belongs
     * @param chapterNumber chapter number
     */
    public ChapterHeader(long bookId, int chapterNumber) {
        this.bookId = bookId;
        this.chapterNumber = chapterNumber;
    }

    /**
     * Book id getter
     *
     * @return book id
     */
    public long getBookId() {
        return bookId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChapterHeader)) return false;

        ChapterHeader that = (ChapterHeader) o;

        return bookId == that.bookId && chapterNumber == that.chapterNumber;

    }

    @Override
    public int hashCode() {
        int result = (int) (bookId ^ (bookId >>> 32));
        result = 31 * result + chapterNumber;
        return result;
    }

    @Override
    public String toString() {
        return "ChapterHeader{" +
                "bookId=" + bookId +
                ", chapterNumber=" + chapterNumber +
                '}';
    }
}
