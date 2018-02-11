#!/usr/bin/env python

import os, sys, socket, select, time
from ping_thread import *
from utils import get_timer, create_socket

lock = threading.Lock()
        
if __name__ == '__main__':
    default_timer = get_timer()
    ping_socket = create_socket()
    if(ping_socket != None):
        threads = []
        for host in sys.argv[1:]:
            event = threading.Event()
            thread = PingThread(host, event, ping_socket, lock, 2, 1024)
            threads.append((id(thread) & 0xFFFF, event, thread))
            thread.setDaemon(True)
            thread.start()
            
        while(any(thread[2].isAlive() == True for thread in threads)):
        
            ready = select.select([ping_socket], [], [], 4)
            if not ready[0]:
                continue
                
            try:
                with lock:
                    recPacket, addr = ping_socket.recvfrom(2048)
            except socket.error:
                print("Can't receive")
            
            icmp_header = recPacket[20:28]
            type, code, checksum, port, sequence = struct.unpack('!BBHHH', icmp_header)
            
            index = [index for index, thread in enumerate(threads) if thread[0] == port]
            
            if type == 0 and index:
                threads[index[0]][2].add_received_packet(recPacket)
                threads[index[0]][1].set()
            
        print("End pinging")
        [thread.join() for id, event, thread in threads]
        ping_socket.close()
