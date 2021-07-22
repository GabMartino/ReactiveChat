
docker stop erlang_node &
docker stop erlang_node2 &
docker stop erlang_node3 &
docker stop erlang_node4 &
docker stop erlang_node5 &
sleep 2
docker rm erlang_node &
docker rm erlang_node2 &
docker rm erlang_node3 &
docker rm erlang_node4 &
docker rm erlang_node5 &


gnome-terminal -- docker run -a stdin -a stdout -a stderr -i --name erlang_node -h erlang_node  --net docker_network erlang_node_with_app erl  -eval               "db_server:start_server(none)." -sname server1@erlang_node -setcookie "cluster" -mnesia dir '"./target/db1"' &

sleep 1

gnome-terminal --  docker run -a stdin -a stdout -a stderr -i --name erlang_node2 -h erlang_node2  --net docker_network erlang_node_with_app erl  -eval "db_server:start_server(server1@erlang_node)."  -sname server2@erlang_node2 -setcookie "cluster" -mnesia dir '"./target/db2"' &

sleep 1

gnome-terminal --  docker run -a stdin -a stdout -a stderr -i --name erlang_node3 -h erlang_node3  --net docker_network erlang_node_with_app erl  -eval "db_server:start_server(server1@erlang_node)." -sname server3@erlang_node3 -setcookie "cluster" -mnesia dir '"./target/db3"'   &

sleep 1

gnome-terminal --  docker run -a stdin -a stdout -a stderr -i --name erlang_node4 -h erlang_node4  --net docker_network erlang_node_with_app erl  -eval "db_server:start_server(server1@erlang_node)."  -sname server4@erlang_node4 -setcookie "cluster" -mnesia dir '"./target/db4"'    &

sleep 1

gnome-terminal --  docker run -a stdin -a stdout -a stderr -i --name erlang_node5 -h erlang_node5  --net docker_network erlang_node_with_app erl  -eval "db_server:start_server(server1@erlang_node)."  -sname server5@erlang_node5  -setcookie "cluster" -mnesia dir '"./target/db5"'   &
