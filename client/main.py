import json
import logging
import os
import time
from uuid import uuid4
from src.comm.types import BaseModel
from src.comm.socket import AtMostOnceSocket, Socket, AtLeastOnceSocket
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


class BookFacilityReq(BaseModel):
    obj_name = "BookFacilityReq"
    def __init__(self, facility_name: str, timeSlot: str):    
        super().__init__()
        self.facilityName = facility_name
        self.timeSlot = timeSlot

at_least_once = True
while True:
    if at_least_once:
        socket = AtLeastOnceSocket(parser)
    else:
        socket = AtMostOnceSocket(parser)
    request_id = uuid4()
    request = BookFacilityReq("Library", "Mon,19,00 - Mon,20,00")
    response = socket.send(request, 1, True)
    logger.info(response)
    time.sleep(2)
    socket.close()
    at_least_once = not at_least_once
