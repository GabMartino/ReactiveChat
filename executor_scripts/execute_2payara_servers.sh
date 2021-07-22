

docker stop payara_server &

docker stop payara_server2 &
sleep 2
docker rm payara_server &
docker rm payara_server2 &

gnome-terminal -- docker run --name payara_server -h payara_server --net docker_network -p 8080:8080 -v /home/gabriele/Bechini_Project/Concurrent_and_Distributed_Systems_project/Container_payara_node_with_app/artifacts:/opt/payara/deployments payara_node_with_app &

sleep 2 
gnome-terminal -- docker exec -d payara_server epmd -d &

sleep 1
gnome-terminal -- docker run --name payara_server2 -h payara_server2 --net docker_network -p 8888:8080 -v /home/gabriele/Bechini_Project/Concurrent_and_Distributed_Systems_project/Container_payara_node_with_app/artifacts:/opt/payara/deployments payara_node_with_app &

sleep 2 
gnome-terminal -- docker exec -d payara_server2 epmd -d
