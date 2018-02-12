#!/usr/bin/env python

import sys, socket, random
from utils import create_receive_socket, create_send_socket
__all__ = ['Tracer']


class Tracer(object):
    def __init__(self, dst, hops=30):
        self.dst = dst
        self.hops = hops
        self.ttl = 1
        self.port = 8888

    def run(self):
        try:
            dst_ip = socket.gethostbyname(self.dst)
        except socket.error as e:
            raise IOError('Unable to resolve {}: {}', self.dst, e)

        text = 'traceroute to {} ({}), {} hops max'.format(
            self.dst,
            dst_ip,
            self.hops
        )

        print(text)

        while True:
            receiver = create_receive_socket(6666)
            sender = create_send_socket(self.ttl)
            sender.sendto(b'', (self.dst, self.port))

            addr = None
            try:
                data, addr = receiver.recvfrom(1024)
            except socket.error as e:
                raise IOError('Socket error: {}'.format(e))
            finally:
                receiver.close()
                sender.close()

            if addr:
                print('{:<4} {}'.format(self.ttl, addr[0]))
            else:
                print('{:<4} *'.format(self.ttl))

            self.ttl += 1

            if addr[0] == dst_ip or self.ttl > self.hops:
                break


if __name__ == '__main__':
    assert len(sys.argv) == 2
    Tracer(sys.argv[1]).run()