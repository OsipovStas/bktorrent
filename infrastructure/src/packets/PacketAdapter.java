package packets;

import book.Chapter;
import book.ChapterHeader;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;

/**
 * Adapter for Packet
 *
 * @author Osipov Stanislav
 */
public abstract class PacketAdapter implements Packet {


    /**
     * Book request list getter
     *
     * @return book request list
     */
    @Override
    public List<ChapterHeader> getBookRequest() {
        return null;
    }

    /**
     * Chapter getter
     *
     * @return chapter
     */
    @Override
    public Chapter getChapter() {
        return null;
    }

    /**
     * Content list getter
     *
     * @return content list
     */
    @Override
    public List<ChapterHeader> getContentList() {
        return null;
    }

    /**
     * Chapter header getter
     *
     * @return chapter header
     */
    @Override
    public ChapterHeader getChapterHeader() {
        return null;
    }

    /**
     * Converts to byte array
     *
     * @return bytes sequence which are represent this object
     * @throws IOException
     */
    @Override
    public byte[] toByteArray() throws IOException {
        byte[] data;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(baos))) {
            oos.writeObject(this);
            oos.flush();
            data = baos.toByteArray();
        }
        return data;
    }
}
