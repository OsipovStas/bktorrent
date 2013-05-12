package packets;

import book.Chapter;
import book.ChapterHeader;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

/**
 * This interface represent packets which server communicates with clients
 *
 * @author Osipov Stanislav
 */
public interface Packet extends Serializable {

    /**
     * Packet type getter
     *
     * @return Packet type
     */
    @NotNull
    PacketType getPacketType();

    /**
     * Content list getter
     *
     * @return content list
     */
    @Nullable
    List<ChapterHeader> getContentList();

    /**
     * Book request list getter
     *
     * @return book request list
     */
    @Nullable
    List<ChapterHeader> getBookRequest();

    /**
     * Chapter getter
     *
     * @return chapter
     */
    @Nullable
    Chapter getChapter();

    /**
     * Chapter header getter
     *
     * @return chapter header
     */
    @Nullable
    ChapterHeader getChapterHeader();

    /**
     * Converts to byte array
     *
     * @return bytes sequence which are represent this object
     * @throws IOException
     */
    @NotNull
    byte[] toByteArray() throws IOException;

}
