package book;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Osipov Stanislav
 */
public final class BookGenerator {

    public static long idCounter = 0;

    public static void main(String[] args) {
        for (String arg : args) {
            long bookId = ++idCounter;
            Path bookPath = Paths.get(arg, bookId + ".bk");
            Path torrentPath = Paths.get(arg, bookId + ".tr");
            Book book = Book.generateRandomBook(bookId);
            try {
                book.dump(bookPath.toFile());
                book.getHeader().dump(torrentPath.toFile().getAbsolutePath());
                Book b2 = Book.read(bookPath.toFile());
                BookHeader head = BookHeader.read(torrentPath.toFile().getAbsolutePath());
                int a = 9;
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
