class BaseModel():
    obj_name = ""

    def __init__(self):
        pass

    def __getitem__(self, key):
        return getattr(self, key)

    def __repr__(self):
        attrs = vars(self)  # returns a dict of all instance attributes
        return f"{self.__class__.__name__}({', '.join(f'{k}={v}' for k, v in attrs.items())})"


class ListAvailabilityReq(BaseModel):
    obj_name = "ListAvailabilityReq"

    def __init__(self, facility_name: str = "", days: str = ""):
        super().__init__()
        self.facilityName = facility_name
        self.days = days


class BookFacilityReq(BaseModel):
    obj_name = "BookFacilityReq"

    def __init__(self, facility_name: str = "", time_slot: str = ""):
        super().__init__()
        self.facilityName = facility_name
        self.timeSlot = time_slot

class EditBookingReq(BaseModel):
    obj_name = "EditBookingReq"
    def __init__(self, confirmation_id: str = "", minute_offset: int = 0):
        super().__init__()
        self.confirmationID = confirmation_id
        self.minuteOffset = minute_offset

class RegisterCallbackReq(BaseModel):
    obj_name = "RegisterCallbackReq"
    def __init__(self, facility_name: str = "", monitoring_period_in_minutes: int = 0):
        super().__init__()
        self.facilityName = facility_name
        self.monitoringPeriodInMinutes = monitoring_period_in_minutes

class NotifyCallbackResp(BaseModel):
    def __init__(self, success: bool = False):
        super().__init__()
        self.success = success

class CancelBookingReq(BaseModel):
    obj_name = "CancelBookingReq"
    def __init__(self, confirmation_id: str = ""):
        super().__init__()
        self.confirmationID = confirmation_id

class ExtendBookingReq(BaseModel):
    obj_name = "ExtendBookingReq"
    def __init__(self, confirmation_id: str = "", minute_offset: int = 0):
        super().__init__()
        self.confirmationID = confirmation_id
        self.minuteOffset = minute_offset