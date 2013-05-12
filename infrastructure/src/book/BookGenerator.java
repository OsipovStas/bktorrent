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


    /**
     * Main method
     *
     * @param args must be {[/output/directories]}
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            return;
        }
        String bookDir = args[0];
        long bookId = Long.parseLong(args[1]);
        Path bookPath = Paths.get(bookDir, bookId + ".bk");
        Path torrentPath = Paths.get(bookDir, bookId + ".tr");
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

    private BookGenerator() {
    }

}
