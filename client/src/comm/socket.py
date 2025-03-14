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

    def send(self, message: any, service_id: int, is_request: bool, server_addr: str = "127.0.0.1", port: int = 12000) -> any:
        request_id = uuid4()
        msg_in_bytes = self.parser.marshall(
            request_id, service_id, is_request, message)

        addr = (server_addr, port)
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

# work on this (wei)
class AtMostOnceSocket(Socket):
    # High level requirements
    # 1. Client re-transmits requests
        # Occurs when client does not receive a response in time, basically timeout
    # 2. Server filter duplicates
    # 3. Client sends ACK
        # After client gets valid response, it sends an ACK to the server
        # Server stops re-sending response once it receives ACK or max attempts

    buffer: list[dict] = []
    shutdown_flag = False
    listen_thread = None

    def __init__(self, parser: Parser, timeout_seconds: int = 60,
                 ip_addr: str = "127.0.0.1", port: int = 11999):
        super().__init__()
        self.parser = parser
        self.socket = socket(AF_INET, SOCK_DGRAM)
        self.timeout_seconds = timeout_seconds
        self.addr = (ip_addr, port)

    def send(self, message: any, service_id: int, is_request: bool,
             server_addr: str = "127.0.0.1", port: int = 12000) -> any:
        request_id = uuid4()
        msg_in_bytes = self.parser.marshall(request_id, service_id, is_request, message)
        addr = (server_addr, port)

        while True:
            # (a) send request
            self.socket.sendto(msg_in_bytes, addr)
            logger.debug(f"[AtMostOnceSocket] Sent request {request_id}")
            print(f"[AtMostOnceSocket] Sent request {request_id}")

            # (b) wait time out secs for response to appear in buffer
            start_time = time.time()
            while True:
                # break if no response after timeout
                if time.time() - start_time >= self.timeout_seconds:
                    break
                if self.shutdown_flag:
                    raise ConnectionError("Socket is closed")     
                # check if we have anything in buffer
                if len(self.buffer) > 0:
                    obj = self.buffer.pop()
                    if obj["request_id"] == request_id:
                        logger.debug(f"[AtMostOnceSocket] Received matching response for {request_id}")
                        print(f"[AtMostOnceSocket] Received matching response for {request_id}")

                        # (c) Send ACK to server
                        ack_bytes = self.parser.marshall_ack(request_id)
                        self.socket.sendto(ack_bytes, addr)
                        logger.debug(f"sent ACK for {request_id}")
                        print(f"sent ACK for {request_id}")
                        # Return the object
                        return obj
                    else:
                        # Not the correct response => ignore
                        logger.debug(f"[AtMostOnceSocket] Ignoring unrelated response: {obj['request_id']}")
                        print(f"[AtMostOnceSocket] Ignoring unrelated response: {obj['request_id']}")
                time.sleep(0.1)

            # If we reach here, no matching response arrived => re-send
            logger.debug("[AtMostOnceSocket] No matching response, re-sending request...")
            print("[AtMostOnceSocket] Timeout, re-sending request...")

    def listen(self):
        # to bind socket to self.addr, while continuously receving packets
        if self.listen_thread is not None:
            return  
        self.socket.bind(self.addr)
        self.shutdown_flag = False
        self.listen_thread = threading.Thread(target=self._listen, daemon=True)
        self.listen_thread.start()

    def _listen(self):
        while not self.shutdown_flag:
            try:
                recv_bytes, _ = self.socket.recvfrom(1024)
                obj = self.parser.unmarshall(recv_bytes)
                self.buffer.append(obj)
            except timeout:
                logger.error("[AtMostOnceSocket] Socket timeout in listen thread")
            except OSError as os_err:
                if self.shutdown_flag:
                    logger.debug("[AtMostOnceSocket] Socket closed due to shutdown.")
                else:
                    logger.error("[AtMostOnceSocket] Socket error: %s", os_err)
                break
    
    def close(self):
        # shut socket down
        if self.listen_thread is not None:
            self.shutdown_flag = True
            self.socket.close()
            self.listen_thread.join()

        self.listen_thread = None
        self.shutdown_flag = False
