import struct
from uuid import UUID
import uuid

from src.comm.types import ACKObj, BaseModel, ErrorObj, RequestType, UnmarshalResult


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

    def get_request_type(self, receive_byte: int) -> RequestType:
        return RequestType(receive_byte)

    def unmarshall(self, recv_bytes: bytes) -> UnmarshalResult:
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
        request_id = uuid.UUID(bytes=recv_bytes[:16])

        service_id = int.from_bytes(recv_bytes[16:18], byteorder='big')

        request_type = self.get_request_type(recv_bytes[18])
        if request_type == RequestType.REQUEST or request_type == RequestType.RESPONSE:
            return self._unmarshal_normal(recv_bytes, request_id, service_id, request_type)
        elif request_type == RequestType.ERROR:
            return self._unmarshal_error(recv_bytes, request_id, service_id)
        elif request_type == RequestType.ACK:
            return self._unmarshal_ack(request_id, service_id)

    def _unmarshal_error(self, recv_bytes: bytes, request_id: uuid.UUID, service_id: int) -> UnmarshalResult:
        error_message = recv_bytes[19:].decode("utf-8")
        return UnmarshalResult(ErrorObj(error_message), request_id, service_id, RequestType.ERROR)

    def _unmarshal_normal(self, recv_bytes: bytes, request_id: uuid.UUID, service_id: int, request_type: RequestType) -> UnmarshalResult:
        data_format = self.data[self.services[service_id]
                                [request_type.label()]]

        class_name = data_format["name"]
        available_classes = {
            cls.obj_name: cls for cls in BaseModel.__subclasses__() if cls.obj_name}
        if class_name not in available_classes:
            raise ValueError(f"Class {class_name} not found")

        obj = available_classes[class_name]()

        class_name = data_format["name"]
        available_classes = {
            cls.obj_name: cls for cls in BaseModel.__subclasses__() if cls.obj_name}
        if class_name not in available_classes:
            raise ValueError(f"Class {class_name} not found")

        obj = available_classes[class_name]()

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
        return UnmarshalResult(obj, request_id, service_id, request_type)

    def _unmarshal_ack(self, request_id: uuid.UUID, service_id: int) -> UnmarshalResult:
        return UnmarshalResult(ACKObj(), request_id, service_id, RequestType.ACK)

    def marshall(self, request_id: UUID, service_id: int, request_type: RequestType, item: BaseModel) -> bytes:
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
        if request_type == RequestType.ERROR:
            return self._marshal_error(request_id, service_id, item)
        elif request_type == RequestType.REQUEST or request_type == RequestType.RESPONSE:
            return self._marshal_normal(request_id, service_id, request_type, item)
        elif request_type == RequestType.ACK:
            return self._marshal_ack(request_id, service_id)
        else:
            raise ValueError("Invalid request type")

    def _marshal_error(self, request_id: UUID, service_id: int, item: ErrorObj) -> bytes:
        error_message = item.errorMessage.encode("utf-8")
        return request_id.bytes + service_id.to_bytes(2, byteorder='big') + RequestType.ERROR.to_bytes(1, byteorder='big') + error_message

    def _marshal_normal(self, request_id: UUID, service_id: int, request_type: RequestType, item: BaseModel) -> bytes:
        data_format = self.data[item.obj_name]

        fields = data_format['fields']
        fields_ptr = 0

        generated_bytes = request_id.bytes
        generated_bytes += service_id.to_bytes(2, byteorder='big')
        generated_bytes += request_type.to_bytes(1, byteorder='big')

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
                    generated_bytes += (1 if item[field_name]
                                        else 0).to_bytes(1, byteorder='big')
            fields_ptr += 1
        return generated_bytes

    def _marshal_ack(self, request_id: UUID, service_id: int) -> bytes:
        return request_id.bytes + service_id.to_bytes(2, byteorder='big') + RequestType.ACK.to_bytes(1, byteorder='big')
