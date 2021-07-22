package reactivechat.websocketscontrollers;

import com.ericsson.otp.erlang.*;
import reactivechat.ejb.ConfigReader;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Startup
@Singleton
public class ErlangEventHandler {

    @Inject
    private QueueSenderSessionBean senderBean;

    boolean waken_up = false;

    static volatile int nodeCounter = 1;
    static volatile OtpNode _node;
    static volatile OtpMbox _mbox;
    static volatile boolean  connected = false;
    static volatile boolean  nodeCreated = false;
    static String _processName = "socket";
    static String _nodeName = "java_server_socket";
    static volatile String serverNodeName = null;
    static volatile Set<String> _serverListNames = new HashSet<>();
    static String serverProcessName = "db_server";
    static int index = 0;
    static long timeout = 1000;
    static long timeout_receive = 2000;
    Thread receiveThread = new Thread(() -> receive());
    public ErlangEventHandler() {

    }

    @PostConstruct
    private synchronized void init(){
        if(!nodeCreated){
            System.out.println(this.getClass() + "INITIALIZATION EVENT HANDLER");
            readConfigFromFile("config.properties");
            resetNewNode();
            tryToConnectToAvailableServers();
            registerMe();
            receiveThread.start();
        }
    }
    synchronized private static void readConfigFromFile(String filename){
        if(serverNodeName == null){
            ConfigReader reader = new ConfigReader(filename);
            try {
                String indexDBServers = "dbServerName";
                int c = 1;
                String name = indexDBServers.concat(String.valueOf(c));
                while (reader.getProperty(name) != null) {
                    _serverListNames.add(reader.getProperty(name));
                    c++;
                    name = indexDBServers.concat(String.valueOf(c));
                }
                serverNodeName = (String)_serverListNames.toArray()[(int)(Math.random()*_serverListNames.size())];
            }catch (Exception e){
                e.printStackTrace();
            }
        }

    }

    synchronized private static void resetNewNode(){
        if(_node != null){
            _node.close();
            _mbox.close();
        }
        try {
            OtpEpmd.unPublishPort(_node);
            _node = new OtpNode(_nodeName, "cluster");//create node with a specific name
            System.out.println("REGISTERED NEW ERLANG NODE " + _node.alive() + " "  + _node.node() + "PORT :" + _node.port());
            _mbox = _node.createMbox(_processName + nodeCounter);
            if(_mbox == null){
                nodeCounter++;
                _mbox = _node.createMbox(_processName + nodeCounter);
            }
            System.out.println("REGISTERED NEW ERLANG MBOX " + _mbox.getName());
            nodeCounter++;
            nodeCreated = true;
            OtpEpmd.publishPort(_node);
        }catch (IOException e) {
            e.printStackTrace();
        }

    }
    synchronized public OtpErlangTuple setMessage(String... objs){
        OtpErlangObject[] msg = new OtpErlangObject[objs.length];
        for(int i = 0 ; i< objs.length ;i++){
            msg[i] = new OtpErlangAtom(objs[i]);
        }

        return new OtpErlangTuple(msg);
    }

    public synchronized JSONObject registerMe() {
        try{
            System.out.println(Thread.currentThread().getId());
            _mbox.send(serverProcessName,serverNodeName, setMessage("register_me", _mbox.getName(), _node.node()) );
            OtpErlangObject o =  _mbox.receive(timeout_receive);
            if(o != null){
                OtpErlangTuple t = (OtpErlangTuple) o;
                if(t.elementAt(1).toString().compareTo("ok") == 0){
                    System.out.println("EVENT LISTENER REGISTERED");
                }
            }
        }catch (OtpErlangDecodeException | OtpErlangExit e){
            e.printStackTrace();
        }

        return null;

    }
    synchronized private void tryToConnectToAvailableServers(){
        int counter = 3;
        while(_node != null && !_node.ping(serverNodeName,timeout) && counter > 0){
            System.out.println(this.getClass() + ": Impossible to connect to remote node " + serverNodeName + " attempt: " + (3 - counter));
            counter--;
        }
        if(!_node.ping(serverNodeName,timeout)){
            System.out.println(this.getClass() + ": Impossible to connect to remote node " + serverNodeName + " ERROR");
        }else{

            System.out.println(this.getClass() + ": Connection Established with " + serverNodeName + " from " + _node.alive());
        }


    }

    public void receive() {
        try {

            while (true){
                //System.out.println("Waiting for a new message event on " + _mbox.getName() + " " + _node.node());
                JSONObject o = new JSONObject();
                OtpErlangObject r = _mbox.receive();
                if(r != null){
                    OtpErlangTuple t = (OtpErlangTuple) r;
                    OtpErlangTuple event = (OtpErlangTuple) t.elementAt(1); // {insert_new_message, RoomId, UserId, Message},
                    switch (event.elementAt(0).toString()){
                        case "insert_new_message" :    o.put("event", "new_message");
                                                        o.put("roomID", Integer.parseInt(event.elementAt(1).toString()));
                                                        o.put("senderID", Integer.parseInt(event.elementAt(2).toString()));
                                                        o.put("message", event.elementAt(3).toString());
                                                        break;
                        case "new_chat":    o.put("event", "new_chat");
                                            o.put("senderID", Integer.parseInt(event.elementAt(1).toString()));
                                            o.put("user", Integer.parseInt(event.elementAt(2).toString()));
                                            break;
                    }

                    //System.out.println("Send event back to the Chat End point");
                    senderBean.sendMessage(o);
                }

            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }


    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        OtpEpmd.unPublishPort(_node);
        _node.close();
    }


}
