import threading, struct
from utils import _checksum, get_timer

ICMP_ECHO_REQUEST = 8
ECHO_REQUEST_DEFAULT_COUNT = 4
ECHO_REQUEST_TIMEOUT = 2

class PingThread (threading.Thread):
    def __init__(self, destinationAddress, event, socket, lock, timeout = ECHO_REQUEST_TIMEOUT, count = ECHO_REQUEST_DEFAULT_COUNT):
        threading.Thread.__init__(self)
        self.timeout = timeout
        self.event = event
        self.count = count
        self.destinationAddress = destinationAddress
        self.receivedPackets = []
        self.sequence_number = 1
        self.lock = lock
        self.socket = socket
        self.id = id(self) & 0xFFFF
        self.timer = get_timer()
        self.received_packet_count = 0

    def run(self):
        threading.Thread.run(self)
        for i in range(self.count):
            self.send_echo_request()
            send_time = self.timer()
            if(self.event.wait(self.timeout)):
                receive_time = self.timer()
                self.received_packet_count += 1
                print("Reply from ", self.destinationAddress, "in ", int(round((receive_time - send_time) * 1000)), " ms")
            else:
                print("Request timeout for ", self.destinationAddress)
                
            self.event.clear()
    
    def send_echo_request(self):
        with self.lock:
            self.socket.sendto(self.create_echo_request(), (self.destinationAddress, 1))
        self.sequence_number += 1

    def create_echo_request(self):
        checksum = 0
        header = struct.pack("!BBHHH", ICMP_ECHO_REQUEST, 0, checksum, self.id, self.sequence_number)
        
        pad_bytes = []
        start_val = 0x42
        for i in range(start_val, start_val + (55-8)):
            pad_bytes += [(i & 0xff)]
        data = bytearray(pad_bytes)

        checksum = _checksum(header + data)
        
        header = struct.pack("!BBHHH", ICMP_ECHO_REQUEST, 0, checksum, self.id, self.sequence_number)

        return header + data
        
    def add_received_packet(self, packet):
        self.receivedPackets.append(packet)