package book;

import com.sun.istack.internal.NotNull;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * This class represents books which peers transfers through the network
 *
 * @author Osipov Stanislav
 */
public final class Book implements Serializable {


    @NotNull
    private BookHeader header;
    @NotNull
    private ConcurrentMap<ChapterHeader, Chapter> chapters;
    @NotNull
    private static Random rng = new Random();

    /**
     * Creates new empty book
     *
     * @param bookHeader book header
     */
    public Book(@NotNull BookHeader bookHeader) {
        this.header = bookHeader;
        this.chapters = new ConcurrentHashMap<>();
    }


    /**
     * Creates new book with chapters
     *
     * @param header   book header
     * @param chapters chapters
     */
    public Book(@NotNull BookHeader header, @NotNull ConcurrentMap<ChapterHeader, Chapter> chapters) {
        this.header = header;
        this.chapters = chapters;
    }

    /**
     * getter for header
     *
     * @return header
     */
    @NotNull
    public BookHeader getHeader() {
        return header;
    }

    /**
     * Chapter getter
     *
     * @return chapters
     */
    @NotNull
    public Map<ChapterHeader, Chapter> getChapters() {
        return chapters;
    }

    /**
     * Content list getter
     *
     * @return content list
     */
    @NotNull
    public List<? extends ChapterHeader> getContentList() {
        return new ArrayList<>(chapters.keySet());
    }

    /**
     * Generates random book with given bookid
     *
     * @param bookId bookid
     * @return random book
     */
    @NotNull
    public static Book generateRandomBook(long bookId) {
        int chapterAmount = 65;
        BookHeader bookHeader = new BookHeader(bookId, chapterAmount);
        ConcurrentMap<ChapterHeader, Chapter> chapters = new ConcurrentHashMap<>();
        for (int i = 0; i < chapterAmount; ++i) {
            ChapterHeader chapterHeader = new ChapterHeader(bookId, i);
            byte[] chapterData = new byte[rng.nextInt(2048) + 51200];
            Arrays.fill(chapterData, (byte) i);
            Chapter chapter = new Chapter(chapterHeader, chapterData);
            chapters.put(chapterHeader, chapter);
        }
        return new Book(bookHeader, chapters);
    }

    /**
     * Serializes book to a file
     *
     * @param filename file to serialize
     * @throws IOException
     */
    public void dump(@NotNull File filename) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(filename)))) {
            oos.writeObject(this);
        }
    }

    /**
     * DeSerializes book from a file
     *
     * @param filename file
     * @return book
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @NotNull
    public static Book read(@NotNull File filename) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(filename)))) {
            return (Book) ois.readObject();
        }
    }
}
