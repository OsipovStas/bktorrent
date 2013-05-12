package packets;

import book.ChapterHeader;
import com.sun.istack.internal.NotNull;

/**
 * Chapter Request Packet
 *
 * @author Osipov Stanislav
 */
public final class ChapterRequestPacket extends PacketAdapter {

    @NotNull
    private final PacketType type = PacketType.CHAPTER_REQUEST;
    @NotNull
    private final ChapterHeader chapterHeader;


    @NotNull
    @Override
    public PacketType getPacketType() {
        return type;
    }

    @NotNull
    @Override
    public ChapterHeader getChapterHeader() {
        return chapterHeader;
    }

    public ChapterRequestPacket(@NotNull ChapterHeader chapterHeader) {
        this.chapterHeader = chapterHeader;
    }
}
