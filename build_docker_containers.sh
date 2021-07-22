



docker rmi erlang_node_with_app &

docker rmi payara_node_with_app &

docker build -t erlang_node_with_app ./Container_erlang_node_with_dbapp &

docker build -t payara_node_with_app ./Container_payara_node_with_app 

