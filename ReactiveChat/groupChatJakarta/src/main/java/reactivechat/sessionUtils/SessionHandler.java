package reactivechat.sessionUtils;


import java.util.ArrayList;

public class SessionHandler {

    static ArrayList<String> sessions = new ArrayList<>();


    public static boolean addSession(String s){

        for( String user : sessions){
            if(user.compareTo(s) == 0){
                return false;
            }
        }

        return sessions.add(s);

    }
    public static boolean removeSession(String s){
        return sessions.removeIf( session -> session.compareTo(s) == 0);
    }

    public static boolean checkSession(String s){
        if(sessions.stream().filter( session -> session.compareTo(s) == 0).findAny().orElse(null) == null){
            return false;
        }else{
            return true;
        }
    }

}
