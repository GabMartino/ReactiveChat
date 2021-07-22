-record(user, {id,nameU}). %defines the users

-record(room, {id,nameR}).

-record(partecipants, {id,  user, room}).


-record(message, {id ,room, user, message}).
-record(table_id, {table_name, last_id}).