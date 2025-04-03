from typing import Optional, Union
import unittest
from src.comm.parser import Parser
from src.comm.socket import AtLeastOnceSocket, AtMostOnceSocket
from src.comm.types import BaseModel, ErrorObj, RequestType, UnmarshalResult

class MockAtLeastOnceSocket(AtLeastOnceSocket):
    def __init__(self):
        pass
    
    def __str__(self):
        return super().__str__()
    
    def send(self, message: any, service_id: int, request_type: RequestType, server_addr: str = "127.0.0.1", port: int = 12000) -> Union[tuple[BaseModel, None], tuple[None, ErrorObj]]:
        pass

    def listen(self) -> Optional[UnmarshalResult]:
        pass

    def non_blocking_listen(self):
        pass

class MockAtMostOnceSocket(AtMostOnceSocket):
    def __init__(self):
        pass

    def __str__(self):
        return super().__str__()
    
    def send(self, message: any, service_id: int, request_type: RequestType, server_addr: str = "127.0.0.1", port: int = 12000) -> Union[tuple[BaseModel, None], tuple[None, ErrorObj]]:
        pass
    
    def listen(self):
        pass

    def non_blocking_listen(self):
        pass

class TestSocket(unittest.TestCase):
    def test_at_least_once_str(self):
        socket = MockAtLeastOnceSocket()
        self.assertEqual(str(socket), "AtLeastOnceSocket")

    def test_at_most_once_str(self):
        socket = MockAtMostOnceSocket()
        self.assertEqual(str(socket), "AtMostOnceSocket")