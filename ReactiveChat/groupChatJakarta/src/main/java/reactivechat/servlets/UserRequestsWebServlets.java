package reactivechat.servlets;

import jakarta.ejb.EJB;
import reactivechat.ejb.DBRequestHandler2;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(
        urlPatterns = {
                "/fetchChat", "/startchat", "/usersList"
        })

public class UserRequestsWebServlets extends HttpServlet {

    @Inject
    private DBRequestHandler2 dbRequestHandler;



    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //super.doPost(req, resp);
        JSONObject body = toJson(req.getReader());
        if(body != null){
            JSONObject res = new JSONObject();
            if(body.get("request").toString().compareTo("fetchChat") == 0){
                //System.out.println(this.getClass().toString() + "FETCH CHAT REQUEST");
                res = handleFetchChatRequest(body.get("value").toString());
            }else if(body.get("request").toString().compareTo("startChat") == 0){
                //System.out.println(this.getClass().toString() + "START CHAT REQUEST");
                res = handleStartChat(body);

            }else if(body.get("request").toString().compareTo("usersList") == 0){
                res = handleUsersListRequest();
            }else{

                res.put("response", "requestNotHandled");
                res.put("value", "404");

            }
            //System.out.println(res);
            sendBackAJson(res,resp);
        }



    }


    private void sendBackAJson(JSONObject o, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        PrintWriter out = res.getWriter();
        out.print(o.toString());
        out.flush();

    }
    private JSONObject toJson(BufferedReader b){
        JSONObject o = null;
        try{
            JSONTokener tokener = new JSONTokener(b);
            o = new JSONObject(tokener);
        }catch (Exception e){
            //no Json BODY
        }


        return o;
    }



    private JSONObject handleStartChat( JSONObject in ){
        //System.out.println(this.getClass().toString() + " STARTCHAT REQUEST");
        JSONArray values = in.getJSONArray("value");
        String firstID = values.getString(0);
        String secondID = values.getString(1);

        //try in
        return dbRequestHandler.startOrFetchChat(firstID, secondID);
    }
    private JSONObject handleUsersListRequest(){
        //System.out.println(this.getClass().toString() + " USERLIST REQUEST");
        return dbRequestHandler.fetchAllUsers();

    }
    private JSONObject handleFetchChatRequest(String userID){
        //System.out.println(this.getClass().toString() + " FETCH CHAT REQUEST");
        return dbRequestHandler.fetchAllChatOfAUser(userID);

    }
    private JSONObject handleFetchMessages( JSONObject in ){
        //System.out.println(this.getClass().toString() + " FETCH MESSAGES REQUEST");
        String roomID = in.get("value").toString();

        //try in
        return dbRequestHandler.selectAllMessagesOfRoom(roomID);
    }


}
