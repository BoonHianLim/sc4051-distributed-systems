import time
from socket import *

pings = 1

# Send ping 10 times
while pings < 11:

    # Create a UDP socket
    clientSocket = socket(AF_INET, SOCK_DGRAM)

    # Ping to server
    textMessage = 'test'
    message = str.encode(textMessage)
    message += bytes([115])
    addr = ("127.0.0.1", 12000)

    # Send ping
    start = time.time()
    clientSocket.sendto(message, addr)

    # If data is received back from server, print
    try:
        data, server = clientSocket.recvfrom(1024)
        end = time.time()
        elapsed = end - start
        # This prints: b'\x48\x65\x6c\x6c\x6f'
        print("Byte values:", list(data))
        print(f"Raw bytes received: {data}")


    # If data is not received back from server, print it has timed out
    except timeout:
        print('REQUEST TIMED OUT')

    pings = pings - 1

    time.sleep(1)
