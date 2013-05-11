package book;

import com.sun.istack.internal.NotNull;

import java.io.*;

/**
 * @author Osipov Stanislav
 */
public final class BookHeader implements Serializable {

    private long id;
    private int chapterAmount;

    public BookHeader(long id, int chapterAmount) {
        this.id = id;
        this.chapterAmount = chapterAmount;
    }

    public long getId() {
        return id;
    }

    public int getChapterAmount() {
        return chapterAmount;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookHeader)) return false;

        BookHeader that = (BookHeader) o;

        if (chapterAmount != that.chapterAmount) return false;
        if (id != that.id) return false;

        return true;
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

    public void dump(@NotNull String filename) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(filename)))) {
            oos.writeObject(this);
        }
    }

    @NotNull
    public static BookHeader read(@NotNull String filename) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(filename)))) {
            return (BookHeader) ois.readObject();
        }
    }
}
