from enum import IntEnum
import logging
import os
import json
from src.comm.types import BookFacilityReq, CancelBookingReq, EditBookingReq, ListAvailabilityReq, RegisterCallbackReq
from src.comm.parser import Parser
from src.comm.socket import AtLeastOnceSocket, AtMostOnceSocket, Socket
from src.utils.logger import setup_logger
from src.utils.input import safe_input

logger = logging.getLogger(__name__)

setup_logger(std_debug_level=1000)  # prevent logs from being written to stdout

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
socket: Socket = AtLeastOnceSocket(parser)
at_least_once = True

print(r'''
   ___                     _               _  _                    
  | _ )    ___     ___    | |__     o O O | \| |    ___   __ __ __ 
  | _ \   / _ \   / _ \   | / /    o      | .` |   / _ \  \ V  V / 
  |___/   \___/   \___/   |_\_\   TS__[O] |_|\_|   \___/   \_/\_/  
_|"""""|_|"""""|_|"""""|_|"""""| {======|_|"""""|_|"""""|_|"""""|  
"`-0-0-'"`-0-0-'"`-0-0-'"`-0-0-'./o--000'"`-0-0-'"`-0-0-'"`-0-0-'  
      
Welcome to Book Now. We are a facility booking service that allows you to book facilities in your area.
We have a wide range of facilities available for booking, such as sports facilities, meeting rooms, and more.
''')

# Main Event Loop
while True:
    print("""Select an option:
1. Query for availabilities
2. Book a facility
3. Change a booking
4. Listen for notifications
5. Cancel a booking
6. Extends a booking
7. Switch Socket Type
8. Exit
    """)

    options = safe_input("Enter an option: ", "int")

    class BookingOptions(IntEnum):
        QUERY = 1
        BOOK = 2
        CHANGE = 3
        LISTEN = 4
        CANCEL = 5
        EXTEND = 6
        SWITCH = 7
    match options:
        case BookingOptions.QUERY:
            logger.info("User selected option 1: Query for availabilities")
            facility_name = safe_input("Enter the facility name: ", "str")
            days = set()
            while True:
                print("Current days entered:", days)
                day = safe_input(
                    "Enter the day 1–7, where 1 = Mon and 7 = Sun. (Enter 0 to stop entering days.)", "int", min_val=0, max_val=7)
                if day == 0:
                    break
                days.add(day)
            logger.info(f"User entered: {facility_name}, {days}")
            user_confirmation = safe_input(
                f"You have selected to query for {facility_name} on {days}. Press 1 to continue.", "int")
            days_str = ['Mon', 'Tue',
                        'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
            result = ",".join([days_str[i - 1] for i in days])
            if user_confirmation == 1:
                logger.info("User confirmed query.")
                print("Sending query request...")
                request = ListAvailabilityReq(facility_name, result)
                response = socket.send(request, 1, True)
                logger.info(response)
                print("Query successful!")
            else:
                logger.info("User cancelled query.")
                print("Operation cancelled.")
            print()
        case BookingOptions.BOOK:
            logger.info("User selected option 2: Book a facility")
            facility_name = safe_input("Enter the facility name: ", "str")
            start_day = safe_input(
                "Enter the start day (1–7, where 1 = Mon and 7 = Sun):", "int", min_val=1, max_val=7)
            start_hour = safe_input(
                "Enter the start hour (0–23):", "int", min_val=0, max_val=23)
            start_minute = safe_input(
                "Enter the start minute (0–59):", "int", min_val=0, max_val=59)
            end_day = safe_input(
                "Enter the end day (1–7, where 1 = Mon and 7 = Sun):", "int", min_val=1, max_val=7)
            end_hour = safe_input(
                "Enter the end hour (0–23):", "int", min_val=0, max_val=23)
            end_minute = safe_input(
                "Enter the end minute (0–59):", "int", min_val=0, max_val=59)
            logger.info(
                f"User entered: {facility_name}, {start_day}, {start_hour}, {start_minute}, {end_day}, {end_hour}, {end_minute}")
            days_str = ['Mon', 'Tue',
                        'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
            time_slot =  f"{days_str[start_day - 1]},{start_hour},{start_minute} - {days_str[end_day - 1]},{end_hour},{end_minute}"
            user_confirmation = safe_input(
                f"You have selected to booking {facility_name} for time_slot {time_slot}. Press 1 to continue.", "int")
            if user_confirmation == 1:
                logger.info("User confirmed booking.")
                print("Sending booking request...")
                request = BookFacilityReq(facility_name, time_slot)
                response = socket.send(request, 2, True)
                logger.info(response)
                print("Booking successful!")
            else:
                logger.info("User cancelled booking.")
                print("Operation cancelled.")
            print()
        case BookingOptions.CHANGE:
            logger.info("User selected option 3: Change a booking")
            confirmation_id = safe_input(
                "Enter the confirmation ID of the booking you want to change:", "str")
            minute_offset = safe_input(
                "Enter the minute offset you want to change by:", "int", min_val=0)
            logger.info(
                f"User entered: {confirmation_id}, {minute_offset}")
            user_confirmation = safe_input(
                f"You have selected to change booking {confirmation_id} by {minute_offset} minutes. Press 1 to continue.", "int")
            if user_confirmation == 1:
                logger.info("User confirmed change.")
                print("Sending change request...")
                request = EditBookingReq(confirmation_id, minute_offset)
                response = socket.send(request, 3, True)
                logger.info(response)
                print("Change successful!")
            else:
                logger.info("User cancelled change.")
                print("Operation cancelled.")
            print()
        case BookingOptions.LISTEN:
            logger.info("User selected option 4: Listen for notifications")
            facility_name = safe_input(
                "Enter the facility name you want to listen for notifications:", "str")
            monitoring_period_in_minutes = safe_input(
                "Enter the monitoring period in minutes:", "int", min_val=0)
            logger.info(
                f"User entered: {facility_name}, {monitoring_period_in_minutes}")
            user_confirmation = safe_input(
                f"You have selected to listen for notifications for {facility_name} for {monitoring_period_in_minutes} minutes. Press 1 to continue.", "int")
            if user_confirmation == 1:
                logger.info("User confirmed listen.")
                print("Sending listen request...")
                request = RegisterCallbackReq(
                    facility_name, monitoring_period_in_minutes)
                response = socket.send(request, 4, True)
                logger.info(response)
                print("Listening successful!")
            else:
                logger.info("User cancelled listen.")
                print("Operation cancelled.")
            print()
        case BookingOptions.CANCEL:
            logger.info("User selected option 5: Cancel a booking")
            confirmation_id = safe_input(
                "Enter the confirmation ID of the booking you want to cancel:", "str")
            logger.info(f"User entered: {confirmation_id}")
            user_confirmation = safe_input(
                f"You have selected to cancel booking {confirmation_id}. Press 1 to continue.", "int")
            if user_confirmation == 1:
                logger.info("User confirmed cancel.")
                print("Sending cancel request...")
                request = CancelBookingReq(confirmation_id)
                response = socket.send(request, 5, True)
                logger.info(response)
                print("Cancel successful!")
            else:
                logger.info("User cancelled cancel.")
                print("Operation cancelled.")
            print()
        case BookingOptions.EXTEND:
            logger.info("User selected option 6: Extend a booking")
            confirmation_id = safe_input(
                "Enter the confirmation ID of the booking you want to extend:", "str")
            minute_offset = safe_input(
                "Enter the minute offset you want to extend by:", "int", min_val=0)
            logger.info(
                f"User entered: {confirmation_id}, {minute_offset}")
            user_confirmation = safe_input(
                f"You have selected to extend booking {confirmation_id} by {minute_offset} minutes. Press 1 to continue.", "int")
            if user_confirmation == 1:
                logger.info("User confirmed extend.")
                print("Sending extend request...")
                request = EditBookingReq(confirmation_id, minute_offset)
                response = socket.send(request, 6, True)
                logger.info(response)
                print("Extend successful!")
            else:
                logger.info("User cancelled extend.")
                print("Operation cancelled.")
            print()
        case BookingOptions.SWITCH:
            logger.info("User selected option 7: Switch Socket Type")
            print("Current socket type is AtLeastOnceSocket" if isinstance(
                socket, AtLeastOnceSocket) else "Current socket type is AtMostOnceSocket")
            user_confirmation = safe_input(
                "Do you want to switch the socket type? Press 1 to continue.", "int")
            if user_confirmation == 1:
                socket.close()
                at_least_once = not at_least_once
                if at_least_once:
                    socket = AtLeastOnceSocket(parser, port=12000)
                else:
                    socket = AtMostOnceSocket(parser, port=12000)
                print("Successfully changed socket type to AtLeastOnceSocket" if at_least_once else "Successfully changed socket type to AtMostOnceSocket")
            else:
                logger.info("User cancelled socket type change. Current socket type is still AtLeastOnceSocket" if isinstance(
                    socket, AtLeastOnceSocket) else "User cancelled socket type change. Current socket type is still AtMostOnceSocket")
                print("Operation cancelled.")
            print()
        case _:
            print("Goodbye!")
            break
