CPSC 559 Project README

System Requirements:
	- Java 17
	- IntelliJ (Optional but recommended)

We have zipped up our entire intellij project for your convenience. If you would like access to just the source code you can find it under the src directory of the unzipped project. Inside the src/ directory you will find the following packages: 

RegistryOperations: Contains code responsible for starting the RMI server and handling registry operations like bind/unbind.

BackEndServiceServer: Contains code responsible for creating remote server object and implementation.

Client: Contains code responsible for handling client interaction

Proxy: Contains code responsible for creating remote proxy object and implementation.

In order to run the code using intelliJ, the following 4 java classes containing main methods need to be run in the specified order

1.) RMIRegistry.java
2.) BackendServiceServer.java
3.) ProxyServer.java
4.) Client.java

Please note that for files 1-3 you will need to put the your own machines private IP wherever you see the following line: System.setProperty("java.rmi.server.hostname", "[IPV4 HERE]");

For files 2-4 you will need to put the IP of the machine running the RMI server in the following line: Registry registry = LocateRegistry.getRegistry("[IPV4 HERE]", 1099);

If you are trying to run the code from the command line you will have to handle compiling and running the classes that are in different packages.
