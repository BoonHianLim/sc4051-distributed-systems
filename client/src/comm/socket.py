import logging
from socket import AF_INET, SOCK_DGRAM, socket, timeout
from typing import Optional
from uuid import uuid4
from src.comm.parser import Parser

logger = logging.getLogger(__name__)

class Socket():
    def __init__(self):
        pass

    def send(self, message: any, service_id: int, is_request: bool) -> any:
        pass

    def listen(self):
        pass


class AtLeastOnceSocket(Socket):
    def __init__(self, parser: Parser, timeout_seconds: int = 60, ip_addr: str = "127.0.0.1", port: int = 11999):
        super().__init__()
        self.parser = parser
        self.socket = socket(AF_INET, SOCK_DGRAM)
        self.socket.settimeout(timeout_seconds)
        addr = (ip_addr, port)
        self.socket.bind(addr)
        logger.info("[AtLeastOnceSocket] Socket created at %s:%s", ip_addr, port)

    def send(self, message: any, service_id: int, is_request: bool, server_addr: str = "127.0.0.1", port: int = 12000) -> any:
        request_id = uuid4()
        logger.info("[AtLeastOnceSocket] To server %s:%s: Sending request %s: %s",
                    server_addr, port, request_id, message)
        msg_in_bytes = self.parser.marshall(
            request_id, service_id, is_request, message)

        addr = (server_addr, port)
        obj = None
        while obj is None:
            self.socket.sendto(msg_in_bytes, addr)
            logger.debug(f"[AtLeastOnceSocket] Sent request.")
            obj = self.listen()
        return obj

    def listen(self) -> Optional[dict]:
        obj = None
        try:
            recv_bytes, _ = self.socket.recvfrom(1024)
            obj = self.parser.unmarshall(recv_bytes)
        except timeout:
            logger.error("Socket timeout error")
        except OSError as os_error:
            logger.error("Socket error: %s", os_error)
        except Exception as e:
            logger.error("Error: %s", e)
        finally:
            return obj

    def close(self):
        self.socket.close()


class AtMostOnceSocket(Socket):
    # High level requirements
    # 1. Client re-transmits requests
    # Occurs when client does not receive a response in time, basically timeout
    # 2. Server filter duplicates
    # 3. Client sends ACK
    # After client gets valid response, it sends an ACK to the server
    # Server stops re-sending response once it receives ACK or max attempts

    def __init__(self, parser: Parser, timeout_seconds: int = 60,
                 ip_addr: str = "127.0.0.1", port: int = 11999):
        super().__init__()
        self.parser = parser
        self.socket = socket(AF_INET, SOCK_DGRAM)
        self.socket.settimeout(timeout_seconds)
        addr = (ip_addr, port)
        self.socket.bind(addr)
        logger.info("[AtMostOnceSocket] Socket created at %s:%s", ip_addr, port)

    def send(self, message: any, service_id: int, is_request: bool,
             server_addr: str = "127.0.0.1", port: int = 12000) -> any:
        request_id = uuid4()
        logger.info("[AtMostOnceSocket] To server %s:%s: Sending request %s: %s",
                    server_addr, port, request_id, message)
        msg_in_bytes = self.parser.marshall(
            request_id, service_id, is_request, message)
        addr = (server_addr, port)
        obj = None
        while obj is None:
            self.socket.sendto(msg_in_bytes, addr)
            logger.debug(f"[AtLeastOnceSocket] Sent request.")
            obj = self.listen()
        return obj

    def listen(self) -> Optional[dict]:
        obj = None
        try:
            recv_bytes, _ = self.socket.recvfrom(1024)
            obj = self.parser.unmarshall(recv_bytes)
        except timeout:
            logger.error("Socket timeout error")
        except OSError as os_error:
            logger.error("Socket error: %s", os_error)
        except Exception as e:
            logger.error("Error: %s", e)
        finally:
            return obj
        
    def close(self):
        # shut socket down
        self.socket.close()
