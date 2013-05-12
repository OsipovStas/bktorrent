package packets;

import book.Chapter;
import com.sun.istack.internal.NotNull;

/**
 * Packet with chapter
 *
 * @author Osipov Stanislav
 */
public final class ChapterPacket extends PacketAdapter {

    @NotNull
    private final PacketType type = PacketType.CHAPTER;
    @NotNull
    private final Chapter chapter;

    @Override
    public PacketType getPacketType() {
        return type;
    }

    @NotNull
    @Override
    public Chapter getChapter() {
        return chapter;
    }

    public ChapterPacket(@NotNull Chapter chapter) {
        this.chapter = chapter;
    }


}
