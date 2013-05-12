package ru.spbau.osipov.tracker;

import book.Chapter;
import book.ChapterHeader;
import com.sun.istack.internal.NotNull;
import packets.ChapterPacket;
import packets.ChapterRequestPacket;
import packets.Packet;
import rawData.RawReceivedData;
import rawData.ReadQuery;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * This class is a simple p2p tracker for transfer Books on the network
 *
 * @author Osipov Stanislav
 */
public final class Tracker implements Runnable {

    private static final int TRACKER_TIME_LIMIT = 240;
    @NotNull
    private Logger log = Logger.getLogger(Tracker.class.getName());

    @NotNull
    private final InetAddress host;

    private final int port;

    private boolean shutdownFlag = false;

    @NotNull
    private final ServerSocketChannel serverSocketChannel;

    @NotNull
    private final Selector selector;

    @NotNull
    private final ByteBuffer readBuffer = ByteBuffer.allocate(4192);

    @NotNull
    private final BlockingQueue<ReadQuery> readQueries = new ArrayBlockingQueue<>(500);

    @NotNull
    private final BlockingQueue<Chapter> chapterPost = new ArrayBlockingQueue<>(1000);

    @NotNull
    private final ConcurrentMap<ChapterHeader, Set<SocketChannel>> chapterIndex = new ConcurrentHashMap<>();

    @NotNull
    private final ConcurrentMap<ChapterHeader, Set<SocketChannel>> chapterRequests = new ConcurrentHashMap<>();

    @NotNull
    private final Set<SocketChannel> changeRequests = new CopyOnWriteArraySet<>();

    @NotNull
    private final ConcurrentMap<SocketChannel, List<ByteBuffer>> pendingData = new ConcurrentHashMap<>();


    //Executors
    @NotNull
    private ExecutorService indexUpdater = Executors.newFixedThreadPool(3);

    @NotNull
    private ExecutorService trackerService = Executors.newCachedThreadPool();

    @NotNull
    private ScheduledExecutorService scheduleService = Executors.newScheduledThreadPool(5);


    /**
     * Initializes tracker
     *
     * @param host host address
     * @param port port number
     * @throws IOException
     */
    public Tracker(@NotNull InetAddress host, int port, @NotNull String logfile) throws IOException {
        this.host = host;
        this.port = port;
        this.serverSocketChannel = ServerSocketChannel.open();
        this.selector = SelectorProvider.provider().openSelector();
        configureLogger(logfile);
        configure();
        log.info("ru.spbau.osipov.tracker.Tracker successfully initialized");
    }

    /**
     * Shutdown tracker
     */
    public void shutdownTracker() {
        log.info("Server shutting down now...");
        shutdownFlag = true;
        selector.wakeup();
    }

    private void configureLogger(@NotNull String logfile) throws IOException {
        FileHandler fileHandler = new FileHandler(logfile, 1000000, 1, true);
        log.addHandler(fileHandler);
        fileHandler.setFormatter(new SimpleFormatter());
    }

    private void configure() throws IOException {
        try {
            serverSocketChannel.configureBlocking(false);
            InetSocketAddress inetSocketAddress = new InetSocketAddress(host, port);
            serverSocketChannel.socket().bind(inetSocketAddress);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Cannot configure connections", e);
            throw e;
        }
        scheduleServices();
    }

    private void scheduleServices() {
        scheduleService.scheduleAtFixedRate(new ChapterHarvester(), 15, 120, TimeUnit.SECONDS);
        scheduleService.scheduleAtFixedRate(new ModeChanger(), 3, 2, TimeUnit.SECONDS);
        scheduleService.scheduleAtFixedRate(new Finisher(), 5, 10, TimeUnit.SECONDS);
        scheduleService.scheduleAtFixedRate(new ReadWorker(), 700, 200, TimeUnit.MILLISECONDS);
        scheduleService.scheduleAtFixedRate(new ChapterPostServiceWorker(), 1000, 200, TimeUnit.MILLISECONDS);
        scheduleService.schedule(new Runnable() {
            @Override
            public void run() {
                shutdownTracker();
            }
        }, TRACKER_TIME_LIMIT, TimeUnit.SECONDS);
    }

    private void send(@NotNull byte[] data, @NotNull SocketChannel peer) {
        ByteBuffer dataBuffer = ByteBuffer.allocate(data.length + Integer.SIZE / 8);
        dataBuffer.putInt(data.length);
        dataBuffer.put(data);
        dataBuffer.flip();
        try {
            List<ByteBuffer> byteBuffers = pendingData.get(peer);
            if (byteBuffers != null) {
                byteBuffers.add(dataBuffer);
            }
        } catch (NullPointerException ex) {
            log.log(Level.SEVERE, "There is no more peer ", ex);
        }
    }

    /**
     * Selector thread
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        while (!shutdownFlag) {
            try {
                for (SocketChannel writable : changeRequests) {
                    SelectionKey selectionKey = writable.keyFor(selector);
                    if (selectionKey != null) {
                        selectionKey.interestOps(SelectionKey.OP_WRITE);
                    }
                }

                changeRequests.clear();

                selector.select();

                Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    keyIterator.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isAcceptable()) {
                        accept(key);
                    } else if (key.isReadable()) {
                        read(key);
                    } else if (key.isWritable()) {
                        write(key);
                    }

                }
            } catch (IOException e) {
                log.log(Level.SEVERE, "Strange I/O exception", e);
            } catch (ClassNotFoundException e) {
                log.log(Level.SEVERE, "Strange ClassNotFoundException", e);
            }
        }
        shutdown();
    }

    private void read(@NotNull SelectionKey key) throws IOException, ClassNotFoundException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        readBuffer.clear();

        int numRead;
        try {
            numRead = socketChannel.read(readBuffer);
        } catch (IOException e) {
            key.cancel();
            socketChannel.close();
            indexUpdater.submit(new RemoveSeedTask(socketChannel));
            log.log(Level.SEVERE, "Remote forcibly closed the connection", e);
            return;
        }

        if (numRead == -1) {
            key.channel().close();
            key.cancel();
            log.info("Remote " + socketChannel.toString() + " shut the socket down");
            return;
        }
        try {
            readQueries.put(new ReadQuery(socketChannel, Arrays.copyOf(readBuffer.array(), numRead)));
        } catch (InterruptedException e) {
            log.log(Level.SEVERE, "Interrupted while reading in select thread ", e);
        }
        log.log(Level.FINE, "Successfully read data from " + socketChannel.toString());

    }

    private void accept(@NotNull SelectionKey key) {
        SocketChannel socketChannel = null;
        try {
            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
            socketChannel = serverSocketChannel.accept();
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_READ);
        } catch (IOException e) {
            log.log(Level.SEVERE, "IOException while accepting channel ", e);
        }
        if (socketChannel != null) {
            pendingData.putIfAbsent(socketChannel, new CopyOnWriteArrayList<ByteBuffer>());
            log.info("SocketChannel " + socketChannel.toString() + " has accepted");
        }
    }


    private void write(@NotNull SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        List<ByteBuffer> queue = pendingData.get(socketChannel);

        while (!queue.isEmpty()) {
            ByteBuffer buf = queue.get(0);
            try {
                socketChannel.write(buf);
            } catch (IOException e) {
                key.cancel();
                socketChannel.close();
                log.log(Level.SEVERE, "Remote forcibly close socket channel during write ", e);
                return;
            }
            if (buf.remaining() > 0) {
                break;
            }
            queue.remove(0);
        }

        if (queue.isEmpty()) {
            key.interestOps(SelectionKey.OP_READ);
        }
    }

    private void shutdown() {
        shutdownExecutors();
        closeConnections();
    }

    private void closeConnections() {
        for (SocketChannel socketChannel : pendingData.keySet()) {
            socketChannel.keyFor(selector).cancel();
            try {
                socketChannel.close();
            } catch (IOException e) {
                log.log(Level.SEVERE, "Problems with closing connection", e);
            }
        }
        try {
            selector.close();
            serverSocketChannel.close();
        } catch (IOException e) {
            log.log(Level.SEVERE, "Problems with shutting down server", e);
        }
        log.info("Connections closed");
    }


    private void shutdownExecutors() {
        scheduleService.shutdown();
        trackerService.shutdown();
        indexUpdater.shutdown();
        log.info("Executors shutting down");
    }

    private final class UpdateChapterIndexTask implements Runnable {
        private SocketChannel peerChannel;

        private List<ChapterHeader> contentList;

        private UpdateChapterIndexTask(SocketChannel peerChannel, List<ChapterHeader> contentList) {
            this.peerChannel = peerChannel;
            this.contentList = contentList;
            log.info("UpdateChapterIndexTask for " + peerChannel.toString());
        }

        /**
         * @see Thread#run()
         */
        @Override
        public void run() {
            for (ChapterHeader chapterHeader : contentList) {
                Set<SocketChannel> seeds = chapterIndex.get(chapterHeader);
                if (seeds == null) {
                    final Set<SocketChannel> value = new CopyOnWriteArraySet<>();
                    seeds = chapterIndex.putIfAbsent(chapterHeader, value);
                    if (seeds == null) {
                        seeds = value;
                    }
                }
                seeds.add(peerChannel);
            }
            log.info("Content list from socketChannel " + peerChannel.toString() + " has been written");
        }

    }

    private final class ReadWorker implements Runnable {

        private Map<SocketChannel, RawReceivedData> rawSendDataMap = new HashMap<>();

        /**
         * @see Thread#run()
         */
        @Override
        public void run() {
            try {
                ReadQuery readQuery = readQueries.poll(100, TimeUnit.MILLISECONDS);
                if (readQuery == null) {
                    return;
                }
                RawReceivedData rawReceivedData = rawSendDataMap.get(readQuery.getSource());
                if (rawReceivedData == null) {
                    rawReceivedData = new RawReceivedData();
                }
                rawReceivedData.addData(readQuery.getReadData());
                for (Packet packet : rawReceivedData.makeMessages()) {
                    switch (packet.getPacketType()) {
                        case CONTENT_LIST:
                            indexUpdater.submit(new UpdateChapterIndexTask(readQuery.getSource(), packet.getContentList()));
                            break;
                        case BOOK_REQUEST:
                            trackerService.submit(new BookRequestTask(readQuery.getSource(), packet.getBookRequest()));
                            break;
                        case CHAPTER:
                            chapterPost.put(packet.getChapter());
                            break;
                    }
                }
                rawSendDataMap.put(readQuery.getSource(), rawReceivedData);
            } catch (InterruptedException e) {
//                log.log(Level.SEVERE, "Interrupted while reading  ", e);
            } catch (ClassNotFoundException e) {
                log.log(Level.SEVERE, "Strange Exception", e);
            } catch (IOException e) {
                log.log(Level.SEVERE, "Packet Corrupted", e);
            }
        }

    }

    private final class BookRequestTask implements Runnable {
        @NotNull
        private final List<ChapterHeader> bookRequest;

        @NotNull
        private final SocketChannel leech;

        /**
         * @see Thread#run()
         */
        @Override
        public void run() {
            registerChaptersQueue(bookRequest);
        }

        public BookRequestTask(@NotNull SocketChannel leech, @NotNull List<ChapterHeader> bookRequest) {
            this.leech = leech;
            this.bookRequest = bookRequest;
            registerChaptersQueue(bookRequest);
        }

        private void registerChaptersQueue(@NotNull List<ChapterHeader> bookRequest) {
            for (ChapterHeader chapterHeader : bookRequest) {
                Set<SocketChannel> leeches = chapterRequests.get(chapterHeader);
                if (leeches == null) {
                    final Set<SocketChannel> value = new CopyOnWriteArraySet<>();
                    leeches = chapterRequests.putIfAbsent(chapterHeader, value);
                    if (leeches == null) {
                        leeches = value;
                    }
                }
                leeches.add(leech);
            }
        }

    }

    private final class ChapterPostServiceWorker implements Runnable {
        /**
         * @see Thread#run()
         */
        @Override
        public void run() {
            try {
                Chapter chapter = chapterPost.poll(100, TimeUnit.MILLISECONDS);
                if (chapter == null) {
                    return;
                }
                Set<SocketChannel> leeches = chapterRequests.remove(chapter.getHeader());
                if (leeches == null) {
                    return;
                }
                byte[] chapterBytes = new ChapterPacket(chapter).toByteArray();
                for (SocketChannel leech : leeches) {
                    send(chapterBytes, leech);
                }
            } catch (InterruptedException e) {
                log.log(Level.SEVERE, "Interrupted", e);
            } catch (IOException e) {
                log.log(Level.SEVERE, "Strange IO Exception when getting chapter bytes ", e);
            }
        }

    }

    private final class ChapterHarvester implements Runnable {
        /**
         * @see Thread#run()
         */
        @Override
        public void run() {
            for (Map.Entry<ChapterHeader, Set<SocketChannel>> headerListEntry : chapterRequests.entrySet()) {
                for (SocketChannel socketChannel : headerListEntry.getValue()) {
                    if (socketChannel.isConnected()) {
                        trackerService.submit(new getChapterTask(headerListEntry.getKey()));
                        break;
                    }
                }
            }
        }


    }

    private final class getChapterTask implements Runnable {

        @NotNull
        private final ChapterHeader chapterHeader;

        public getChapterTask(@NotNull ChapterHeader chapterHeader) {
            this.chapterHeader = chapterHeader;
        }

        /**
         * @see Thread#run()
         */
        @Override
        public void run() {
            Set<SocketChannel> seeds = chapterIndex.get(chapterHeader);
            if (seeds != null) {
                for (SocketChannel seed : seeds) {
                    if (seed.isConnected()) {
                        try {
                            send(new ChapterRequestPacket(chapterHeader).toByteArray(), seed);
                            return;
                        } catch (IOException e) {
                            log.log(Level.SEVERE, "Strange IOException", e);
                        }
                    } else {
                        indexUpdater.submit(new RemoveSeedTask(seed));
                    }


                }
            }
        }
    }

    private final class RemoveSeedTask implements Runnable {


        @NotNull
        private final SocketChannel seed;

        public RemoveSeedTask(@NotNull SocketChannel seed) {
            this.seed = seed;
        }

        /**
         * @see Thread#run()
         */
        @Override
        public void run() {
            for (Set<SocketChannel> socketChannels : chapterIndex.values()) {
                socketChannels.remove(seed);
            }
        }
    }

    private final class ModeChanger implements Runnable {
        /**
         * @see Thread#run()
         */
        @Override
        public void run() {
            for (Map.Entry<SocketChannel, List<ByteBuffer>> entry : pendingData.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    changeRequests.add(entry.getKey());
                    selector.wakeup();
                }
            }
        }
    }

    private final class Finisher implements Runnable {
        /**
         * @see Thread#run()
         */
        @Override
        public void run() {
            for (SocketChannel socketChannel : pendingData.keySet()) {
                if (!socketChannel.isConnected()) {
                    pendingData.remove(socketChannel);
                }
            }
        }
    }


    /**
     * Creates and starts tracker thread
     *
     * @param args must be {hostname, portNumber, /path/to/logfile}
     */
    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Wrong arguments " + Arrays.toString(args));
            return;
        }
        try {
            InetAddress localHost = InetAddress.getByName(args[0]);
            int port = Integer.parseInt(args[1]);
            String logfile = args[2];
            Tracker tracker = new Tracker(localHost, port, logfile);
            new Thread(tracker).start();
        } catch (IOException e) {
            System.err.println("Strange IOException " + e.getMessage());
            e.printStackTrace();
        }
    }
}
