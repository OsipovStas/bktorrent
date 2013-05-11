package packets;

import book.Chapter;
import book.ChapterHeader;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

/**
 * @author Osipov Stanislav
 */
public interface Packet extends Serializable {

    PacketType getQueryType();

    List<ChapterHeader> getContentList();

    List<ChapterHeader> getBookRequest();

    Chapter getChapter();

    ChapterHeader getChapterHeader();

    byte[] toByteArray() throws IOException;

}
