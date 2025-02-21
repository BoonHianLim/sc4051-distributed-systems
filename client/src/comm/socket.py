from socket import AF_INET, SOCK_DGRAM, socket, timeout, error
from uuid import uuid4

from src.comm.parser import Parser


class Socket():
    def __init__(self):
        pass

    def send(self, message: any, obj_name: str, recv_obj_name: str) -> any:
        pass


class AtLeastOnceSocket(Socket):
    def __init__(self, parser: Parser, timeout_seconds: int = 60, ip_addr: str = "127.0.0.1", port: int = 11999):
        super().__init__()
        self.parser = parser
        self.socket = socket(AF_INET, SOCK_DGRAM)
        self.socket.settimeout(timeout_seconds)
        addr = (ip_addr, port)
        self.socket.bind(addr)

    def send(self, message: any, obj_name: str, recv_obj_name: str) -> any:
        request_id = uuid4()
        msg_in_bytes = self.parser.marshall(obj_name, message, request_id)

        addr = ("127.0.0.1", 12000)
        self.socket.sendto(msg_in_bytes, addr)

        recv_bytes = None
        obj = None
        while recv_bytes is None:
            try:
                recv_bytes, _ = self.socket.recvfrom(1024)
                obj = self.parser.unmarshall(recv_bytes, recv_obj_name)
                if obj["request_id"] != request_id:
                    continue
            except timeout:
                print("Socket timeout error")
            except error as e:
                print(f"Socket error: {e}")
        obj.pop("request_id")
        return obj

    def receive(self) -> any:
        pass

    def close(self):
        self.socket.close()

class AtMostOnceSocket(Socket):
    def __init__(self, parser: Parser, timeout_seconds: int = 60, ip_addr: str = "127.0.0.1", port: int = 11999):
        super().__init__()
        self.parser = parser
        self.socket = socket(AF_INET, SOCK_DGRAM)
        self.socket.settimeout(timeout_seconds)
        addr = (ip_addr, port)
        self.socket.bind(addr)

    def send(self, message: any, obj_name: str, recv_obj_name: str) -> any:
        request_id = uuid4()
        msg_in_bytes = self.parser.marshall(obj_name, message, request_id)

        addr = ("127.0.0.1", 12000)
        self.socket.sendto(msg_in_bytes, addr)

        recv_bytes = None
        obj = None
        while recv_bytes is None:
            try:
                recv_bytes, _ = self.socket.recvfrom(1024)
                obj = self.parser.unmarshall(recv_bytes, recv_obj_name)
                if obj["request_id"] != request_id:
                    continue
            except timeout:
                print("Socket timeout error")
            except error as e:
                print(f"Socket error: {e}")
        obj.pop("request_id")
        return obj

    def receive(self) -> any:
        pass

    def close(self):
        self.socket.close()
