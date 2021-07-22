


erlc  -o './target/db_manager.beam' ./src/db_manager.erl;
erlc  -o './target/db_server.beam' ./src/db_server.erl;

gnome-terminal -- erl  -eval "db_server:start_server(none)." -sname server1@localhost  -setcookie "cluster" -mnesia dir '"./target/db1"'  -s mnesia   &
