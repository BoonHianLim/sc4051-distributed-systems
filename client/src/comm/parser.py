import struct
from uuid import UUID
import uuid

from src.comm.object import BaseModel


class Parser():
    """
    A class to parse and marshal data according to a specified schema.
    Attributes:
        data (dict): A dictionary to store the parsed schema information.
    Methods:
        __init__(schema: any):
            Initializes the Parser with the given schema.
        unmarshall(recv_bytes: bytes, obj_name: str) -> dict:
        marshall(obj_name: str, item: any, request_id: UUID) -> bytes:
    """

    def __init__(self, schema: any, services_schema: any):
        self.data = {}

        for obj in schema:
            # parse statically before? / lazy parse later?
            self.data[obj['name']] = {}
            self.data[obj['name']]['name'] = obj['name']
            self.data[obj['name']]['fields'] = [(list(field.keys())[0], list(
                field.values())[0]) for field in obj['fields']]

        self.services = {}
        for obj in services_schema:
            obj_id: int = obj['id']
            self.services[obj_id] = {}
            self.services[obj_id]['name'] = obj['name']
            self.services[obj_id]['request'] = obj['request']
            self.services[obj_id]['response'] = obj['response']

    def unmarshall(self, recv_bytes: bytes) -> dict:
        """
        Unmarshalls the received bytes into a dictionary object based on the specified object name.
        Args:
            recv_bytes (bytes): The received bytes to be unmarshalled.
            obj_name (str): The name of the object format to use for unmarshalling.
        Returns:
            dict: A dictionary containing the unmarshalled data, including a "request_id" field.
        Raises:
            KeyError: If the obj_name is not found in the data format.
            struct.error: If there is an error unpacking the float value.
        """

        obj = {}
        request_id = uuid.UUID(bytes=recv_bytes[:16])

        service_id = int.from_bytes(recv_bytes[16:18], byteorder='big')

        is_request = recv_bytes[18] == 0
        data_format = self.data[self.services[service_id]
                                ["request" if is_request else "response"]]

        bytes_ptr = 19
        fields = data_format["fields"]
        fields_ptr = 0

        while bytes_ptr < len(recv_bytes) and fields_ptr < len(fields):
            field_name, field_type = fields[fields_ptr]
            match field_type:
                case "int":
                    _value = int.from_bytes(
                        recv_bytes[bytes_ptr:bytes_ptr+4], byteorder='big')
                    obj[field_name] = _value
                    bytes_ptr += 4
                case "str":
                    str_len_in_byte = int.from_bytes(
                        recv_bytes[bytes_ptr:bytes_ptr+2], byteorder='big')
                    bytes_ptr += 2
                    _value = recv_bytes[bytes_ptr:bytes_ptr +
                                        str_len_in_byte].decode('utf-8')
                    obj[field_name] = _value
                    bytes_ptr += str_len_in_byte
                case "float":
                    _value = struct.unpack(
                        ">f", recv_bytes[bytes_ptr:bytes_ptr+4])[0]
                    obj[field_name] = _value
                    bytes_ptr += 4
                case "bool":
                    _value = recv_bytes[bytes_ptr] == 1
                    obj[field_name] = _value
                    bytes_ptr += 1
            fields_ptr += 1
        obj["request_id"] = request_id
        obj["service_id"] = service_id 
        obj["is_request"] = is_request
        return obj

    def marshall(self, request_id: UUID, service_id: int, is_request: bool, item: BaseModel) -> bytes:
        """
        Marshalls the given object into a byte stream according to the specified format.
        Args:
            item (BaseModel): The object to be marshalled, represented as a dictionary. 
            Else, it can be a class instance with attributes corresponding to the fields.
            This can be done by implementing __dict__ or __getitem__ method in the class.
            request_id (UUID): The unique identifier for the request.
        Returns:
            bytes: The marshalled byte stream representing the object.
        """

        data_format = self.data[item.obj_name]

        fields = data_format['fields']
        fields_ptr = 0

        generated_bytes = request_id.bytes
        generated_bytes += service_id.to_bytes(2, byteorder='big')
        generated_bytes += (0 if is_request else 1).to_bytes(1, byteorder='big')

        while fields_ptr < len(fields):
            field_name, field_type = fields[fields_ptr]
            match field_type:
                case "int":
                    generated_bytes += item[field_name].to_bytes(
                        4, byteorder='big')
                case "str":
                    _value = item[field_name].encode('utf-8')
                    generated_bytes += len(_value).to_bytes(2, byteorder='big')
                    generated_bytes += _value
                case "float":
                    generated_bytes += struct.pack(">f", item[field_name])
                case "bool":
                    generated_bytes += (1 if item[field_name] else 0).to_bytes(1, byteorder='big')
            fields_ptr += 1
        return generated_bytes
