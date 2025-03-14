class BaseModel():
    obj_name = ""
    def __init__(self):
        pass
    
    def __getitem__(self, key):
        return getattr(self, key)