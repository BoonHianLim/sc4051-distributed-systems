import errno
import logging
import random
from socket import AF_INET, SOCK_DGRAM, socket, timeout, error
from typing import Optional, Union
from uuid import uuid4

from src.comm.types import BaseModel, ErrorObj, RequestType, SocketLostType, UnmarshalResult
from src.comm.parser import Parser

logger = logging.getLogger(__name__)


class Socket():
    def __init__(self):
        self.packet_to_be_lost = 0
        self.loss_type = SocketLostType.LOST_IN_CLIENT_TO_SERVER  # Default loss type

    def set_packet_to_be_lost(self, count: int):
        self.packet_to_be_lost = count

    def set_loss_type(self, loss_type: SocketLostType):
        self.loss_type = loss_type

    def has_lost(self, lost_type: SocketLostType) -> bool:
        if (lost_type == SocketLostType.LOST_IN_CLIENT_TO_SERVER and self.loss_type == SocketLostType.LOST_IN_CLIENT_TO_SERVER) \
                or (lost_type == SocketLostType.LOST_IN_SERVER_TO_CLIENT and self.loss_type == SocketLostType.LOST_IN_SERVER_TO_CLIENT) \
                or (lost_type == SocketLostType.ACK and self.loss_type == SocketLostType.ACK):
            if self.packet_to_be_lost > 0:
                self.packet_to_be_lost -= 1
                logger.info("[Socket] Simulated packet loss")
                return True
            else:
                logger.info("[Socket] No simulated packet loss")
                return False
        return False
    
    def send(self, message: any, service_id: int, request_type: RequestType) -> Union[tuple[BaseModel, None], tuple[None, ErrorObj]]:
        pass

    def listen(self):
        pass

    def non_blocking_listen(self):
        pass

class AtLeastOnceSocket(Socket):
    def __str__(self):
        return "AtLeastOnceSocket"
    
    def __init__(self, parser: Parser, timeout_seconds: int = 60, ip_addr: str = "0.0.0.0", port: int = 11999):
        super().__init__()
        self.parser = parser
        self.socket = socket(AF_INET, SOCK_DGRAM)
        self.timeout_seconds = timeout_seconds
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
            self.socket.settimeout(self.timeout_seconds)  # Restore timeout
        logger.debug("[AtLeastOnceSocket] Buffer cleared")
    
    def send(self, message: any, service_id: int, request_type: RequestType, server_addr: str = "172.20.10.5", port: int = 12000) -> Union[tuple[BaseModel, None], tuple[None, ErrorObj]]:
        request_id = uuid4()
        logger.info("[AtLeastOnceSocket] To server %s:%s: Sending request %s: %s",
                    server_addr, port, request_id, message)
        msg_in_bytes = self.parser.marshall(
            request_id, service_id, request_type, message)

        addr = (server_addr, port)
        result = None
        while result is None:
            request_lost = self.has_lost(SocketLostType.LOST_IN_CLIENT_TO_SERVER)
            if request_lost:
                print("Packet lost in transit before sending to server.")
                logger.info("[AtLeastOnceSocket] Simulated packet loss before sending to server")
            else:
                self._clear_buffer()  # Clear the buffer before sending
                self.socket.sendto(msg_in_bytes, addr)
            logger.debug(f"[AtLeastOnceSocket] Sent request.")

            result = self.listen()
            if result is None:
                if request_lost:
                    logger.info("[AtLeastOnceSocket] Simulated packet loss before sending to server. Receive None as we never send the packet to server. Retrying,")
                    print("Packet has lost in transit and we timeout. Retrying...")
                else:
                    logger.error("[AtLeastOnceSocket] No response received. Timeout. Retrying...")
                continue
            if result.request_id != request_id:
                logger.error(
                    "[AtLeastOnceSocket] Received response with different request ID: %s", result.request_id)
                result = None
            if self.has_lost(SocketLostType.LOST_IN_SERVER_TO_CLIENT):
                print("Packet lost in transit after sending to server.")
                logger.info(
                    "[AtLeastOnceSocket] Simulated packet loss after sending to server")
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
            recv_bytes, _ = self.socket.recvfrom(4096)
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
            recv_bytes = self.socket.recv(4096)
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
                 ip_addr: str = "0.0.0.0", port: int = 11999):
        super().__init__()
        self.parser = parser
        self.socket = socket(AF_INET, SOCK_DGRAM)
        self.timeout_seconds = timeout_seconds
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
            self.socket.settimeout(self.timeout_seconds)  # Restore timeout
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
            request_lost = self.has_lost(SocketLostType.LOST_IN_CLIENT_TO_SERVER)
            if request_lost:
                print("Packet lost in transit before sending to server.")
                logger.info(
                    "[AtMostOnceSocket] Simulated packet loss before sending to server")
            else:
                self._clear_buffer()  # Clear the buffer before sending
                self.socket.sendto(msg_in_bytes, addr)
            logger.debug(f"[AtMostOnceSocket] Sent request.")
            result = self.listen()
            if result is None:
                if request_lost:
                    logger.info(
                        "[AtLeastOnceSocket] Simulated packet loss before sending to server. Receive None as we never send the packet to server. Retrying,")
                    print("Packet has lost in transit and we timeout. Retrying...")
                else:
                    logger.error(
                        "[AtLeastOnceSocket] No response received. Timeout. Retrying...")
                continue
            if result.request_id != request_id:
                logger.error(
                    "[AtMostOnceSocket] Received response with different request ID: %s", result.request_id)
                result = None
            if self.has_lost(SocketLostType.LOST_IN_SERVER_TO_CLIENT):
                print("Packet lost in transit after sending to server.")
                logger.info(
                    "[AtMostOnceSocket] Simulated packet loss after sending to server")
                result = None

        if self.has_lost(SocketLostType.ACK):
            print("ACK packet lost in transit.")
            logger.info(
                "[AtMostOnceSocket] Simulated ACK packet loss in transit")
        else:
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
            recv_bytes, _ = self.socket.recvfrom(4096)
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
            recv_bytes = self.socket.recv(4096)
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
