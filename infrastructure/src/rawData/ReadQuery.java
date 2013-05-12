package rawData;

import com.sun.istack.internal.NotNull;

import java.nio.channels.SocketChannel;

/**
 * Bytes which reader gets from socket channel
 *
 * @author Osipov Stanislav
 */
public final class ReadQuery {
    @NotNull
    private SocketChannel source;
    @NotNull
    private byte[] readData;

    /**
     * Creates ReadQuery
     *
     * @param source   source
     * @param readData data
     */
    public ReadQuery(@NotNull SocketChannel source, @NotNull byte[] readData) {
        this.source = source;
        this.readData = readData;
    }

    /**
     * Source getter
     *
     * @return source
     */
    @NotNull
    public SocketChannel getSource() {
        return source;
    }

    /**
     * Data getter
     *
     * @return data
     */
    @NotNull
    public byte[] getReadData() {
        return readData;
    }

}
