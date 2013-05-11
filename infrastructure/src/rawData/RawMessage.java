package rawData;

import com.sun.istack.internal.Nullable;
import packets.Packet;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;

/**
 * @author Osipov Stanislav
 */
public final class RawMessage {
    private byte[] data;
    private int count;

    public RawMessage(int size) {
        this.data = new byte[size];
    }

    public void addData(ByteBuffer dataBuffer) {
        int length = Math.min(dataBuffer.remaining(), data.length - count);
        dataBuffer.get(data, count, length);
        count += length;
    }

    public boolean isMade() {
        return count == data.length;
    }

    @Nullable
    public Packet getReadyMessage() {
        try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new ByteArrayInputStream(data)))) {
            return (Packet) ois.readObject();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return null;
    }

}
