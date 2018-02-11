import sys, socket, time, os

def _checksum(source_string):
    """
    A port of the functionality of in_cksum() from ping.c
    Ideally this would act on the string as a series of 16-bit ints (host
    packed), but this works.
    Network data is big-endian, hosts are typically little-endian
    """
    count_to = (int(len(source_string)/2))*2
    sum = 0
    count = 0

    # Handle bytes in pairs (decoding as short ints)
    lo_byte = 0
    hi_byte = 0
    while count < count_to:
        if (sys.byteorder == "little"):
            lo_byte = source_string[count]
            hi_byte = source_string[count + 1]
        else:
            lo_byte = source_string[count + 1]
            hi_byte = source_string[count]
        try:     # For Python3
            sum = sum + (hi_byte * 256 + lo_byte)
        except:  # For Python2
            sum = sum + (ord(hi_byte) * 256 + ord(lo_byte))
        count += 2

    # Handle last byte if applicable (odd-number of bytes)
    # Endianness should be irrelevant in this case
    if count_to < len(source_string): # Check for odd length
        lo_byte = source_string[len(source_string)-1]
        try:      # For Python3
            sum += lo_byte
        except:   # For Python2
            sum += ord(lo_byte)

    sum &= 0xffffffff # Truncate sum to 32 bits (a variance from ping.c, which
                      # uses signed ints, but overflow is unlikely in ping)

    sum = (sum >> 16) + (sum & 0xffff)    # Add high 16 bits to low 16 bits
    sum += (sum >> 16)                    # Add carry from above (if any)
    answer = ~sum & 0xffff                # Invert and truncate to 16 bits
    answer = socket.htons(answer)
    return answer
    
def get_timer():
    if sys.platform == "win32":
        return time.clock
    else:
        return time.time

def create_socket():
    icmp = socket.getprotobyname("icmp")
    try:
        ping_socket = socket.socket(socket.AF_INET, socket.SOCK_RAW, icmp)
        ping_socket.setblocking(False)
    except socket.error(errno, msg):
        if errno == 1:
            msg = msg + (
                " - Note that ICMP messages can only be sent from processes"
                " running as root."
            )
            raise socket.error(msg)
        raise
    
    return ping_socket