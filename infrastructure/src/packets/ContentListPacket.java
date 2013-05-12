package packets;

import book.ChapterHeader;
import com.sun.istack.internal.NotNull;

import java.util.List;

/**
 * Packet with content list
 *
 * @author Osipov Stanislav
 */
public final class ContentListPacket extends PacketAdapter {

    @NotNull
    private final PacketType queryType = PacketType.CONTENT_LIST;

    @NotNull
    private List<ChapterHeader> contentList;

    public ContentListPacket(@NotNull List<ChapterHeader> contentList) {
        this.contentList = contentList;
    }

    @NotNull
    @Override
    public PacketType getPacketType() {
        return queryType;
    }

    @NotNull
    @Override
    public List<ChapterHeader> getContentList() {
        return contentList;
    }
}
