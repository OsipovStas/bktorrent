package packets;

import book.ChapterHeader;

/**
 * @author Osipov Stanislav
 */
public final class ChapterRequestPacket extends PacketAdapter {

    private final PacketType type = PacketType.CHAPTER_REQUEST;
    private final ChapterHeader chapterHeader;


    @Override
    public PacketType getQueryType() {
        return type;
    }

    @Override
    public ChapterHeader getChapterHeader() {
        return chapterHeader;
    }

    public ChapterRequestPacket(ChapterHeader chapterHeader) {
        this.chapterHeader = chapterHeader;
    }
}
