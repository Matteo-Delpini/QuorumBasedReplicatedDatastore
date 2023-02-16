# Quorum Based Distributed Datastore
DS optional project - Matteo Delpini, Alessandro D'Alberti

## How to use
We provided a shell script that allows you to start the coordinator, two replicas and a client, all on the same host. Alternatively, if you want to start the different components yourself or you want to test the components in a distributed setting, follow these instructions:<br>
1. Start the coordinator with the command
    ```java -jar coordinator.jar <port-number>```<br>
The port number will serve for replicas and clients to connect to the coordinator
2. Start a client with the command ```java -jar client.jar <coordinator-ip-address> <coordinator-port>``` or start a replica with the command ```java -jar replica.jar <coordinator-ip-address> <coordinator-port> <registry-name>```<br>
The registry name must be unique amongst all the replicas that live on the same host (two replicas can have the same name only if they live on different hosts)
3. You started all the necessary components and can use the client to make get or put requests, the coordinator to change the ```read-threshold``` and ```write-threshold``` parameters, or the replica to see its local copy of the database
