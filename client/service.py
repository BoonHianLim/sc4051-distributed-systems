from enum import IntEnum
import logging
import os
import json
import time
from socket import timeout
from typing import Optional

from src.comm.types import BookFacilityReq, BookFacilityResp, CancelBookingReq, CancelBookingResp, EditBookingReq, EditBookingResp, ListAvailabilityReq, ListAvailabilityResp, NotifyCallbackReq, RegisterCallbackReq, RegisterCallbackResp, RequestType, SocketLostType, SocketSwitchingReq, UnmarshalResult
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
socket = None
bind_port = 11999
while socket is None:
    try:
        socket: Socket = AtLeastOnceSocket(parser, port=bind_port)
    except OSError as e:
        if "Address already in use" in str(e) or "Only one usage" in str(e):
            # Address is already in use, try again after 1 second
            print(f"Port {bind_port} is already in use. Retrying...")
            bind_port += 1
            socket = None
        else:
            # Some other error occurred, raise it
            raise e
    except Exception as e:
        # Some other error occurred, raise it
        raise e
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

DAYS_STR = ['Mon', 'Tue',
            'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
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
8. Modify Packet Loss Settings
9. Exit
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
        LOSE_PACKET = 8

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
            result = ",".join([DAYS_STR[i - 1] for i in days])
            if user_confirmation == 1:
                logger.info("User confirmed query.")
                print("Sending query request...")
                request = ListAvailabilityReq(facility_name, result)
                response, err = socket.send(request, 1, RequestType.REQUEST)
                if err is not None:
                    logger.error(err)
                    print(f"Receive error from server: {err.errorMessage}")
                else:
                    availability_response: ListAvailabilityResp = response
                    logger.info(availability_response)
                    availabilities_list = availability_response.availabilities.split(
                        ":")
                    for availability in availabilities_list:
                        print(availability)
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
            time_slot = f"{DAYS_STR[start_day - 1]},{start_hour},{start_minute} - {DAYS_STR[end_day - 1]},{end_hour},{end_minute}"
            logger.info(
                f"User entered: {facility_name} with time slot {time_slot}")
            user_confirmation = safe_input(
                f"You have selected to booking {facility_name} for {time_slot}. Press 1 to continue.", "int")
            if user_confirmation == 1:
                logger.info("User confirmed booking.")
                print("Sending booking request...")
                request = BookFacilityReq(facility_name, time_slot)
                response, err = socket.send(
                    request, 2, RequestType.REQUEST)
                if err is not None:
                    logger.error(err)
                    print(f"Receive error from server: {err.errorMessage}")
                else:
                    booking_response: BookFacilityResp = response
                    logger.info(booking_response)
                    print(
                        f"Booking successful! Confirmation ID: {booking_response.confirmationID}")
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
                response, err = socket.send(
                    request, 3,  RequestType.REQUEST)
                if err is not None:
                    logger.error(err)
                    print(f"Receive error from server: {err.errorMessage}")
                else:
                    change_response: EditBookingResp = response
                    logger.info(change_response)
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
                response, err = socket.send(
                    request, 4, RequestType.REQUEST)
                current_time = time.time()
                end_time = current_time + (monitoring_period_in_minutes * 60)
                if err is not None:
                    logger.error(err)
                    print(f"Receive error from server: {err.errorMessage}")
                else:
                    listen_response: RegisterCallbackResp = response
                    logger.info(listen_response)
                    print(
                        f"Listening successful! Start listening now for {monitoring_period_in_minutes} minutes.")
                    while time.time() < end_time:
                        # Busy wait for notifications
                        response: Optional[UnmarshalResult] = socket.non_blocking_listen()
                        logger.info(response)
                        if response:
                            notify_request: NotifyCallbackReq = response.obj
                            print(
                                f"Notification received for {notify_request.availabilities}.")

                    print(
                        f"Listening period of {monitoring_period_in_minutes} minutes has ended. Stopping listening.")
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
                response, err = socket.send(
                    request, 6, RequestType.REQUEST)
                if err is not None:
                    logger.error(err)
                    print(f"Receive error from server: {err.errorMessage}")
                else:
                    cancel_response: CancelBookingResp = response
                    logger.info(cancel_response)
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
                response, err = socket.send(
                    request, 7, RequestType.REQUEST)
                if err is not None:
                    logger.error(err)
                    print(f"Receive error from server: {err.errorMessage}")
                else:
                    extend_response: EditBookingResp = response
                    logger.info(extend_response)
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
                request = SocketSwitchingReq("AtMostOnceSocket" if isinstance(
                    socket, AtLeastOnceSocket) else "AtLeastOnceSocket")
                response, err = socket.send(
                    request, 8, RequestType.REQUEST)
                if err is not None:
                    logger.error(err)
                    print(f"Receive error from server: {err.errorMessage}")
                    continue
                socket_switch_response: SocketSwitchingReq = response
                logger.info(socket_switch_response)
                socket.close()
                at_least_once = not at_least_once
                socket = None
                while socket is None:
                    try:
                        if at_least_once:
                            socket = AtLeastOnceSocket(parser)
                        else:
                            socket = AtMostOnceSocket(parser)
                    except OSError as e:
                        if "Address already in use" in str(e):
                            # Address is already in use, try again after 1 second
                            print(
                                f"Port {bind_port} is already in use. Retrying...")
                            bind_port += 1
                            socket = None
                        else:
                            # Some other error occurred, raise it
                            raise e
                    except Exception as e:
                        # Some other error occurred, raise it
                        raise e
                print(f"Successfully changed socket type to {str(socket)}")
            else:
                logger.info("User cancelled socket type change. Current socket type is still AtLeastOnceSocket" if isinstance(
                    socket, AtLeastOnceSocket) else "User cancelled socket type change. Current socket type is still AtMostOnceSocket")
                print("Operation cancelled.")
            print()
        case BookingOptions.LOSE_PACKET:
            logger.info("User selected option 8: Modify Packet Loss Settings")
            print(f"Current packet loss settings: {socket.loss_type.label()} with loss rate {socket.loss_rate}")
            user_confirmation = safe_input(
                "Do you want to modify the packet loss settings? Press 1 to continue.", "int")
            if user_confirmation == 1:
                loss_type = safe_input(
                    "Enter the packet loss type (0 for lost in client to server, 1 for lost in server to client, 2 for mixed):", "int", min_val=0, max_val=2)
                loss_rate = safe_input(
                    "Enter the packet loss rate (0.0–1.0):", "float", min_val=0.0, max_val=1.0)
                socket.loss_type = SocketLostType(loss_type)
                socket.loss_rate = loss_rate
                print(f"Successfully changed packet loss settings to {socket.loss_type.label()} with loss rate {socket.loss_rate}")
            else:
                logger.info("User cancelled packet loss settings change.")
                print("Operation cancelled.")
            print()
        case _:
            print("Goodbye!")
            break
