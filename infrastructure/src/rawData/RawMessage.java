package rawData;

import com.sun.istack.internal.NotNull;
import packets.Packet;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;

/**
 * This class represents raw messages. It can be conversed to Packets
 *
 * @author Osipov Stanislav
 */
public final class RawMessage {
    @NotNull
    private byte[] data;
    private int count;


    /**
     * Creates raw message with size size
     *
     * @param size size of raw message
     */
    public RawMessage(int size) {
        this.data = new byte[size];
    }

    /**
     * Add data from byte buffer to raw message
     *
     * @param dataBuffer with data
     */
    public void addData(@NotNull ByteBuffer dataBuffer) {
        int length = Math.min(dataBuffer.remaining(), data.length - count);
        dataBuffer.get(data, count, length);
        count += length;
    }

    /**
     * Checks is all the data received to construct Packet
     *
     * @return true if message completely read
     */
    public boolean isMade() {
        return count == data.length;
    }


    /**
     * Construct Packet from raw data
     *
     * @return packet
     */
    @NotNull
    public Packet getReadyMessage() throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new ByteArrayInputStream(data)))) {
            return (Packet) ois.readObject();
        }
    }

}
