package packets;

import book.ChapterHeader;
import com.sun.istack.internal.NotNull;

import java.util.List;

/**
 * Packet with book request
 *
 * @author Osipov Stanislav
 */
public final class BookRequestPacket extends PacketAdapter {

    @NotNull
    private PacketType type = PacketType.BOOK_REQUEST;
    @NotNull
    private List<ChapterHeader> bookRequest;

    @Override
    public PacketType getPacketType() {
        return type;
    }

    @Override
    public List<ChapterHeader> getBookRequest() {
        return bookRequest;
    }

    public BookRequestPacket(@NotNull List<ChapterHeader> bookRequest) {
        this.bookRequest = bookRequest;
    }
}
