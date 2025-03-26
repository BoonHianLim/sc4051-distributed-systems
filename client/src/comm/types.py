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


class UnmarshalResult():
    def __init__(self, obj: BaseModel, request_id: str, service_id: int, is_request: bool):
        self.obj = obj
        self.request_id = request_id
        self.service_id = service_id
        self.is_request = is_request

    def __eq__(self, value):
        if not isinstance(value, self.__class__):
            return False
        return self.obj == value.obj and self.request_id == value.request_id and self.service_id == value.service_id and self.is_request == value.is_request


class ListAvailabilityReq(BaseModel):
    obj_name = "ListAvailabilityReq"

    def __init__(self, facility_name: str = "", days: str = ""):
        super().__init__()
        self.facilityName = facility_name
        self.days = days


class ListAvailabilityResp(BaseModel):
    obj_name = "ListAvailabilityResp"

    def __init__(self, availabilities: str):
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

    def __init__(self, facility_name: str = ""):
        super().__init__()
        self.facilityName = facility_name

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