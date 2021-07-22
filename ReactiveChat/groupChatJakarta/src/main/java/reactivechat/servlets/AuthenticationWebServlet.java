package reactivechat.servlets;


import jakarta.ejb.EJB;
import reactivechat.ejb.DBRequestHandler2;
import reactivechat.sessionUtils.SessionHandler;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

@WebServlet(
        urlPatterns = {
            "/login", "/signup", "/logout"
        })
public class AuthenticationWebServlet extends HttpServlet {


    @Inject
    private DBRequestHandler2 dbRequestHandler;



    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //fetch url of request
        String path = req.getRequestURI().substring(req.getContextPath().length());
        switch ( path ) {

            case "/login" : // 1) called the first time, not logged no cookies no payload -> error login
                            // 2) called with no payload but with cookie -> check and ok
                            // 3) called with payload but no cookie -> check, ok and send cookie
                            sendBackAJson(handleLogin(req, resp), resp);
                            break;
            case "/signup":  sendBackAJson(handleSignup(req, resp), resp);

                            break;

            case "/logout": sendBackAJson(handleLogout(req, resp), resp);
                                break;
            default:   JSONObject res = new JSONObject();
                            res.put("response", "requestNotHandled");
                            res.put("value", "404");
                            res.put("data", path);
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


    private JSONObject handleLogout(HttpServletRequest req, HttpServletResponse res) throws IOException {
        //System.out.println(this.getClass().toString() + " LOGOUT REQUEST");
        JSONObject body = toJson(req.getReader());
        JSONObject o = new JSONObject();
        o.put("response", "logoutUser" );
        //System.out.println("Logout attempt");
        if(body != null && body.get("request").toString().compareTo("logoutUser") == 0){
            //set cookie to delete
            if(SessionHandler.removeSession(body.get("value").toString())){

                o.put("value", "ok");
            }else{
                o.put("value", "error");
            }

        }else{
            o.put("value", "Error: Payload doesn't match with the logout request.");
        }
        Cookie loginCookie = null;
        Cookie[] cookies = req.getCookies();
        if(cookies != null){
            for(Cookie cookie : cookies){
                if(cookie.getName().equals("loginCookie")){
                    loginCookie = cookie;
                    break;
                }
            }
        }
        if(loginCookie != null){
            loginCookie.setMaxAge(0);
            res.addCookie(loginCookie);
        }
        return o;
    }


    private JSONObject checkLogin(String username, boolean id){
        //System.out.println(this.getClass().toString() + " CHECK LOGIN");
        JSONObject db_resp = null;
        if(!id){
            db_resp = dbRequestHandler.checkUserByName(username);
        }else{
            db_resp = dbRequestHandler.checkUserByID(username);
        }

        if( db_resp != null && db_resp.getString("value").compareTo("ok") == 0){
            //System.out.println("Adding Session");
            SessionHandler.addSession(username);
        }
        return db_resp;
    }
    private JSONObject handleLogin(HttpServletRequest req, HttpServletResponse res) throws IOException {
       // System.out.println(this.getClass().toString() + " LOGIN REQUEST");
        //Prepare response
        JSONObject o = null;
        //o.put("response", "loginUser" );
        JSONObject body = toJson(req.getReader());

        if(body != null){
            String requestType = body.get("request").toString();
            String username = body.get("value").toString();
                if(requestType.compareTo("loginUser") == 0){//check match request payload

                    o = checkLogin(username, false);
                    System.out.println(username);
                }else{
                    o = new JSONObject();
                    o.put("value", "error");//Error request
                }
        }else {

            String loginCookieValue = checkLoginCookie(req);
            if (loginCookieValue != null) {
                o = checkLogin(loginCookieValue, true);//adding session is implicit
            } else {
                o = new JSONObject();
                o.put("value", "error");//Error request
            }
        }
        o.put("response", "loginUser" );
        return  o;
    }

    private String checkLoginCookie(HttpServletRequest req){
        //System.out.println(this.getClass().toString() + "CHECK LOGIN COOKIE");
        Cookie loginCookie = null;
        Cookie[] cookies = req.getCookies();
        if(cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("loginCookie")) {
                    loginCookie = cookie;
                    break;
                }
            }
        }
        if(loginCookie != null){
            return loginCookie.getValue();
        }
        return null;

    }

    private JSONObject handleSignup(HttpServletRequest req, HttpServletResponse resp) throws IOException {
       // System.out.println(this.getClass().toString() + " SIGNUP REQUEST");
        JSONObject o = new JSONObject();
        o.put("response", "registerUser" );
        JSONObject body = toJson(req.getReader());
        if(body != null){
            String username = body.get("value").toString();
            o =  dbRequestHandler.insertNewUser(username);
        }else{
            o = new JSONObject();
            o.put("value", "error");//Error request
        }
        return  o;

    }
}
