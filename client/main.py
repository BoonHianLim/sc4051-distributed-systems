import json
import os
import time
from uuid import uuid4
from src.comm.socket import Socket, AtLeastOnceSocket, AtMostOnceSocket
from src.comm.parser import Parser

schema = None
inteface_path = os.path.join(os.path.dirname(
    os.path.dirname(os.path.abspath(__file__))), 'interface.json')
with open(inteface_path, "r", encoding="utf-8") as f:
    schema = json.load(f)
parser: Parser = Parser(schema)
socket: Socket
data = {
    "name": "Biboo",
    "age": 21,
    "height": 151.12
}
at_least_once = True
while True:
    if at_least_once:
        socket = AtLeastOnceSocket(parser)
    else:
        socket = AtMostOnceSocket(parser)
    request_id = uuid4()
    response = socket.send(data, "Person", "Person")
    print(response)
    time.sleep(2)
    socket.close()
    at_least_once = not at_least_once
