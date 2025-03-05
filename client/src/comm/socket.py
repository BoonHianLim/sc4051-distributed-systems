import logging
from socket import AF_INET, SOCK_DGRAM, socket, timeout
import threading
import time
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
    buffer: list[dict] = []
    shutdown_flag = False
    listen_thread = None

    def __init__(self, parser: Parser, timeout_seconds: int = 60, ip_addr: str = "127.0.0.1", port: int = 11999):
        super().__init__()
        self.parser = parser
        self.socket = socket(AF_INET, SOCK_DGRAM)
        self.timeout_seconds = timeout_seconds
        self.addr = (ip_addr, port)

    def send(self, message: any, service_id: int, is_request: bool) -> any:
        request_id = uuid4()
        msg_in_bytes = self.parser.marshall(
            request_id, service_id, is_request, message)

        addr = ("127.0.0.1", 12000)
        while True:
            self.socket.sendto(msg_in_bytes, addr)

            start_time = time.time()
            # busy wait
            while len(self.buffer) == 0:
                if time.time() - start_time >= self.timeout_seconds:
                    break
                if self.shutdown_flag:
                    raise ConnectionError("Socket is closed")
                time.sleep(0.1)
            success = len(self.buffer) >= 1
            if success:
                break
        # assume that buffer elements will always be correctly parsed
        obj = self.buffer.pop()

        # clear buffer
        self.buffer.clear()
        return obj

    def listen(self):
        if self.listen_thread is not None:
            return
        self.socket.bind(self.addr)
        self.shutdown_flag = False
        self.listen_thread = threading.Thread(target=self._listen, daemon=True)
        self.listen_thread.start()

    def _listen(self):
        while self.shutdown_flag is False:
            recv_bytes = None
            obj = None
            try:
                recv_bytes, _ = self.socket.recvfrom(1024)
                obj = self.parser.unmarshall(recv_bytes)
                if obj["is_request"]:
                    # We received a request, since we are client, that means a callback.
                    # TODO: Handle the callback here.

                    # For testing purpose, add to buffer first.
                    # TODO: To remove the following line.
                    self.buffer.append(obj)
                else:
                    self.buffer.append(obj)
            except timeout:
                logger.error("Socket timeout error")
            except OSError as os_error:
                if self.shutdown_flag:
                    logger.debug("Socket closed, mostly due to shutdown.")
                else:
                    logger.error("Socket error: %s", os_error)

    def close(self):
        if self.listen_thread is not None:
            self.shutdown_flag = True
            self.socket.close()
            self.listen_thread.join()
        # clean up
        self.listen_thread = None
        self.shutdown_flag = False


class AtMostOnceSocket(Socket):
    pass
