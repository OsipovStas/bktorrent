package book;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This class is used to generate random book instance and torrent file for it
 *
 * @author Osipov Stanislav
 */
public final class BookGenerator {

    public static long idCounter = 0;

    /**
     * Main method
     *
     * @param args must be {[/output/directories]}
     */
    public static void main(String[] args) {
        for (String arg : args) {
            long bookId = ++idCounter;
            Path bookPath = Paths.get(arg, bookId + ".bk");
            Path torrentPath = Paths.get(arg, bookId + ".tr");
            Book book = Book.generateRandomBook(bookId);
            try {
                book.dump(bookPath.toFile());
                book.getHeader().dump(torrentPath.toFile().getAbsolutePath());
                Book.read(bookPath.toFile());
                BookHeader.read(torrentPath.toFile().getAbsolutePath());
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private BookGenerator() {
    }

}
