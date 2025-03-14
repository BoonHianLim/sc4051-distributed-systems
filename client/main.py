import json
import logging
import os
import time
from uuid import uuid4
from src.comm.object import BaseModel
from src.comm.socket import Socket, AtLeastOnceSocket
from src.comm.parser import Parser
from src.utils.logger import setup_logger

logger = logging.getLogger(__name__)

setup_logger()

interface_schema = None
services_schema = None
# TODO: Dotenv this?
inteface_path = os.path.join(os.path.dirname(
    os.path.dirname(os.path.abspath(__file__))), 'interface.json')
services_path = os.path.join(os.path.dirname(
    os.path.dirname(os.path.abspath(__file__))), 'services.json')


with open(inteface_path, "r", encoding="utf-8") as f:
    interface_schema = json.load(f)
with open(services_path, "r", encoding="utf-8") as f:
    services_schema = json.load(f)
parser: Parser = Parser(interface_schema, services_schema)
socket: Socket
class Person(BaseModel):
    obj_name = "Person"
    def __init__(self, name: str = "", age: int = 0, height: float = 0.0, is_singaporean: bool = True):    
        super().__init__()
        self.name = name
        self.age = age
        self.height = height
        self.is_singaporean = is_singaporean

at_least_once = True
while True:
    if at_least_once:
        socket = AtLeastOnceSocket(parser)
    else:
        socket = AtLeastOnceSocket(parser)
    socket.listen()
    request_id = uuid4()
    request = Person("Biboo", 18, 142, False)
    response = socket.send(request, 1, True)
    logger.info(response)
    time.sleep(2)
    socket.close()
    at_least_once = not at_least_once
