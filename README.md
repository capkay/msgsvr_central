# msgsvr_central
Multi-client centralized messaging server
README:

The code is developed in java. 

Files : 

1. PClient.java : is where all the user interface logic and also the communication requests are implemented. Each request is sent to the  server along with the relevant data that the server needs for the specific request. Also, the data that the client receives from the server is also processed and displayed to the user. A custom protocol is implemented for smooth communication with the server which can be seen in the design section.

2. PServer.java : implements a class that takes care of running the server socket and creates threads for each client connection. The server also handles mutual exclusion between shared resources between the multiple threads during a session. The server also prints logging messages of what each client connected to the server is doing with timestamps. The server communicates with the client using a custom protocol that sends some messages followed by relevant data, where the message represents a query/request from the client. These can be viewed in the design section.

3. Data.java : is simply a class definition which will be used to create objects that store message related data ( From address, to address, message & timestamp) . This is used by both the server and client to store/retrieve messages.

To run the server:

java PServer <port_number>

To run a client:

java PClient <host> <port_number>
