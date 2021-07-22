

docker rm payara_server &
sleep 1

gnome-terminal -- docker run --name payara_server -h payara_server --net docker_network -p 8080:8080 -v /home/gabriele/Bechini_Project/Concurrent_and_Distributed_Systems_project/Container_payara_node_with_app/artifacts:/opt/payara/deployments payara_node_with_app &

sleep 2 
gnome-terminal -- docker exec -d payara_server epmd -d
