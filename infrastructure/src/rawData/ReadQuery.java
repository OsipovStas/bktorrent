package rawData;

import java.nio.channels.SocketChannel;

/**
 * @author Osipov Stanislav
 */
public final class ReadQuery {
    private SocketChannel source;
    private byte[] readData;

    public ReadQuery(SocketChannel source, byte[] readData) {
        this.source = source;
        this.readData = readData;
    }

    public SocketChannel getSource() {
        return source;
    }

    public byte[] getReadData() {
        return readData;
    }

}
