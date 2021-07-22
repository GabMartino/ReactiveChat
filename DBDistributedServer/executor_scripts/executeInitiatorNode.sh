




erl  -eval "db_server:start_server(none)." -sname server1@erlang_node  -mnesia dir '"./target/db1"'  -s mnesia