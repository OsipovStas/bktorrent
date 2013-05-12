package rawData;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import packets.Packet;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * This class represents the sequence of data read from the socket
 *
 * @author Osipov Stanislav
 */
public final class RawReceivedData {

    @Nullable
    private RawMessage rawMessage = null;
    @NotNull
    private ByteBuffer dataBuffer = ByteBuffer.allocate(8064);

    /**
     * Creates list of fully read packets
     *
     * @return list of packets
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @NotNull
    public List<Packet> makeMessages() throws IOException, ClassNotFoundException {
        List<Packet> messages = new ArrayList<>();
        dataBuffer.flip();
        while (dataBuffer.remaining() > 0) {
            if (rawMessage == null) {
                if (dataBuffer.remaining() < 4) {
                    break;
                }
                rawMessage = new RawMessage(dataBuffer.getInt());
            }
            rawMessage.addData(dataBuffer);
            if (rawMessage.isMade()) {
                messages.add(rawMessage.getReadyMessage());
                rawMessage = null;
            }
        }
        dataBuffer.compact();
        return messages;
    }

    /**
     * Adds data sequence
     *
     * @param readData data from socket
     */
    public void addData(@NotNull byte[] readData) {
        dataBuffer.put(readData);
    }
}
