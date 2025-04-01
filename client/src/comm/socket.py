import errno
import logging
from socket import AF_INET, SOCK_DGRAM, socket, timeout, error
from typing import Optional, Union
from uuid import uuid4

from src.comm.types import BaseModel, ErrorObj, RequestType, UnmarshalResult
from src.comm.parser import Parser

logger = logging.getLogger(__name__)


class Socket():
    def __init__(self):
        pass

    def send(self, message: any, service_id: int, request_type: RequestType) -> Union[tuple[BaseModel, None], tuple[None, ErrorObj]]:
        pass

    def listen(self):
        pass

    def non_blocking_listen(self):
        pass
class AtLeastOnceSocket(Socket):
    def __str__(self):
        return "AtLeastOnceSocket"
    
    def __init__(self, parser: Parser, timeout_seconds: int = 60, ip_addr: str = "127.0.0.1", port: int = 11999):
        super().__init__()
        self.parser = parser
        self.socket = socket(AF_INET, SOCK_DGRAM)
        self.socket.settimeout(timeout_seconds)
        addr = (ip_addr, port)
        self.socket.bind(addr)
        logger.info(
            "[AtLeastOnceSocket] Socket created at %s:%s", ip_addr, port)

    def _clear_buffer(self):
        logger.debug("[AtLeastOnceSocket] Clearing buffer")
        self.socket.setblocking(False)
        try:
            while True:
                data, addr = self.socket.recvfrom(4096)
                logger.info(f"Cleared packet from {addr}: {data}")
        except BlockingIOError:
            # Buffer is empty
            pass
        finally:
            self.socket.setblocking(True)  # Restore blocking mode
        logger.debug("[AtLeastOnceSocket] Buffer cleared")
        
    def send(self, message: any, service_id: int, request_type: RequestType, server_addr: str = "127.0.0.1", port: int = 12000) -> Union[tuple[BaseModel, None], tuple[None, ErrorObj]]:
        request_id = uuid4()
        logger.info("[AtLeastOnceSocket] To server %s:%s: Sending request %s: %s",
                    server_addr, port, request_id, message)
        msg_in_bytes = self.parser.marshall(
            request_id, service_id, request_type, message)

        addr = (server_addr, port)
        result = None
        while result is None:
            self._clear_buffer()  # Clear the buffer before sending
            self.socket.sendto(msg_in_bytes, addr)
            logger.debug(f"[AtLeastOnceSocket] Sent request.")
            result = self.listen()
            if result.request_id != request_id:
                logger.error(
                    "[AtLeastOnceSocket] Received response with different request ID: %s", result.request_id)
                result = None

        if result.request_type == RequestType.ERROR:
            logger.error("[AtLeastOnceSocket] Error: %s", result.obj)
            assert isinstance(
                result.obj, ErrorObj), "Error object is not of type ErrorObj"
            return None, result.obj
        return result.obj, None

    def listen(self) -> Optional[UnmarshalResult]:
        result = None
        try:
            recv_bytes, _ = self.socket.recvfrom(1024)
            result = self.parser.unmarshall(recv_bytes)
        except timeout:
            logger.error("Socket timeout error")
        except OSError as os_error:
            logger.error("Socket error: %s", os_error)
        except Exception as e:
            logger.error("Error: %s", e)
        finally:
            return result

    def non_blocking_listen(self) -> Optional[UnmarshalResult]:
        result = None
        try:
            recv_bytes = self.socket.recv(1024, socket.MSG_DONTWAIT)
            result = self.parser.unmarshall(recv_bytes)
        except error as e:
            err = e.args[0]
            if err == errno.EAGAIN or err == errno.EWOULDBLOCK:
                # No data available to read, continue without error
                pass
            else:
                logger.error("Socket error: %s", e)
        except timeout:
            pass
        except OSError as os_error:
            logger.error("Socket error: %s", os_error)
        except Exception as e:
            logger.error("Error: %s", e)
        finally:
            return result

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

    def __str__(self):
        return "AtMostOnceSocket"
    
    def __init__(self, parser: Parser, timeout_seconds: int = 60,
                 ip_addr: str = "127.0.0.1", port: int = 11999):
        super().__init__()
        self.parser = parser
        self.socket = socket(AF_INET, SOCK_DGRAM)
        self.socket.settimeout(timeout_seconds)
        addr = (ip_addr, port)
        self.socket.bind(addr)
        logger.info("[AtMostOnceSocket] Socket created at %s:%s",
                    ip_addr, port)

    def _clear_buffer(self):
        logger.debug("[AtMostOnceSocket] Clearing buffer")
        self.socket.setblocking(False)
        try:
            while True:
                data, addr = self.socket.recvfrom(4096)
                logger.info(f"Cleared packet from {addr}: {data}")
        except BlockingIOError:
            # Buffer is empty
            pass
        finally:
            self.socket.setblocking(True)
        logger.debug("[AtMostOnceSocket] Buffer cleared")

    def send(self, message: any, service_id: int, request_type: RequestType,
             server_addr: str = "127.0.0.1", port: int = 12000) -> Union[tuple[BaseModel, None], tuple[None, ErrorObj]]:
        request_id = uuid4()
        logger.info("[AtMostOnceSocket] To server %s:%s: Sending request %s: %s",
                    server_addr, port, request_id, message)
        msg_in_bytes = self.parser.marshall(
            request_id, service_id, request_type, message)
        addr = (server_addr, port)
        result = None
        while result is None:
            self._clear_buffer()  # Clear the buffer before sending
            self.socket.sendto(msg_in_bytes, addr)
            logger.debug(f"[AtMostOnceSocket] Sent request.")
            result = self.listen()
            if result.request_id != request_id:
                logger.error(
                    "[AtMostOnceSocket] Received response with different request ID: %s", result.request_id)
                result = None

        ack_packet_in_bytes = self.parser.marshall(
            request_id, service_id, RequestType.ACK, None)
        self.socket.sendto(ack_packet_in_bytes, addr)

        if result.request_type == RequestType.ERROR:
            logger.error("[AtMostOnceSocket] Error: %s", result.obj)
            assert isinstance(
                result.obj, ErrorObj), "Error object is not of type ErrorObj"
            return None, result.obj
        return result.obj, None

    def listen(self) -> Optional[UnmarshalResult]:
        result = None
        try:
            recv_bytes, _ = self.socket.recvfrom(1024)
            result = self.parser.unmarshall(recv_bytes)
        except timeout:
            logger.error("Socket timeout error")
        except OSError as os_error:
            logger.error("Socket error: %s", os_error)
        except Exception as e:
            logger.error("Error: %s", e)
        finally:
            return result

    def non_blocking_listen(self) -> Optional[UnmarshalResult]:
        result = None
        try:
            recv_bytes = self.socket.recv(1024, socket.MSG_DONTWAIT)
            result = self.parser.unmarshall(recv_bytes)
        except timeout:
            pass
        except OSError as os_error:
            logger.error("Socket error: %s", os_error)
        except Exception as e:
            logger.error("Error: %s", e)
        finally:
            return result
        
    def close(self):
        # shut socket down
        self.socket.close()
