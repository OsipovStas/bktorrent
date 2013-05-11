package packets;

import book.ChapterHeader;

import java.util.List;

/**
 * @author Osipov Stanislav
 */
public final class BookRequestPacket extends PacketAdapter {

    private PacketType type = PacketType.BOOK_REQUEST;
    private List<ChapterHeader> bookRequest;

    @Override
    public PacketType getQueryType() {
        return type;
    }

    @Override
    public List<ChapterHeader> getBookRequest() {
        return bookRequest;
    }

    public BookRequestPacket(List<ChapterHeader> bookRequest) {
        this.bookRequest = bookRequest;
    }
}
