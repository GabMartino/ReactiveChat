

erlc  -o './target/db_manager.beam' ./src/db_manager.erl;
erlc  -o './target/db_server.beam' ./src/db_server.erl;

gnome-terminal -- erl  -eval "db_server:start_server(none)." -sname server1@localhost -setcookie "cluster" -mnesia dir '"./target/db1"'    &

sleep 1

gnome-terminal -- erl    -eval "db_server:start_server(server1@localhost)."  -sname server2@localhost -setcookie "cluster"  -mnesia dir '"./target/db2"' &
sleep 1

gnome-terminal -- erl   -eval "db_server:start_server(server1@localhost)." -sname server3@localhost -setcookie "cluster" -mnesia dir '"./target/db3"'   &


sleep 1
gnome-terminal -- erl  -eval "db_server:start_server(server1@localhost)."  -sname server4@localhost -setcookie "cluster" -mnesia dir '"./target/db4"'    &
sleep 1

gnome-terminal -- erl   -eval "db_server:start_server(server1@localhost)."  -sname server5@localhost  -setcookie "cluster" -mnesia dir '"./target/db5"'   &