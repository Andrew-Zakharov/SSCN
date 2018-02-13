import os, sys, socket, select, time, struct, random, threading
from utils import get_timer, create_socket, _checksum

ICMP_ECHO_REQUEST = 8
sequnce_number = 0
id = id(threading.current_thread()) & 0xFFFF
port = random.randint(1, 65535)

def create_echo_request():
        checksum = 0
        header = struct.pack("!BBHHH", ICMP_ECHO_REQUEST, 0, checksum, id, sequnce_number)
        
        pad_bytes = []
        start_val = 0x42
        for i in range(start_val, start_val + (55-8)):
            pad_bytes += [(i & 0xff)]
        data = bytearray(pad_bytes)

        checksum = _checksum(header + data)
        
        header = struct.pack("!BBHHH", ICMP_ECHO_REQUEST, 0, checksum, id, sequnce_number)

        return header + data
        
if __name__ == '__main__':
    default_timer = get_timer()
    icmp_socket = socket.socket(socket.AF_INET,socket.SOCK_RAW,socket.IPPROTO_ICMP)
    icmp_socket.bind(('', port))
    if(icmp_socket != None):
        ttl = 1
        i = 0
        destination = sys.argv[1]
        while(True):
            sequnce_number += 1
            i += 1
            icmp_socket.setsockopt(socket.SOL_IP, socket.IP_TTL, ttl)
            icmp_socket.sendto(create_echo_request(), (destination, port))
            
            ready = select.select([icmp_socket], [], [], 15)
            if not ready[0]:
                print(i, "*\t*\t*")
                continue
                
            try:
                recPacket, addr = icmp_socket.recvfrom(2048)
            except socket.error:
                print("Can't receive")
            
            icmp_header = recPacket[20:28]
            type, code, checksum, actual_port, sequence = struct.unpack('!BBHHH', icmp_header)
            
            print(i, addr[0])
            
            if type == 0:
                break
                
            if type == 11 and code == 0:
                ttl +=1
                
        icmp_socket.close()