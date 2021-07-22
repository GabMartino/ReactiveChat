
-module(db_server).

-export([start_server/1]).
-import(db_manager, [start/0,  traverse_table_and_show/1, add_new_node_to_db/1]).
-export([wait_for_requests/1, exec/1]).

send_response(OpId, Value, SenderProcess, SenderNode) ->
    io:format("Server: Sending response to ~s ~s ~n", [SenderProcess,SenderNode]),
    {SenderProcess, SenderNode} ! {OpId, Value, db_server, node()}.



sendMessage(Message, Process, Node) ->
  {Process, Node} ! {event, Message, self(), node()}.

broadcast_to_nodes(Event) ->
  io:format("Broadcasting event to all erlang cluster~n"),
  [sendMessage(Event,db_server, N) || N <- nodes() ],
  ok.

sendEventServlet([],Event) ->
  ok;

sendEventServlet([H | L], Event) ->
  Process = element(2,H),
  Node = element(3, H),
  io:format("Sending Event to ~p ~p ~n",[Process, Node]), {Process, Node} ! {event, Event, self(), node()},
  sendEventServlet(L, Event).

sendEvent(EventListernerTable, Event) ->
  List = ets:lookup(EventListernerTable, event_listener),
  sendEventServlet(List, Event).



wait_for_requests(EventListernerTable) ->
    %%io:format("Server: Waiting new request~n"),
    receive
        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% CHECKED %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        %%%%%%%%%%%% RETURNS {response_insert_user, ID | user_already_present , SenderProcess, SenderNode}
        {insert_user, Username, SenderProcess, SenderNode} -> io:format("Server: inserting_new_user from : ~p ~p ~n", [SenderProcess, SenderNode]), 
                                                            R = db_manager:insert_user(Username), 
                                                            send_response(response_insert_user, R, SenderProcess, SenderNode) ;
        %%%%%%%%%%%% RETURNS {response_check_user, not_found | {Username, ID}, SenderProcess, SenderNode}
        {check_user_by_id, UserID, SenderProcess, SenderNode} -> io:format("Server: check_user_by_id from : ~p ~p ~n",[SenderProcess, SenderNode]),
                                                                  R = db_manager:check_user_id(UserID),
                                                                  send_response(response_check_user_by_ID, R, SenderProcess, SenderNode);



        %%%%%%%%%%%% RETURNS {response_check_user, not_found | {Username, ID}, SenderProcess, SenderNode}
        {check_user_by_name, Username, SenderProcess, SenderNode} -> io:format("Server: check_user_by_name from : ~p ~p ~n",[SenderProcess, SenderNode]),
                                                                      R = db_manager:check_user_name(Username),
                                                                      send_response(response_check_user_by_name, R, SenderProcess, SenderNode);

        {select_all_chats_of_a_user,UserID, SenderProcess, SenderNode} -> io:format("Server: select_all_chat_of_a_user from : ~p ~p ~n",[SenderProcess, SenderNode]),
                                                                            send_response(response_select_all_chats_of_a_user, db_manager:select_all_chats_of_a_user(UserID), SenderProcess, SenderNode);



        {select_all_chat, SenderProcess, SenderNode} -> io:format("Server: check_user_by_name from : ~p ~p ~n",[SenderProcess, SenderNode]),
                                                                            send_response(response_select_all_chat, db_manager:select_all_chats(), SenderProcess, SenderNode);


        {select_all_chat_with_partecipants, SenderProcess, SenderNode} -> io:format("Server: check_user_by_name from : ~p ~p ~n",[SenderProcess, SenderNode]),
                                                      send_response(response_select_all_chat_with_partecipants, db_manager:select_all_chats_with_partecipants(), SenderProcess, SenderNode);

        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%




        %%%%%%%%%%%% RETURNS {response_create_group, ID | room_already_present , SenderProcess, SenderNode}
        {create_group, RoomName, SenderProcess, SenderNode} -> io:format("Server: inserting_new_room from : ~p ~p ~n",[SenderProcess, SenderNode]), 
                                                            R = db_manager:create_new_room(RoomName),
                                                            send_response(response_create_group, R, SenderProcess, SenderNode);




        {check_group_by_name, RoomName, SenderProcess, SenderNode} -> io:format("Server: checking_group from : ~p ~p ~n",[SenderProcess, SenderNode]),
                                                                    R = db_manager:check_room_name(RoomName),
                                                                    send_response(response_check_room_by_name, R, SenderProcess, SenderNode);

        {check_group_by_id, RoomID, SenderProcess, SenderNode} -> R = db_manager:check_group_id(RoomID),
                                                                  send_response(response_check_room_by_ID, R, SenderProcess, SenderNode);


        %%%%%%%%%%%% RETURNS {response_insert_new_message, no_partecipant_to_room_or_wrong_room_name | {Username, ID}, SenderProcess, SenderNode}
        {insert_new_message, RoomId, UserId, Message, SenderProcess, SenderNode} -> R = db_manager:add_message(RoomId, UserId, Message),

                                                                                        send_response(response_insert_new_message ,R, SenderProcess, SenderNode),
                                                                                        io:format("~p ~n", [R]),
                                                                                        if
                                                                                          R == ok -> Event = {insert_new_message, db_manager:return_the_number(RoomId), db_manager:return_the_number(UserId), Message},
                                                                                                      io:format("Event ~p ~n",[Event]),
                                                                                                      broadcast_to_nodes(Event),
                                                                                                      sendEvent(EventListernerTable,Event);
                                                                                          true -> ok
                                                                                        end;



        {insert_partecipant_to_group, RoomId, UserID, SenderProcess, SenderNode} -> io:format("Server: inserting_new_partecipant from : ~p ~p ~n",[SenderProcess, SenderNode]),
                                                                                       R = db_manager:add_partecipant_to_group(RoomId, UserID),
                                                                                        send_response(response_inserting_partecipant, R, SenderProcess, SenderNode) ;
        
        
        {select_all_users, SenderProcess, SenderNode} -> send_response(response_select_all_users, db_manager:select_all_users(), SenderProcess, SenderNode);

        {select_n_messages, RoomName, N, SenderProcess, SenderNode} -> io:format("select_messages~n"), send_response(response_select_n_messages, db_manager:fetch_last_N_messages_of_room(RoomName, N), SenderProcess, SenderNode);

        {select_all_messages_of_a_room, RoomID,SenderProcess, SenderNode} -> io:format("select_messages~n"), send_response(response_select_messages, db_manager:fetch_all_messages_of_room(RoomID), SenderProcess, SenderNode);

        


        {select_all_partecipants_to_room, RoomID, SenderProcess, SenderNode} -> send_response(response_select_all_partecipants_to_room, db_manager:select_partecipant_to_room(RoomID), SenderProcess, SenderNode);


        {start_new_chat, UserIDA, UserIDB, SenderProcess, SenderNode } -> send_response(response_start_chat, db_manager:fetch_or_create_new_chat(UserIDA, UserIDB), SenderProcess, SenderNode),
                                                                           Event = {new_chat, db_manager:return_the_number(UserIDA) , db_manager:return_the_number(UserIDB)},
                                                                          io:format("Event ~p ~n",[Event]),
                                                                          broadcast_to_nodes(Event),
                                                                          sendEvent(EventListernerTable,Event);

        {join_to_cluster_db, SenderProcess, SenderNode}  -> io:format("Received request of join to db cluster~n"),
                                                            R = add_new_node_to_db(SenderNode), 
                                                            send_response(response_join_to_cluster_db, R, SenderProcess, SenderNode); %%adding new node request


        {event, Message, SenderProcess, SenderNode} -> io:format("Received event from ~p ~p ~n",[Message, self()]), sendEvent(EventListernerTable, Message);
        {register_me, SenderProcess, SenderNode} -> io:format("Registering event Listener ~p  ~p~n", [SenderProcess, SenderNode]),
                                                    ets:insert(EventListernerTable, {event_listener, SenderProcess, SenderNode}),
                                                    List = ets:lookup(EventListernerTable, event_listener),
                                                    io:format("List of registered event listeners: ~p ~n",[List]),
                                                    send_response(response_register_me, ok, SenderProcess, SenderNode);

        {_Unexpected, SenderProcess, SenderNode} -> io:format("Received event from ~p ~n",[_Unexpected]), send_response(504, "not_valid_message", SenderProcess, SenderNode)
       
    end,
    wait_for_requests(EventListernerTable).



connect_me_to_db_creator(MainNodeName) ->
  io:format("Request connection to DB initiator~n"),

  mnesia:start(),
  {db_server, MainNodeName} ! {join_to_cluster_db, db_server, node()},
  io:format("Waiting for response~n"),
  receive
    {response_join_to_cluster_db, Response, SenderProcess, SenderNode} -> io:format("~p~n", [element(1,Response)]),

                                                                            mnesia:change_table_copy_type(schema, node(), disc_copies),
                                                                            [{Tb, mnesia:add_table_copy(Tb, node(), disc_copies)}
                                                                              || {Tb, [{MainNodeName, disc_copies}]} <- [{Tb, mnesia:table_info(Tb, where_to_commit)}
                                                                              || Tb <- mnesia:system_info(tables)]];

    _ -> io:format("Received something~n")
  end.
exec(Node) ->
    case Node  of 
        none -> io:format("MAIN SERVER: THIS IS THE NODE INITIATOR~n"),
                  db_manager:start();

        _ -> %%connect this node to the initiation server
                connect_me_to_db_creator(Node)

    end,
  EventListernerTable = ets:new(event_listeners_table, [bag]),
  wait_for_requests(EventListernerTable).



start_server(NodeToConnect)->
    %% if NodeToConnect is none this is the main server 
    %% if NodeToConnect has a name that is the main server and request to connect
    
    S = spawn(?MODULE,exec, [NodeToConnect]),
    register(db_server,S),
    io:format("Server Executing...~n").




