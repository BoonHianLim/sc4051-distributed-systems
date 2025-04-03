# SC4051: Distributed System Project: Facility Booking System

As part of SC4051: Distributed System requirement, we have implemented a distributed facility booking application with client-server architecture that allows users to book facilities and manage bookings. The system implements reliable communication using UDP sockets with different message delivery guarantees.

## Project Structure

The project consists of two main components:

- **Client**: A Python application that provides a command-line interface for users to interact with the booking system
- **Server**: A Java application that manages the facility booking service, handles requests, and maintains the state of facilities and bookings

```
facility-booking-system/
├── client/               # Python client application
│   ├── src/              # Source code for the client
│   │   ├── comm/         # Communication modules
│   │   ├── tests/        # Client tests
│   │   └── utils/        # Utility modules
│   ├── main.py           # Client entry point for automated testing
│   ├── service.py        # Main client application
│   └── requirements.txt  # Python dependencies
├── dspserver/            # Java server application
│   ├── src/              # Source code for the server
│   │   ├── main/         # Main server code
│   │   └── test/         # Server tests
│   ├── pom.xml           # Maven dependencies
│   └── logback.xml       # Logging configuration
├── interface.json        # Interface definition shared between client and server
├── services.json         # Service definition shared between client and server
├── runclient.sh          # Script to run the client
└── runserver.sh          # Script to run the server
```
## Tech Stack
[![Tech Stack](https://skillicons.dev/icons?i=java,py&perline=3)](https://skillicons.dev)

## Features

- **Facility Management**: Create and manage different types of facilities
- **Availability Listing**: Query available time slots for facilities
- **Booking Management**: Book, edit, extend, and cancel facility bookings
- **Callback Notifications**: Register for notifications about facility availability changes
- **Reliable Communication**: Two socket types with different delivery guarantees:
  - **At-Least-Once**: Ensures messages are delivered at least once, even if there are network issues
  - **At-Most-Once**: Ensures messages are processed exactly once, preventing duplicate processing
- **Custom Communication Protocol**: Implements a custom binary protocol for client-server communication
- **Socket Type Switching**: Dynamically switch between delivery semantics during operation

## Supported Operations

- **List Availability**: Query available time slots for a facility on specific days
- **Book Facility**: Book a facility for a specific time slot
- **Edit Booking**: Change the timing of an existing booking
- **Register Callback**: Register to receive notifications about facility availability changes
- **Cancel Booking**: Cancel an existing booking
- **Extend Booking**: Extend the duration of an existing booking
- **Switch Socket Type**: Change between At-Least-Once and At-Most-Once delivery semantics

## Prerequisites

### For Client
- Python 3.10 or higher
- Virtual environment (recommended)

### For Server
- Java 17 or higher
- Maven

## Setup Instructions

### Setting up the Server

1. Navigate to the server directory:
   ```
   cd dspserver
   ```

2. Build the server using Maven:
   ```
   mvn clean package
   ```

3. Run the server:
   ```
   java -jar target/dspserver-1.0-SNAPSHOT.jar
   ```
   
   Alternatively, use the provided script:
   ```
   ./runserver.sh
   ```

### Setting up the Client

1. Navigate to the client directory:
   ```
   cd client
   ```

2. Create and activate a virtual environment:
   ```
   python -m venv venv
   
   # On Windows
   venv\Scripts\activate
   
   # On macOS/Linux
   source venv/bin/activate
   ```

3. Install dependencies:
   ```
   pip install -r requirements.txt
   ```

4. Run the client:
   ```
   python service.py
   ```
   
   Alternatively, use the provided script:
   ```
   ./runclient.sh
   ```

## Communication Protocol

The system uses a custom binary protocol for client-server communication defined in `interface.json` and `services.json`. This protocol includes:

- **Request/Response Messages**: Each service has defined request and response formats
- **Error Handling**: Error messages with descriptive text
- **Acknowledgments**: ACK messages for confirming receipt in at-most-once delivery

## Available Facilities

The server initializes with the following facilities:
- Gym
- Pool
- Spa
- Event Hall
- Lounge

## Running Tests

### Client Tests
```
cd client
python -m unittest discover ./
```

### Server Tests
```
cd dspserver
mvn test
```

## Socket Delivery Guarantees

### At-Least-Once Socket (AtLeastOnceSocket)
- Ensures that messages are delivered at least once to the recipient
- May result in duplicate message delivery
- Suitable for operations where processing the same message multiple times is acceptable

### At-Most-Once Socket (AtMostOnceSocket)
- Ensures that messages are delivered at most once to the recipient
- Prevents duplicate message processing
- Uses acknowledgments to confirm message receipt
- Suitable for operations where duplicate processing must be avoided
