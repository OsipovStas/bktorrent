package packets;

import book.Chapter;
import book.ChapterHeader;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;

/**
 * @author Osipov Stanislav
 */
public abstract class PacketAdapter implements Packet {


    @Override
    public List<ChapterHeader> getContentList() {
        return null;
    }

    @Override
    public List<ChapterHeader> getBookRequest() {
        return null;
    }

    @Override
    public Chapter getChapter() {
        return null;
    }

    @Override
    public ChapterHeader getChapterHeader() {
        return null;
    }

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
