




erl    -eval "db_server:start_server(server1@erlang_node)."  -sname server2@erlang_node2  -mnesia dir '"./target/db2"'