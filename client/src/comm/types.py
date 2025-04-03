from enum import IntEnum


class BaseModel():
    obj_name = ""

    def __init__(self):
        pass

    def __getitem__(self, key):
        return getattr(self, key)

    def __setitem__(self, key, value):
        setattr(self, key, value)

    def __repr__(self):
        attrs = vars(self)  # returns a dict of all instance attributes
        return f"{self.__class__.__name__}({', '.join(f'{k}={v}' for k, v in attrs.items())})"

    def __eq__(self, value):
        if not isinstance(value, self.__class__):
            return False
        return vars(self) == vars(value)


class RequestType(IntEnum):
    REQUEST = 0
    RESPONSE = 1
    ERROR = 2
    ACK = 3

    def label(self):
        match self:
            case RequestType.REQUEST:
                return "request"
            case RequestType.RESPONSE:
                return "response"
            case RequestType.ERROR:
                return "error"
            case RequestType.ACK:
                return "ack"

class SocketLostType(IntEnum):
    LOST_IN_CLIENT_TO_SERVER = 0
    LOST_IN_SERVER_TO_CLIENT = 1
    ACK = 2

    def label(self):
        match self:
            case SocketLostType.LOST_IN_CLIENT_TO_SERVER:
                return "lost in client to server"
            case SocketLostType.LOST_IN_SERVER_TO_CLIENT:
                return "lost in server to client"
            case SocketLostType.ACK:
                return "ack"

class UnmarshalResult():
    def __init__(self, obj: BaseModel, request_id: str, service_id: int, request_type: RequestType):
        self.obj = obj
        self.request_id = request_id
        self.service_id = service_id
        self.request_type = request_type

    def __eq__(self, value):
        if not isinstance(value, self.__class__):
            return False
        return self.obj == value.obj and self.request_id == value.request_id and self.service_id == value.service_id and self.request_type == value.request_type

class ErrorObj(BaseModel):
    obj_name = "ErrorObj"

    def __init__(self, error_message: str = ""):
        super().__init__()
        self.errorMessage = error_message

class ACKObj(BaseModel):    
    obj_name = "ACKObj"

    def __init__(self):
        super().__init__()

class ListAvailabilityReq(BaseModel):
    obj_name = "ListAvailabilityReq"

    def __init__(self, facility_name: str = "", days: str = ""):
        super().__init__()
        self.facilityName = facility_name
        self.days = days


class ListAvailabilityResp(BaseModel):
    obj_name = "ListAvailabilityResp"

    def __init__(self, availabilities: str = ""):
        super().__init__()
        self.availabilities = availabilities


class BookFacilityReq(BaseModel):
    obj_name = "BookFacilityReq"

    def __init__(self, facility_name: str = "", time_slot: str = ""):
        super().__init__()
        self.facilityName = facility_name
        self.timeSlot = time_slot

class BookFacilityResp(BaseModel):
    obj_name = "BookFacilityResp"

    def __init__(self, confirmation_id: str = ""):
        super().__init__()
        self.confirmationID = confirmation_id


class EditBookingReq(BaseModel):
    obj_name = "EditBookingReq"

    def __init__(self, confirmation_id: str = "", minute_offset: int = 0):
        super().__init__()
        self.confirmationID = confirmation_id
        self.minuteOffset = minute_offset

class EditBookingResp(BaseModel):
    obj_name = "EditBookingResp"

    def __init__(self, success: bool = False):
        super().__init__()
        self.success = success

class RegisterCallbackReq(BaseModel):
    obj_name = "RegisterCallbackReq"

    def __init__(self, facility_name: str = "", monitoring_period_in_minutes: int = 0):
        super().__init__()
        self.facilityName = facility_name
        self.monitoringPeriodInMinutes = monitoring_period_in_minutes


class RegisterCallbackResp(BaseModel):
    obj_name = "RegisterCallbackResp"

    def __init__(self, success: bool = False):
        super().__init__()
        self.success = success

class NotifyCallbackReq(BaseModel):
    obj_name = "NotifyCallbackReq"

    def __init__(self, availabilities: str = ""):
        super().__init__()
        self.availabilities = availabilities

class NotifyCallbackResp(BaseModel):
    def __init__(self, success: bool = False):
        super().__init__()
        self.success = success


class CancelBookingReq(BaseModel):
    obj_name = "CancelBookingReq"

    def __init__(self, confirmation_id: str = ""):
        super().__init__()
        self.confirmationID = confirmation_id

class CancelBookingResp(BaseModel):
    obj_name = "CancelBookingResp"

    def __init__(self, success: bool = False):
        super().__init__()
        self.success = success

class ExtendBookingReq(BaseModel):
    obj_name = "ExtendBookingReq"

    def __init__(self, confirmation_id: str = "", minute_offset: int = 0):
        super().__init__()
        self.confirmationID = confirmation_id
        self.minuteOffset = minute_offset

class ExtendBookingResp(BaseModel):
    obj_name = "ExtendBookingResp"

    def __init__(self, success: bool = False):
        super().__init__()
        self.success = success

class SocketSwitchingReq(BaseModel):
    obj_name = "SocketSwitchingReq"

    def __init__(self, switch: str = ""):
        super().__init__()
        self.switch = switch
    
class SocketSwitchingResp(BaseModel):
    obj_name = "SocketSwitchingResp"

    def __init__(self, message: bool = False):
        super().__init__()
        self.message = message
