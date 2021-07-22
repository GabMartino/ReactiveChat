

docker rm erlang_node &

gnome-terminal -- docker run -a stdin -a stdout -a stderr -i --name erlang_node -h erlang_node  --net docker_network erlang_node_with_app erl  -eval "db_server:start_server(none)." -sname server1@erlang_node -setcookie "cluster" -mnesia dir '"./target/db1"'

