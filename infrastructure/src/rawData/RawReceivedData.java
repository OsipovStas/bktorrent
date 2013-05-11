package rawData;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import packets.Packet;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Osipov Stanislav
 */
public final class RawReceivedData {

    @Nullable
    private RawMessage rawMessage = null;
    @NotNull
    private ByteBuffer dataBuffer = ByteBuffer.allocate(8064);

    public List<Packet> makeMessages() {
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

    public void addData(byte[] readData) {
        dataBuffer.put(readData);
    }
}
