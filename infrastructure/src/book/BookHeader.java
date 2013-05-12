package book;

import com.sun.istack.internal.NotNull;

import java.io.*;

/**
 * Objects of this class are used as torrent files
 *
 * @author Osipov Stanislav
 */
public final class BookHeader implements Serializable {

    private long id;
    private int chapterAmount;

    /**
     * Creates BookHeader
     *
     * @param id            book id
     * @param chapterAmount chapter amount in full book
     */
    public BookHeader(long id, int chapterAmount) {
        this.id = id;
        this.chapterAmount = chapterAmount;
    }

    /**
     * Id getter
     *
     * @return id
     */
    public long getId() {
        return id;
    }

    /**
     * Chapter amount getter
     *
     * @return chapter amount
     */
    public int getChapterAmount() {
        return chapterAmount;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookHeader)) return false;

        BookHeader that = (BookHeader) o;

        return chapterAmount == that.chapterAmount && id == that.id;

    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + chapterAmount;
        return result;
    }

    @Override
    public String toString() {
        return "BookHeader{" +
                "id=" + id +
                ", chapterAmount=" + chapterAmount +
                '}';
    }

    /**
     * Serializes book to file
     *
     * @param filename file name
     * @throws IOException
     */
    public void dump(@NotNull String filename) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(filename)))) {
            oos.writeObject(this);
        }
    }

    /**
     * DeSerializes book
     *
     * @param filename file name
     * @return book
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @NotNull
    public static BookHeader read(@NotNull String filename) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(filename)))) {
            return (BookHeader) ois.readObject();
        }
    }
}
