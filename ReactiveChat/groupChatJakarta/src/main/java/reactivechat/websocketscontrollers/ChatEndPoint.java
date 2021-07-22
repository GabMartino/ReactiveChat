package reactivechat.websocketscontrollers;


import jakarta.ejb.EJB;
import reactivechat.ejb.DBRequestHandler2;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Startup;
import jakarta.ejb.Stateless;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Startup
@ServerEndpoint(value = "/chatEndpoint/{username}")
@Stateless
public class ChatEndPoint implements Serializable {

    @Inject
    private QueueSenderSessionBean senderBean;

    @Inject
    private DBRequestHandler2 dbRequestHandler;


    //<userID, session.getID>
    Map<String,Session>  userIDsessionmap = new ConcurrentHashMap<>();

    Map<String, ArrayList<String>> roomsToUsers = new HashMap<>();

    public ChatEndPoint() {
    }

    public Session getUserSession(String userID){
        return userIDsessionmap.get(userID);

    }
    @PostConstruct
    public void initRoomSet(){
        //System.out.println(this.getClass() + "INITIALIZATION CHAT SET ");
        JSONObject resp = dbRequestHandler.fetchAllChatsWithPartecipants();
        if(resp != null){
            try {
                JSONArray chat_list_with_partecipants = (JSONArray)(resp.get("data"));
                if(chat_list_with_partecipants != null){
                    System.out.println(this.getClass() +  " " + chat_list_with_partecipants.toString());
                    for(int i = 0; i< chat_list_with_partecipants.length(); i++){
                        JSONObject o =  ((JSONObject)chat_list_with_partecipants.get(i));
                        ArrayList<String> couple = new ArrayList<>();
                        couple.add((String) o.get("userA"));
                        couple.add((String) o.get("userB"));
                        roomsToUsers.put((String) o.get("roomID"), couple);

                    }
                }
            }catch (JSONException e){
                //do nothing
            }
        }

    }
    public void printRooms(){
        Iterator it = roomsToUsers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            System.out.println(pair.getKey() + " = " +((ArrayList) pair.getValue()).get(0) + " " + ((ArrayList) pair.getValue()).get(1));

        }
    }
    public void printIDUsersConnected(){
        Iterator it = userIDsessionmap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            System.out.println(pair.getKey() + " = " + pair.getValue());

        }
    }
    @OnOpen
    public void messageOpen(Session session, @PathParam("username") String userID) throws IOException{

        userIDsessionmap.put(userID, session); //insert mapping session -> iduser

        System.out.println("CREATED NEW WEB SOCKET FOR USERID : " + userID);

    }

    @OnClose
    public void close(Session session) {
        System.out.println("Closing session...");
        Iterator it = userIDsessionmap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            if(pair.getValue().equals(session)){
                userIDsessionmap.remove(pair.getKey());//to do
                System.out.println("Session Closed.");
                break;
            }

        }


    }

    @OnMessage
    public void messageReceiver(Session session,
                                String message) throws IOException, EncodeException {
        try {
            //System.out.println(this.getClass() + " new message" + message);
            JSONObject m = new JSONObject(message);
            JSONObject mcontent = (JSONObject) m.get("value");
            String roomDestination = mcontent.get("destination").toString();
            String sourceId = mcontent.get("source").toString();
            String msg = mcontent.get("message").toString();

            dbRequestHandler.insertNewMessage(roomDestination, sourceId, msg);

        }catch (JSONException e){
            e.printStackTrace();
        }

    }
    @OnError
    public void onError(Session session, Throwable throwable) {

        System.out.println("There has been an error with session " + session.getId() );
        throwable.printStackTrace();
        close(session);
    }


    public void forwardMessage(JSONObject m){
        if(m.getString("event").compareTo("new_message") == 0){
            //System.out.println(m.toString());
            String roomID = m.get("roomID").toString();

            ArrayList<String> couple = roomsToUsers.get(roomID);
            //printRooms();
            if(couple != null){
                String destinationID = null;
                for( String u : couple){
                    String sender = m.get("senderID").toString();
                    if(u.compareTo(sender) != 0){
                        destinationID = u;
                        break;
                    }
                }
                if(destinationID != null){
                    JSONObject message = new JSONObject();
                    message.put("event", "new_message");
                    message.put("roomID", m.get("roomID"));
                    message.put("senderID", m.get("senderID"));
                    message.put("msg", m.get("message"));
                    try {

                        Session session = getUserSession(destinationID);
                        if(session != null){
                            //System.out.println(this.getClass() + " SENDING MESSAGE TO THE OTHER USER"+ destinationID);
                            session.getBasicRemote().sendObject(message);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (EncodeException e) {
                        e.printStackTrace();
                    }
                }
            }


        }else if (m.getString("event").compareTo("new_chat") == 0){
            String destinationID = m.get("user").toString();

            Session session = getUserSession(destinationID);
            if(session != null){
                JSONObject message = new JSONObject();
                message.put("event", "chat_refresh");
                //System.out.println(this.getClass() + " SENDING MESSAGE TO THE OTHER USER"+ destinationID);
                try {
                    session.getBasicRemote().sendObject(message);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (EncodeException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    public void onJMSMessage(@Observes @WSJMSMessage Message m){
        try {
            JSONObject msg =  new JSONObject(m.getBody(String.class));
            System.out.println("new event received " + msg.toString());
            if(msg.get("event").toString().compareTo("new_chat") == 0){
                initRoomSet();
                forwardMessage(msg);
            }else{

                forwardMessage(msg);
            }
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

}
