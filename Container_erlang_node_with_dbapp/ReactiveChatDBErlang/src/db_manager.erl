



-module(db_manager).
-include("db_schema.hrl").
-include_lib("stdlib/include/qlc.hrl").
-define(LOG(X), io:format("{~p,~p}: ~p~n", [?MODULE,?LINE,X])).
-export([start/0,
        select_all_chats/0,
        select_partecipant_to_room/1,
        select_all_users/0,
        fetch_or_create_new_chat/2, 
        fetch_last_N_messages_of_room/2, 
        check_user_name/1,
        check_user_id/1,
        check_group_id/1,
        check_room_name/1,
        traverse_table_and_show/1, 
        insert_user/1, 
        create_new_room/1, 
        add_message/3,
        add_new_node_to_db/1,
        fetch_all_messages_of_room/1,
        select_all_chats_of_a_user/1,select_roommate/2,select_all_chats_with_partecipants/0, return_the_number/1]).

init() ->
    ?LOG(mnesia:create_schema([node()])),
	  ?LOG(mnesia:start()),
	  ?LOG(mnesia:create_table(user,  [{disc_copies, [node()]},{record_name, user},{attributes, record_info(fields,user)}])),
	  ?LOG(mnesia:create_table(room,[{disc_copies, [node()]},{record_name, room},{attributes, record_info(fields,room)}])),
    ?LOG(mnesia:create_table(partecipants,[{disc_copies, [node()]},{record_name, partecipants}, {attributes, record_info(fields,partecipants)}])),
    ?LOG(mnesia:create_table(message,[{disc_copies, [node()]},{record_name, message},{attributes, record_info(fields,message)}])),
    ?LOG(mnesia:create_table(table_id,[{disc_copies, [node()]},{record_name, table_id},{attributes, record_info(fields,table_id)}])),
	  ?LOG(mnesia:wait_for_tables([message,room,partecipants, user, table_id], 2000)),
    %%mnesia:change_table_frag(user, {activate, []}),
    %%mnesia:change_table_frag(room, {activate, []}),
    %%mnesia:change_table_frag(partecipants, {activate, []}),
    %%mnesia:change_table_frag(message, {activate, []}),
    start_counter(user), start_counter(room), start_counter(message),start_counter(partecipants).





%%%start the counter for the autoincrement of IDs in the tables

start_counter(TableName) ->
    Fun = fun() ->
                mnesia:write(table_id, 
                    #table_id{table_name=TableName, last_id=0}, 
                    write
                )
            end,
    mnesia:transaction(Fun).


return_the_string(Value) ->
  if
    is_atom(Value) -> atom_to_list(Value);
    is_list(Value) -> Value;
    true -> Value
  end.




%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%


%%%%%           INSERT NEW USER

%%  RETURN THE ID OF THE USER IF INSERTED OR user_already_present OTHERWISE
insert_user(UserName) ->
    Username_s = return_the_string(UserName),
    Check = check_user_name(Username_s),
    case Check of
        not_found -> Index = mnesia:dirty_update_counter(table_id, user, 1), Uname = #user{id = Index, nameU = Username_s},
                    Fun = fun() ->
                                    mnesia:write(Uname)
                            end,
                    mnesia:transaction(Fun),
                    Index;
        _ -> user_already_present
            
    end.
                
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%


%%%             CREATE A NEW ROOM WITH A ROOMNAME


%% RETURNS {RoomName,Index} | room_already_present
create_new_room(RoomName) ->
    if
      RoomName == "CHAT" -> Index = mnesia:dirty_update_counter(table_id, room, 1),
                            Rname = #room{id = Index, nameR = RoomName},
                            Fun = fun() -> mnesia:write(Rname) end,
                            mnesia:transaction(Fun),
                            {RoomName,Index};
      true -> Check = check_room_name(RoomName),
              case Check of
                not_found ->  Index = mnesia:dirty_update_counter(table_id, room, 1),
                              Rname = #room{id = Index, nameR = RoomName},
                              Fun = fun() -> mnesia:write(Rname) end,
                              mnesia:transaction(Fun),
                              {RoomName,Index};

                _ -> room_already_present

              end
    end.

   
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

%%              ADD PARTECIPANT TO A ROOM

%% RETURNS ok  || error_adding
add_partecipant_to_room(RoomID, UserID) ->
    RoomIDN = return_the_number(RoomID),
    UserIDN = return_the_number(UserID),
    CheckName  = check_user_id(UserIDN),
    case CheckName of
          not_found -> error_adding_check_name;
          _ -> CheckRoomName = check_room_id(RoomIDN),
                case CheckRoomName of
                    not_found -> error_adding_check_room;
                    _ -> Check  = check_partecipant(RoomID, UserID),
                      case Check of
                          found -> ok;
                          not_found ->  Index = mnesia:dirty_update_counter(table_id, partecipants, 1),
                            Part = #partecipants{id = Index, user = UserIDN, room = RoomIDN},
                            Fun = fun() -> mnesia:write(Part) end,
                            R = mnesia:transaction(Fun),
                            element(2, R)
                      end
                end
    end.
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%



%%%%    SELECT LIST of N messages of a room

fetch_last_N_messages_of_room(Room, N) ->
    Q  =qlc:q([M || M <- mnesia:table(message), M#message.room == Room]),
    QC = make_cursor_local(Q),
    Result = get_next_local(QC, N),
    del_cursor_local(QC),
    Result.

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%


%%%%% FETCH ALL THE MESSAGES OF THE ROOM 
fetch_all_messages_of_room(RoomID) ->
    RoomIDn = return_the_number(RoomID),
    H = qlc:q([M || M <- mnesia:table(message), M#message.room == RoomIDn ]),
    R = mnesia:transaction(fun() -> qlc:e(H) end),
    element(2,R).
    


add_message(RoomID, UserID, Message) ->
    RoomIDN = return_the_number(RoomID),
    UserIDN = return_the_number(UserID),
    Check  = check_partecipant(RoomIDN, UserIDN),
    case Check of 
        not_found -> no_partecipant_to_room_or_wrong_room_name;
        found -> Index = mnesia:dirty_update_counter(table_id, message, 1),
                    M = #message{id = Index, user = UserIDN, room = RoomIDN, message = Message},
                    Fun = fun() ->
                                mnesia:write(M)
                        end,
                    R = mnesia:transaction(Fun), element(2,R)
    end.

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%


%%%%%%  FETCH OR CREATE A NEW CHAT BETWEEN TWO USERS

return_the_number(Value) ->
  if
      is_atom(Value) -> list_to_integer(atom_to_list(Value));
      is_list(Value) -> list_to_integer(Value);
      true -> Value
  end.



fetch_or_create_new_chat(UserIDA, UserIDB)->
    UserIDAN = return_the_number(UserIDA),
    UserIDBN = return_the_number(UserIDB),
    %%check presence of both users  
    Check1 = check_user_id(UserIDAN),
    Check2 = check_user_id(UserIDBN),
    if
        Check1 =/= not_found, Check2 =/= not_found, UserIDAN =/= UserIDBN -> H = qlc:q([P1 || P1 <- mnesia:table(partecipants),
                                                                                              P2 <- mnesia:table(partecipants),
                                                                                              P1#partecipants.user == UserIDAN,
                                                                                              P2#partecipants.user == UserIDBN,
                                                                                              P1#partecipants.room == P2#partecipants.room]),
                                            R = mnesia:transaction(fun() -> qlc:e(H) end),
                                            Result = element(2,R),
                                            RoomName = "CHAT",
                                            case Result of
                                                [] ->   Room = create_new_room(RoomName),
                                                        RoomID = element(2, Room),
                                                        R1 = add_partecipant_to_room(RoomID, UserIDA),
                                                        R2 = add_partecipant_to_room(RoomID, UserIDB),
                                                        RoomID;
                                                _ -> element(4,lists:nth(1,Result))
                                            end;
        true -> error_creating_new_chat
    end.


select_all_chats_with_partecipants() ->
  H = qlc:q([{P1#partecipants.user, P2#partecipants.user, P1#partecipants.room} || P1 <- mnesia:table(partecipants),
                                                                                    P2 <- mnesia:table(partecipants),
                                                                                    P1#partecipants.room == P2#partecipants.room,
                                                                                    P1#partecipants.user =/= P2#partecipants.user ]),
  R = mnesia:transaction(fun() -> qlc:e(H) end),
  Result = element(2,R),
  lists:ukeysort(3,Result).



select_all_chats_of_a_user(UserID) ->
  UserIDn = return_the_number(UserID),

  H = qlc:q([P || P <- mnesia:table(partecipants), P#partecipants.user == UserIDn ]),
  R = mnesia:transaction(fun() -> qlc:e(H) end),
  Result = element(2,R),
  V = [{element(4, Elem),element(1,check_user_id(select_roommate( element(4, Elem), UserIDn))),select_roommate( element(4, Elem), UserIDn), fetch_all_messages_of_room(element(4, Elem))} || Elem <- Result],
  V.


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%5

%%RETURNS THE LIST OF THE partecipants to room

select_roommate(RoomID, UserID)->
  H = qlc:q([ P2#partecipants.user || P1 <- mnesia:table(partecipants),
                  P1#partecipants.room == RoomID,
                  P1#partecipants.user == UserID,
                  P2 <- mnesia:table(partecipants),
                  P2#partecipants.room == P1#partecipants.room,
                  P2#partecipants.user =/= UserID]),
  R = mnesia:transaction(fun() -> qlc:e(H) end),
  R1 = lists:nth(1,element(2,R)),
  R1.

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%5

%%RETURNS THE LIST OF THE partecipants to room

select_partecipant_to_room(RoomID)->
    H = qlc:q([P || P <- mnesia:table(partecipants), P#partecipants.room == RoomID ]),
    R = mnesia:transaction(fun() -> qlc:e(H) end),
    element(2,R).
   
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%5

%%RETURNS THE LIST OF THE ROOMS
select_all_chats() ->
    H = qlc:q([R || R <- mnesia:table(room)]),
    R = mnesia:transaction(fun() -> qlc:e(H) end),
    element(2,R).

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%5

%%RETURNS THE LIST OF THE USERS
select_all_users() ->
    H = qlc:q([R || R <- mnesia:table(user)]),
    R = mnesia:transaction(fun() -> qlc:e(H) end),
    element(2,R).


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

%%%     CHECK IF A PARTECIPANT IS PRESENT IN THE ROOM

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%% RETURNS FOUND | NOT FOUND
check_partecipant(RoomID, UserID)->
    H = qlc:q([P || P <- mnesia:table(partecipants), P#partecipants.user == UserID, P#partecipants.room == RoomID]),
    R = mnesia:transaction(fun() -> qlc:e(H) end),
    Result = element(2,R),
    case Result of
        [] -> not_found;
        _  -> found
    end.


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

 %               CHECK PRESENCE OF A USER


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%%% RETURNS not_found IF FOUND NOTHING
%%% RETURNS {User, ID} if FOUND
check_user_name(User) ->
    User_s = return_the_string(User),
    H = qlc:q([U || U <- mnesia:table(user), U#user.nameU == User_s]),
    R = mnesia:transaction(fun() -> qlc:e(H) end),
    Result = element(2,R),
    case Result of
        [] -> not_found;
        _  -> if
                is_tuple(Result) -> element(1, Result);
                true -> Elem = lists:nth(1, Result), ID = element(2, Elem), {User_s, ID}
              end
    end.

%%% RETURNS not_found IF FOUND NOTHING
%%% RETURNS {User, ID} if FOUND
check_user_id(UserID) ->
    UserIDn = return_the_number(UserID),
    H = qlc:q([U || U <- mnesia:table(user), U#user.id == UserIDn]),
    R = mnesia:transaction(fun() -> qlc:e(H) end),
    Result = element(2,R),
    case Result of
        [] -> not_found;
        {no_exists,user} -> not_found;
        _  -> Elem = lists:nth(1, Result), User = element(3, Elem), {User, UserIDn}
    end.



%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

 %               CHECK PRESENCE OF A ROOM


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

%%% RETURNS not_found | {GroupName, RoomID}
check_group_id(RoomID) ->
    H = qlc:q([R || R <- mnesia:table(room), R#room.id == RoomID, R#room.nameR =/= "CHAT"]),
    R = mnesia:transaction(fun() -> qlc:e(H) end),
    Result = element(2,R),
    case Result of
        [] -> not_found;
        _  -> Elem = lists:nth(1, Result), GroupName = element(3, Elem), {GroupName, RoomID}
    end.

%%% RETURNS not_found  | {RoomName, RoomID}
check_room_name(RoomName) ->
    H = qlc:q([R || R <- mnesia:table(room), R#room.nameR == RoomName]),
    R = mnesia:transaction(fun() -> qlc:e(H) end),
    Result = element(2,R),
    case Result of
        [] -> not_found;
        _  -> Elem = lists:nth(1, Result), ID = element(2, Elem), {RoomName, ID}
    end.
%%% RETURNS not_found  | {RoomName, RoomID}
check_room_id(RoomID) ->
  H = qlc:q([R || R <- mnesia:table(room), R#room.id == RoomID]),
  R = mnesia:transaction(fun() -> qlc:e(H) end),
  Result = element(2,R),
  case Result of
    [] -> not_found;
    _  -> found
  end.



%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%


%%%%%%%%% PRINT OUT THE TABLES

traverse_table_and_show(Table_name)->
    Iterator =  fun(Rec,_)->
                    io:format("~p~n",[Rec])
                end,
    case mnesia:is_transaction() of
        true -> mnesia:foldl(Iterator,[],Table_name);
        false -> 
            Exec = fun({Fun,Tab}) -> mnesia:foldl(Fun, [],Tab) end,
            mnesia:activity(transaction,Exec,[{Iterator,Table_name}],mnesia_frag)
    end.

%% Invocated by the main serve add a new node
add_new_node_to_db(Nodename) ->
    io:format("MAIN SERVER: Adding ~p in the node cluster ~n", [Nodename]),
    Result = mnesia:change_config(extra_db_nodes, [Nodename]),%% add the new node to the cluster
    io:format("~p~n", [Result]),


    Result.




make_cursor_local(Query) ->
    mnesia:activity(async_dirty, fun() -> qlc:cursor(Query) end, mnesia_frag).
    
get_next_local(Cursor, N) ->
    Get = fun() -> qlc:next_answers(Cursor,N) end,
    mnesia:activity(async_dirty, Get, mnesia_frag).
    
del_cursor_local(Cursor) ->
    qlc:delete_cursor(Cursor).
      

start() ->
    init().
