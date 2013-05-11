package packets;

import book.Chapter;

/**
 * @author Osipov Stanislav
 */
public final class ChapterPacket extends PacketAdapter {

    private final PacketType type = PacketType.CHAPTER;
    private final Chapter chapter;

    @Override
    public PacketType getQueryType() {
        return type;
    }

    @Override
    public Chapter getChapter() {
        return chapter;
    }

    public ChapterPacket(Chapter chapter) {
        this.chapter = chapter;
    }


}
