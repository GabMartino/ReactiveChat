package reactivechat.ejb;

import com.ericsson.otp.erlang.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@Singleton
@Startup
public class DBRequestHandler2 {

    static volatile int nodeCounter = 0;
    static volatile OtpNode _node;
    static volatile OtpMbox _mbox;

    static volatile String _processName = "endPoint";
    static volatile String _nodeName;
    static volatile String _serverProcessName;
    static volatile Set<String> _serverListNames = new HashSet<>();
    static volatile ArrayList<Boolean> _serverListConnections = new ArrayList<>();
    static SchedulingMethod _sched = SchedulingMethod.RoundRobin;

    static int index = 0;
    static  long timeout = 200;
    static long timeout_receive = 2000;



    @PostConstruct
    public synchronized static void init(){
        if(nodeCounter == 0){
            readConfigFromFile("config.properties");
            resetNewNode();
            tryToConnectToAvailableServers();
            nodeCounter++;
        }
    }
    @PreDestroy
    public synchronized static void destroy(){
        _mbox.close();
        _node.close();

    }

    private synchronized static void readConfigFromFile(String filename){
        ConfigReader reader = new ConfigReader(filename);
        try {
            Set<String> propKeys = reader.getAllKeys();
            _serverProcessName = reader.getProperty("erlangProcessName");

            /// get name of the actual node
            _nodeName = reader.getProperty("actualNodeName");
            if (_nodeName == null) {
                _nodeName = "java_server";
            }
            /**
             * All the names are similar with an increment counter
             */
            String indexDBServers = "dbServerName";
            int c = 1;
            String name = indexDBServers.concat(String.valueOf(c));
            while (reader.getProperty(name) != null) {
                _serverListNames.add(reader.getProperty(name));
                c++;
                name = indexDBServers.concat(String.valueOf(c));
            }
            System.out.println("Found " + _serverListNames.size() + " servers");
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private synchronized static void resetNewNode(){
        if(_node != null){
            _node.close();
            _mbox.close();
        }
        try {
            OtpEpmd.unPublishPort(_node);
            for (String i : OtpEpmd.lookupNames()){
                if(i.compareTo(_nodeName) == 0){
                    System.out.println("Error node with this name already created");
                    break;
                }

            }

            _node = new OtpNode(_nodeName, "cluster");//create node with a specific name

            System.out.println("REGISTERED NEW ERLANG NODE " + _node.alive() + " "  + _node.node() + " PORT: "+_node.port());
            _node.registerStatusHandler(new ActualNodeCheck());
            _mbox = _node.createMbox(_processName );
            System.out.println("REGISTERED NEW ERLANG MBOX " + _mbox.getName());
            nodeCounter++;
            OtpEpmd.publishPort(_node);
        }catch (IOException e) {
            _node.close();
            e.printStackTrace();
        }

    }
    private synchronized static void tryToConnectToAvailableServers(){
        for( String sname : _serverListNames) {
            if(_node.ping(sname, timeout)){
                System.out.println("Connection Established with " + sname + " from " + _node.alive());
                _serverListConnections.add(true);
            }else{
                _serverListConnections.add(false);
                System.out.println("Impossible to connect to remote node " + sname);
            }
        }

    }



    static class ActualNodeCheck extends OtpNodeStatus{
        @Override
        public void remoteStatus(String node, boolean up, Object info) {
            super.remoteStatus(node, up, info);
            System.out.println("NODE: " + node + " is " + up + " " + info.toString());
            if(!up && !_serverListConnections.stream().filter(value -> value ==  true).findAny().orElse(false)){
                OtpEpmd.unPublishPort(_node);
                tryToConnectToAvailableServers();

            }
        }

        @Override
        public void localStatus(String node, boolean up, Object info) {
            super.localStatus(node, up, info);
            System.out.println("NODE: " + node + " is " + up + " " + info.toString());
            if(!up && !_serverListConnections.stream().filter(value -> value ==  true).findAny().orElse(false)){
                OtpEpmd.unPublishPort(_node);
                tryToConnectToAvailableServers();
            }
        }
    };


    public static  void printDBServerNames(){
        for( String server : _serverListNames){
            System.out.println(server);
        }

    }
    private static String fetchServerName(){
        switch (_sched){
            case RoundRobin:
                                    return (String) _serverListNames.stream().toArray()[index++ % _serverListNames.size()];

            case FetchFirst:    return (String) _serverListNames.stream().toArray()[0];

        }
        return null;

    }

    /**
     * THIS UTILS METHOD CREATE AN OTPERLANGTUPLE FROM A SET OF STRINGS
     * @param objs
     * @return
     */
    public static OtpErlangTuple setMessage(String... objs){
        OtpErlangObject[] msg = new OtpErlangObject[objs.length];
        for(int i = 0 ; i< objs.length ;i++){
            msg[i] = new OtpErlangAtom(objs[i]);
        }

        return new OtpErlangTuple(msg);
    }

    /**
     * This methods returns a response after sending a message to a mbox
     *  Format of the answer
     *      {
     *          response : "Name of the response" derived from the erlang node
     *          value : errorvalue | { tuple } | ID of the requested object
     *      }
     *  //THIS METHOD DOESN'T CHECK ANY ERROR SIMPLY MAKES REQUEST AND RETURN REPSONSE
     * @return
     * @throws OtpErlangExit
     * @throws OtpErlangDecodeException
     */

    private static JSONObject receiveResponse() throws OtpErlangExit, OtpErlangDecodeException {
        JSONObject resp =  new JSONObject();
        OtpErlangObject o = null;
        try {
            o = _mbox.receive(timeout_receive);
            if(o != null){
                OtpErlangTuple t = (OtpErlangTuple) o;
                //System.out.println(t.toString());
                String db_response = t.elementAt(0).toString();
                resp.put("response", db_response);
                resp.put("value", "ok");
                switch (db_response) {
                    //CHECKED FUNCTIONS
                    case "response_insert_user":
                    case "response_check_user_by_ID":
                    case "response_check_user_by_name":
                    case "response_check_room_by_ID":   try{
                                                            OtpErlangTuple e = (OtpErlangTuple) t.elementAt(1);
                                                            JSONObject elem = new JSONObject();
                                                            elem.put("name", e.elementAt(0));
                                                            elem.put("id", e.elementAt(1));
                                                            resp.put("data", elem);
                                                        }catch (ClassCastException e){
                                                            resp.put("data",  t.elementAt(1).toString());
                                                            resp.put("value", "error");
                                                        }
                                                        break;
                    case "response_select_all_users":   try{
                                                            JSONArray list = new JSONArray();
                                                            OtpErlangList l = ((OtpErlangList) t.elementAt(1));
                                                            for (int i = 0; i< l.arity() ; i++){
                                                                JSONObject r = new JSONObject();
                                                                OtpErlangTuple v = (OtpErlangTuple) l.elementAt(i);
                                                                r.put("name", v.elementAt(2));
                                                                r.put("id", v.elementAt(1));
                                                                list.put(r);
                                                            }
                                                            resp.put("data", list);
                                                        }catch (ClassCastException e){
                                                            resp.put("data",  t.elementAt(1).toString());
                                                            resp.put("value", "error");
                                                        }
                                                        break;


                    case "response_select_all_chats_of_a_user" : try {
                                                                    OtpErlangList l = ((OtpErlangList) t.elementAt(1));
                                                                    JSONArray array = new JSONArray();
                                                                    for(int i = 0; i< l.arity(); i++){
                                                                        OtpErlangTuple e = (OtpErlangTuple) l.elementAt(i);
                                                                        //{ROOMID, USerNAMe, USERID, messages
                                                                        JSONObject elem = new JSONObject();
                                                                        elem.put("roomID", e.elementAt(0));
                                                                        elem.put("username", e.elementAt(1));
                                                                        elem.put("userID", e.elementAt(2));
                                                                        JSONArray messages = new JSONArray();
                                                                        OtpErlangList msg_list = ((OtpErlangList) e.elementAt(3));
                                                                        for(int j= 0; j< msg_list.arity(); j++){
                                                                                JSONObject msg = new JSONObject();
                                                                                OtpErlangTuple d = (OtpErlangTuple) msg_list.elementAt(j);
                                                                                msg.put("msgID",d.elementAt(1));
                                                                                msg.put("userID",d.elementAt(3));
                                                                                msg.put("msg",d.elementAt(4));
                                                                                messages.put(msg);
                                                                        }
                                                                        elem.put("messages", messages);
                                                                        array.put(elem);
                                                                    }
                                                                    resp.put("data", array);
                                                                }catch (ClassCastException e){
                                                                    resp.put("data",  t.elementAt(1).toString());
                                                                    resp.put("value", "error");
                                                                }
                                                                    break;
                    //NOT CHECKED FUNCTIONS



                    case "response_select_all_chat_with_partecipants" :try {
                                                                            OtpErlangList l = ((OtpErlangList) t.elementAt(1));
                                                                            JSONArray array = new JSONArray();
                                                                            for(int i = 0; i< l.arity(); i++){
                                                                                OtpErlangTuple e = (OtpErlangTuple) l.elementAt(i);
                                                                                JSONObject elem = new JSONObject();
                                                                                elem.put("userA",e.elementAt(0).toString());
                                                                                elem.put("userB",e.elementAt(1).toString());
                                                                                elem.put("roomID",e.elementAt(2).toString());
                                                                                array.put(elem);
                                                                            }
                                                                            resp.put("data", array);
                                                                        }catch (ClassCastException e){
                                                                            resp.put("data",  t.elementAt(1).toString());
                                                                            resp.put("value", "error");
                                                                        }
                                                                            break;
                    case "response_select_all_partecipants_to_room":

                                                                    try {
                                                                        OtpErlangList l = ((OtpErlangList) t.elementAt(1));
                                                                        JSONArray array = new JSONArray();
                                                                        for(int i = 0; i< l.arity(); i++){
                                                                            OtpErlangTuple e = (OtpErlangTuple) l.elementAt(i);

                                                                            array.put(e.elementAt(1));
                                                                        }
                                                                        resp.put("data", array);
                                                                    }catch (ClassCastException e){
                                                                        resp.put("data",  t.elementAt(1).toString());
                                                                        resp.put("value", "error");
                                                                    }
                                                                    break;


                    case "response_insert_new_message": break;


                    case "response_select_all_chat" :try{
                                                        OtpErlangList l = ((OtpErlangList) t.elementAt(1));
                                                        JSONArray array = new JSONArray();
                                                        for(int i = 0; i< l.arity(); i++){
                                                            JSONObject elem = new JSONObject();
                                                            OtpErlangTuple e = (OtpErlangTuple) l.elementAt(i);
                                                            elem.put("name",e.elementAt(2));
                                                            elem.put("roomID",e.elementAt(1));
                                                            array.put(elem);
                                                        }
                                                        resp.put("data",array);
                                                    }catch (ClassCastException e){
                                                        resp.put("data",  t.elementAt(1).toString());
                                                        resp.put("value", "error");
                                                    }
                                                        break;


                    case "response_start_chat":     try{
                                                        int id = Integer.parseInt(t.elementAt(1).toString());
                                                        resp.put("data", id);
                                                    }catch (NumberFormatException e){
                                                        resp.put("data", t.elementAt(1).toString());
                                                        resp.put("value", "error");
                                                    }
                                                        break;

                    case "response_select_messages":    try{
                                                            OtpErlangList l = ((OtpErlangList) t.elementAt(1));
                                                            JSONArray array = new JSONArray();
                                                            for(int i = 0; i< l.arity(); i++){
                                                                JSONObject elem = new JSONObject();
                                                                OtpErlangTuple e = (OtpErlangTuple) l.elementAt(i);
                                                                elem.put("userID",e.elementAt(3));
                                                                elem.put("roomID",e.elementAt(2));
                                                                elem.put("msg",e.elementAt(4));
                                                                array.put(elem);
                                                            }
                                                            resp.put("data",array);
                                                        }catch (ClassCastException e){
                                                            resp.put("data",  t.elementAt(1).toString());
                                                            resp.put("value", "error");
                                                        }


                                                        break;





                    default: resp.put("value", "error");

                }
            }else{
                tryToConnectToAvailableServers();
                System.out.println(DBRequestHandler2.class + " Possible disconnection from remote erlang node");
                resp.put("value", "error");
            }
        }catch (Exception e){
            e.printStackTrace();
            resp.put("value", "error");
            return resp;
        }

        return resp;
    }

    /**
     * THIS METHOD REQUIRE A USERNAME AS STRING AND RETURNS A JSONOBJECT OF TYPE
     *  {
     *      response : response_insert_user
     *      value : ok | error
     *      data : {
     *              name: "username"
     *              id:  "id in the DB"
     *      } | NULL IF ERROR
     *
     *  }
     *
     * @param user
     * @return
     */
    public static JSONObject insertNewUser(String user){

        try {
            String serverName = fetchServerName();
            System.out.println("Sending message to "+ _serverProcessName + " " + serverName);
            _mbox.send(_serverProcessName,serverName, setMessage("insert_user", user,  _mbox.getName(), _node.node()) );

            return receiveResponse();
        }catch (OtpErlangDecodeException |  OtpErlangExit e){
            e.printStackTrace();
        }

        return null;


    }

    /**
     * THIS METHOD REQUIRES AN ID OF A USER AND RETURN A JSONOBJECT OF THE TYPE
     * {
     *     response : response_check_user_by_ID
     *     value : ok | error
     *     data : {
     *          name:
     *          id
     *     } | NULL IF ERROR
     *
     * }
     *
     *
     * @param userid
     * @return
     */
    public static JSONObject checkUserByID(String userid)  {
        try {
            String serverName = fetchServerName();
            System.out.println("Sending message to "+ _serverProcessName + " " + serverName);
            _mbox.send(_serverProcessName,serverName, setMessage("check_user_by_id", userid, _mbox.getName(), _node.node()) );
            return receiveResponse();
        }catch (OtpErlangDecodeException | OtpErlangExit | NullPointerException e){
            e.printStackTrace();
        }

        return null;


    }

    /**
     * THIS METHOD REQUIRES A USERNAME OF A USER AND RETURN A JSONOBJECT OF THE TYPE
     * {
     *     response : response_check_user_by_name
     *     value : ok | error
     *     data : {
     *          name:
     *          id
     *     } | NULL IF ERROR
     *
     * }
     *
     *
     * @param username
     * @return
     */
    public static JSONObject checkUserByName(String username)  {
        try {
            String serverName = fetchServerName();
            System.out.println("Sending message to "+ _serverProcessName + " " + serverName + " waiting from " + _mbox.getName());
            _mbox.send(_serverProcessName,serverName, setMessage("check_user_by_name", username, _mbox.getName(), _node.node()) );
            // not_found || { }
            return receiveResponse();
        }catch (OtpErlangDecodeException | OtpErlangExit e){
            e.printStackTrace();
        }

        return null;


    }

    public static JSONObject insertNewMessage(String roomId, String UserId, String message) {
        try{
            String serverName = fetchServerName();
            System.out.println("Sending message to "+ _serverProcessName + " " + serverName);
            _mbox.send(_serverProcessName,serverName, setMessage("insert_new_message",roomId, UserId, message,   _mbox.getName(), _node.node()) );
            return receiveResponse();
        }catch (OtpErlangDecodeException | OtpErlangExit e){
            e.printStackTrace();
        }

        return null;

    }

    public static JSONObject selectAllMessagesOfRoom(String roomID) {
        try{
            String serverName = fetchServerName();
            System.out.println("Sending message to "+ _serverProcessName + " " + serverName);
            _mbox.send(_serverProcessName,serverName, setMessage("select_all_messages_of_a_room",roomID, _mbox.getName(), _node.node()) );
            return receiveResponse();
        }catch (OtpErlangDecodeException | OtpErlangExit e){
            e.printStackTrace();
        }

        return null;

    }

    public static JSONObject startOrFetchChat(String userIdA, String userIdB) {
        try{
            String serverName = fetchServerName();
            System.out.println("Sending message to "+ _serverProcessName + " " + serverName);
            _mbox.send(_serverProcessName,serverName, setMessage("start_new_chat",userIdA,userIdB,  _mbox.getName(), _node.node()) );
            // error_creating_new_chat || ID
            return receiveResponse();
        }catch (OtpErlangDecodeException | OtpErlangExit e){
            e.printStackTrace();
        }

        return null;

    }

    public static JSONObject fetchAllUsers()  {
        try{
            String serverName = fetchServerName();
            System.out.println("Sending message to "+ _serverProcessName + " " + serverName);
            _mbox.send(_serverProcessName,serverName, setMessage("select_all_users",  _mbox.getName(), _node.node()) );
            // error_creating_new_chat || ID
            return receiveResponse();
        }catch (OtpErlangDecodeException | OtpErlangExit e){
            e.printStackTrace();
        }

        return null;

    }
    public static JSONObject fetchAllChat()  {
        try{
            String serverName = fetchServerName();
            System.out.println("Sending message to "+ _serverProcessName + " " + serverName);
            _mbox.send(_serverProcessName,serverName, setMessage("select_all_chat",  _mbox.getName(), _node.node()) );
            // error_creating_new_chat || ID
            return receiveResponse();
        }catch (OtpErlangDecodeException | OtpErlangExit e){
            e.printStackTrace();
        }

        return null;

    }


    public static JSONObject fetchAllChatsWithPartecipants()  {
        try{
            String serverName = fetchServerName();
            System.out.println("Sending message to "+ _serverProcessName + " " + serverName);
            _mbox.send(_serverProcessName,serverName, setMessage("select_all_chat_with_partecipants",  _mbox.getName(), _node.node()) );
            // error_creating_new_chat || ID
            return receiveResponse();
        }catch (OtpErlangDecodeException | OtpErlangExit e){
            e.printStackTrace();
        }

        return null;

    }

    /**
     * THis methods create a JSON Object with all chat. Generally is called only once
     *  {
     *      response : "response_fetch_all_chat"
     *      value : ok | error
     *      data: [
     *          { //this object is return when the user has already talk to this other user
     *              type: chat
     *              roomID : id
     *              userID : id
     *              messages : [ ... ]
     *          },
     *          { // this object is return when has not been created a chat yet
     *              type : user
     *              userID : id
     *          },
     *          ...
     *      ]
     *  }
     *
     *
     * @return
     */
    public  static JSONObject fetchAllChatOfAUser(String userID){
        try{
            String serverName = fetchServerName();
            System.out.println("Sending message to "+ _serverProcessName + " " + serverName);
            _mbox.send(_serverProcessName,serverName, setMessage("select_all_chats_of_a_user", userID,  _mbox.getName(), _node.node()) );
            // error_creating_new_chat || ID
            return receiveResponse();
        }catch (OtpErlangDecodeException | OtpErlangExit e){
            e.printStackTrace();
        }

        return null;

    }





}
