__author__ = 'stas'

from subprocess import call
import sys
import os
import threading
import time

class TrackerThread(threading.Thread):

    def __init__(self, java, jar, host, port, log):
        super(TrackerThread, self).__init__()
        self.java = java
        self.jar = jar
        self.host = host
        self.port = port
        self.log = log

    def run(self):
        call([self.java, "-jar", self.jar, self.host, self.port, self.log])

class PeerThread(threading.Thread):

    def __init__(self, java, jar, root, host, port, mode, log, torrent = None):
        super(PeerThread, self).__init__()
        self.java = java
        self.jar = jar
        self.root = root
        self.host = host
        self.port = port
        self.mode = mode
        self.log = log
        self.torrent = torrent

    def run(self):
        if self.torrent == None or not os.path.exists(self.torrent):
            call([self.java, "-jar", self.jar, self.root, self.host, self.port, self.mode, self.log])
        else:
            call([self.java, "-jar", self.jar, self.root, self.host, self.port, self.mode, self.log, self.torrent])

[java, clientJar, serverJar, clientDir, host, port] = sys.argv[1:]

for i in xrange(1, 5):
    if not os.path.exists(clientDir + str(i)):
        os.makedirs(clientDir + str(i))

if not os.path.exists(clientDir + str(5)):
    os.makedirs(clientDir + "5")

trackerlog = clientDir + "trackerlog"
clientlog = clientDir + "clientlog"
torrentdir = clientDir + str(5)

TrackerThread(java, serverJar, host, port, trackerlog).start()
time.sleep(2)
PeerThread(java, clientJar, clientDir + str(5), host, port, "seed", clientlog).start()
for i in xrange(1, 5):
    PeerThread(java, clientJar, clientDir + str(i), host, port, "leech", clientlog, torrentdir).start()

