# ReactiveChat
Reactive Chat is an implementation of a distributed architecture able to offer soft-real time features.# Reactive Chat

Reactive chat is an implementation of a distributed architecture able to offer soft-real time features.

## Project Folders and scripts

### Docker containers
### Database app container
```bash
Container_erlang_node_with_dbapp\Dockerfile
```
This dockerfile creates a container of Erlang over Alpine and imports the erlang modules for the DB server.
```bash
Container_erlang_node_with_dbapp\ReactiveChatDBErlang
```
Folder of the Erlang DB app. This will be copied in the container.
### Java Web Server container
```bash
Container_payara_node_with_app\Dockerfile
```
This dockerfile creates a container with payara (JDK-11) and install Erlang.
```bash
Container_payara_node_with_app\artifacts
```
The artifact folder contains the deployed application.

### Project folders

```bash
DBDistributedServer
```
Project folder of the erlang db app (IntelliJ)
```bash
Frontend
```
Project folder of the React Web application
(Reinstall node modules to use it)
```bash
ReactiveChat
```
Project folder of WebServer application

### Scripts
```bash
execute_single_node_infrastructure.sh
```
This script execute one web server and one Erlang node for the DB
```bash
execute_distributed_infrastructure.sh
```
This script execute two web server and five Erlang nodes for the DB.



## Notes for the correct usage

- Erlang OTP version:24;
- Import manually the Jinterface 12.x jar library in the web server app project. (The library is in the Erlang installation folder of your system);
- Use JDK-11 
- When using the two scripts 
```bash
execute_single_node_infrastructure.sh
```
and 
```bash
execute_distributed_infrastructure.sh
```
keep an eye on the file 
```bash
Container_payara_node_with_app\artifacts\ReactiveChat\WEB-INF\classes\config.properties
```

```bash
actualNodeName=java_server
erlangProcessName=db_server
dbServerName1=server1@erlang_node
dbServerName2=server2@erlang_node2
dbServerName3=server3@erlang_node3
dbServerName4=server4@erlang_node4
dbServerName5=server5@erlang_node5
```
When using the the single one infrastructure comment the all the erlang node but the first one (The master).

## Little problems and workaround

- The payara server node containers to correctly use the JInterface library need to execute the EPMD (pseudo-DNS for the erlang nodes). This needs to be executed before the deployment of the application. To solve this problem is necessary to change the Dockerfile executing the EPMD before the execution of the server or forcely executing it before the deployment finished (as I did).
- The web server makes instances of the classes sometimes not considering the JAVA EE annotations. This because it considers also old version already deployed or multiple instances for the distributed handling of the EJBs. This unfortunaly can cause a multiple creation of the internal Erlang nodes with JInterface with the same name but different socket port, overwriting the informations of the EPMD. This almost surely brings to connection problems with the other erlang nodes. To avoid this problem a more precise managing of the erlang node creation in JAVA should be set. A little workaround to avoid this problem is to delete all the other versions of the application deployed in the folder:
```bash
Container_payara_node_with_app\artifacts
```
just leaving the main one.
