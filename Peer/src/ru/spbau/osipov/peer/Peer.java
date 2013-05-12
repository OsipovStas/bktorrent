package ru.spbau.osipov.peer;

import book.Book;
import book.BookHeader;
import book.Chapter;
import book.ChapterHeader;
import com.sun.istack.internal.NotNull;
import packets.BookRequestPacket;
import packets.ChapterPacket;
import packets.ContentListPacket;
import packets.Packet;
import rawData.RawReceivedData;
import rawData.ReadQuery;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * This class represents peer
 *
 * @author Osipov Stanislav
 */
public final class Peer implements Runnable {

    @NotNull
    private final static Logger log = Logger.getLogger(Peer.class.getName());

    private static final int PEER_TIME_LIMIT = 200;

    private boolean shutdownFlag = false;

    private boolean isSeed = false;

    @NotNull
    private final InetAddress host;
    private int port;

    // The selector we'll be monitoring
    @NotNull
    private final Selector selector;

    @NotNull
    private SocketChannel socketChannel;

    @NotNull
    private final ByteBuffer readBuffer = ByteBuffer.allocate(4092);

    @NotNull
    private final File rootDir;
    @NotNull
    private final ConcurrentMap<Long, Book> bookshelf = new ConcurrentHashMap<>();
    @NotNull
    private final List<ChapterHeader> contentList = new ArrayList<>();
    @NotNull
    private final List<ByteBuffer> pendingData = new CopyOnWriteArrayList<>();
    @NotNull
    private final Set<SocketChannel> changeRequests = new CopyOnWriteArraySet<>();
    @NotNull
    private final BlockingQueue<ReadQuery> readQueries = new ArrayBlockingQueue<>(100);


    @NotNull
    private ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(2);

    /**
     * Initializes peer
     *
     * @param rootDir peer root directory
     * @param host    hostname
     * @param port    port number
     * @param logfile logfile
     * @param isSeed  mode
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public Peer(@NotNull File rootDir, @NotNull InetAddress host, int port, @NotNull String logfile, boolean isSeed) throws IOException, ClassNotFoundException {
        this.host = host;
        this.port = port;
        this.rootDir = rootDir;
        this.selector = SelectorProvider.provider().openSelector();
        this.isSeed = isSeed;
        configureLogger(logfile);
        initialize();
        makeConnection();
    }

    /**
     * Adds torrents
     *
     * @param torrentDir directory where  the torrent files are
     */
    public void addBooks(@NotNull File torrentDir) {
        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isFile() && pathname.getName().endsWith(".tr");
            }
        };
        File[] files = torrentDir.listFiles(filter);
        if (files == null) {
            return;
        }
        readTorrents(files);
    }

    private void readTorrents(File[] files) {
        for (File file : files) {
            try {
                BookHeader header = BookHeader.read(file.getAbsolutePath());
                Book book = bookshelf.putIfAbsent(header.getId(), new Book(header));
                if (book == null) {
                    log.info("Book added to shelf " + header.toString());
                }
                log.info("Torrent added" + header.toString());
            } catch (IOException e) {
                log.log(Level.SEVERE, "Strange IOException", e);
            } catch (ClassNotFoundException e) {
                log.log(Level.SEVERE, "Strange ClassNotFoundException", e);
            }
        }
    }

    /**
     * Shutdown peer
     */
    public void shutdownPeer() {
        shutdownFlag = true;
        selector.wakeup();
    }

    private void configureLogger(String logfile) throws IOException {
        FileHandler fileHandler = new FileHandler(logfile, 1000000, 1, true);
        log.addHandler(fileHandler);
        fileHandler.setFormatter(new SimpleFormatter());
    }

    private void makeConnection() throws IOException {
        this.socketChannel = SocketChannel.open();
        socketChannel.connect(new InetSocketAddress(host, port));
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ);
        fillContent();
    }

    private void fillContent() {
        for (Book book : bookshelf.values()) {
            contentList.addAll(book.getContentList());
        }
    }

    private void initialize() throws IOException, ClassNotFoundException {
        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isFile() && pathname.getName().endsWith(".bk");
            }
        };
        File[] files = rootDir.listFiles(filter);
        if (files == null) {
            return;
        }
        readBooks(files);
        scheduleServices();
    }

    private void scheduleServices() {
        scheduledExecutor.scheduleAtFixedRate(new ContentSender(), 3, 5, TimeUnit.SECONDS);
        scheduledExecutor.scheduleAtFixedRate(new RequestSender(), 3, 40, TimeUnit.SECONDS);
        scheduledExecutor.scheduleAtFixedRate(new ModeChanger(), 5, 2, TimeUnit.SECONDS);
        scheduledExecutor.scheduleAtFixedRate(new ReadWorker(), 3000, 100, TimeUnit.MILLISECONDS);
        if (!isSeed) {
            scheduledExecutor.scheduleAtFixedRate(new Shutdowner(), 50, 20, TimeUnit.SECONDS);
        }
        scheduledExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                shutdownPeer();
            }
        }, PEER_TIME_LIMIT, TimeUnit.SECONDS);
    }

    private void sendBookRequests() throws IOException {
        for (Book book : bookshelf.values()) {
            BookHeader bookHeader = book.getHeader();
            List<ChapterHeader> bookRequest = getBookRequest(book, bookHeader);
            if (bookRequest.size() > 0) {
                sendData(new BookRequestPacket(bookRequest).toByteArray());
                log.info("Send book request for book " + bookHeader.toString() + " for chapters " + bookRequest.toString());
            }
        }
    }

    private List<ChapterHeader> getBookRequest(Book book, BookHeader bookHeader) {
        List<ChapterHeader> bookRequest = new ArrayList<>();
        for (int i = 0; i < bookHeader.getChapterAmount(); ++i) {
            ChapterHeader chapterHeader = new ChapterHeader(bookHeader.getId(), i);
            if (!book.getChapters().containsKey(chapterHeader)) {
                bookRequest.add(chapterHeader);
            }
        }
        return bookRequest;
    }

    private void readBooks(File[] files) {
        for (File file : files) {
            try {
                Book book = Book.read(file);
                bookshelf.put(book.getHeader().getId(), book);
                log.info("Read book " + book.getHeader().toString());
            } catch (IOException e) {
                log.log(Level.SEVERE, "Strange IOException", e);
            } catch (ClassNotFoundException e) {
                log.log(Level.SEVERE, "Strange ClassNotFoundException", e);
            }
        }
    }

    private void sendData(byte[] data) {
        ByteBuffer dataBuffer = ByteBuffer.allocate(data.length + Integer.SIZE / 8);
        dataBuffer.putInt(data.length);
        dataBuffer.put(data);
        dataBuffer.flip();
        pendingData.add(dataBuffer);
    }


    /**
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

                    if (key.isWritable()) {
                        write(key);
                    } else if (key.isReadable()) {
                        read(key);
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

    private void shutdown() {
        shutdownExecutors();
        closeConnection();
        dumpBooks();
        log.info("ru.spbau.osipov.peer.Peer power off now");
    }

    private void dumpBooks() {
        for (Book book : bookshelf.values()) {
            File file = Paths.get(rootDir.getAbsolutePath(), book.getHeader().getId() + ".bk").toFile();
            try {
                book.dump(file);
            } catch (IOException e) {
                log.log(Level.SEVERE, "Can't dump book " + book.getHeader().toString(), e);
            }
        }
    }

    private void shutdownExecutors() {
        scheduledExecutor.shutdown();
    }

    private void closeConnection() {
        try {
            socketChannel.keyFor(selector).cancel();
            socketChannel.close();
            selector.close();
        } catch (IOException e) {
            log.log(Level.SEVERE, "Can't close connection cleanly ", e);
        }
    }

    private void read(SelectionKey key) throws IOException, ClassNotFoundException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        readBuffer.clear();

        int numRead;
        try {
            numRead = socketChannel.read(readBuffer);
        } catch (IOException e) {
            key.cancel();
            socketChannel.close();
            log.log(Level.SEVERE, "Remote server forcibly closed the connection", e);
            return;
        }

        if (numRead == -1) {
            key.channel().close();
            key.cancel();
            log.info("Remote server" + socketChannel.toString() + " shut the socket down");
            return;
        }
        try {
            readQueries.put(new ReadQuery(socketChannel, Arrays.copyOf(readBuffer.array(), numRead)));
        } catch (InterruptedException e) {
            log.log(Level.SEVERE, "Interrupted while reading in select thread ", e);
        }
        log.log(Level.FINE, "Successfully read data from server " + socketChannel.toString());

    }

    private void write(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        while (!pendingData.isEmpty()) {
            ByteBuffer buf = pendingData.get(0);
            socketChannel.write(buf);
            if (buf.remaining() > 0) {
                break;
            }
            pendingData.remove(0);
        }
        if (pendingData.isEmpty()) {
            key.interestOps(SelectionKey.OP_READ);
        }
    }

    private final class ReadWorker implements Runnable {

        private RawReceivedData rawReceivedData = new RawReceivedData();

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
                rawReceivedData.addData(readQuery.getReadData());
                for (Packet packet : rawReceivedData.makeMessages()) {
                    switch (packet.getPacketType()) {
                        case CHAPTER_REQUEST:
                            sendChapter(packet.getChapterHeader());
                            break;
                        case CHAPTER:
                            saveChapter(packet.getChapter());
                            break;
                    }
                }
            } catch (InterruptedException e) {
                log.log(Level.SEVERE, "Interrupted while reading  ", e);
            } catch (ClassNotFoundException e) {
                log.log(Level.SEVERE, "Strange Exception", e);
            } catch (IOException e) {
                log.log(Level.SEVERE, "Packet Corrupted", e);
            }
        }

        private void saveChapter(Chapter chapter) {
            Book book = bookshelf.get(chapter.getHeader().getBookId());
            if (book == null || book.getChapters().size() == book.getHeader().getChapterAmount()) {
                return;
            }
            book.getChapters().put(chapter.getHeader(), chapter);
            contentList.add(chapter.getHeader());
            log.info("Chapter " + chapter.getHeader().toString() + " received from server");
            if (book.getChapters().size() == book.getHeader().getChapterAmount()) {
                log.info("Book " + book.getHeader().toString() + " completely received!");
            }
        }

        private void sendChapter(ChapterHeader chapterHeader) {
            Book book = bookshelf.get(chapterHeader.getBookId());
            if (book == null) {
                return;
            }
            Chapter chapter = book.getChapters().get(chapterHeader);
            if (chapter == null) {
                return;
            }
            try {
                sendData(new ChapterPacket(chapter).toByteArray());
            } catch (IOException e) {
                log.log(Level.SEVERE, "IOException while sending chapter", e);
            }
        }

    }

    private final class ContentSender implements Runnable {
        /**
         * @see Thread#run()
         */
        @Override
        public void run() {
            if (contentList.size() < 10) {
                return;
            }
            List<ChapterHeader> newContentList = new ArrayList<>();
            while (!contentList.isEmpty()) {
                newContentList.add(contentList.remove(0));
            }
            if (!newContentList.isEmpty()) {
                try {
                    sendData(new ContentListPacket(newContentList).toByteArray());
                } catch (IOException e) {
                    log.log(Level.SEVERE, "Strange IOException while creating new content list", e);
                }
                log.info("Content list request send to server");
            }
        }
    }

    private final class RequestSender implements Runnable {
        /**
         * @see Thread#run()
         */
        @Override
        public void run() {
            try {
                sendBookRequests();
            } catch (IOException e) {
                log.log(Level.SEVERE, "IO exception during send requests", e);
            }
        }
    }

    private final class ModeChanger implements Runnable {
        /**
         * @see Thread#run()
         */
        @Override
        public void run() {
            if (!pendingData.isEmpty()) {
                changeRequests.add(socketChannel);
                selector.wakeup();
            }
        }
    }


    private final class Shutdowner implements Runnable {
        /**
         * @see Thread#run()
         */
        @Override
        public void run() {
            for (Book book : bookshelf.values()) {
                if (book.getHeader().getChapterAmount() != book.getChapters().size()) {
                    return;
                }
            }
            log.info("Shutting down " + socketChannel.toString());
            shutdownPeer();
        }
    }

    /**
     * Creates peer thread and starts it
     *
     * @param args must be {path/to/peer/dir, hostname, portNumber, mode, /path/to/logfile, /dir/with/torrent/files}
     */
    public static void main(String[] args) {
        if (args.length < 5) {
            System.err.println("Wrong arguments " + Arrays.toString(args));
            return;
        }
        try {
            File peerRoot = new File(args[0]);
            InetAddress localHost = InetAddress.getByName(args[1]);
            int port = Integer.parseInt(args[2]);
            String logfile = args[4];
            boolean mode = args[3].equals("seed");
            Peer peer = new Peer(peerRoot, localHost, port, logfile, mode);
            new Thread(peer).start();
            if (args.length == 6) {
                peer.addBooks(new File(args[5]));
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

}
