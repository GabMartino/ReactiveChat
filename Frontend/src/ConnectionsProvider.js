
const server_main_url = "http://localhost:8080/groupchat-1.0-SNAPSHOT";
const login_url = "/login";
const logout_url = "/logout";
const signup_url = "/signup";
const usersList_url = "/usersList";
const startChat_url = "/startchat";
const fetchMessages_url = "/fetchMessagesFromRoom";
const send_message_url = "/sendMessage";




class ConnectionProvider {




    async sendRequest(url, payload) {
        let res = await axios.post(url,payload )
        return res;
    }
    //--------------------------------------------------------/////
    async loginWithCookies(){
      axios.defaults.withCredentials = true;
      let res = await this.sendRequest(server_main_url + login_url, 
                                  null
                                  );             
  
      
      
      const response = res.data;
      if( response.value == "ok"){
        console.log("login accepted")
        this.setState({
          logged : true,
          username : response.data.name.replace(/['"]+/g, ''),
            id : response.data.id
        })
        this.requestUsersList();
        this.forceUpdate();
        }
      }
     
     //--------------------------------------------------------/////
    async login( Username ){
      let payload = JSON.stringify({
                                      request : "loginUser",
                                      value : Username
                                    });
  
      axios.defaults.withCredentials = true;
  
      let res = await this.sendRequest(server_main_url + login_url, 
        payload
        );             
  
  
        console.log(res.data)
      
        let response = res.data;
        if( response.value == "ok"){
  
          console.log(response.value)
          cookies.setItem("loginCookie", response.data.id);
          this.setState({
            logged : true,
            username : response.data.name.replace(/['"]+/g, ''),
              id : response.data.id
          })
          this.requestUsersList();
          this.forceUpdate();
        }else{
            alert(response.data);
        }
  
    }
     //--------------------------------------------------------/////
    async logout(){
      let payload = JSON.stringify({
        request : "logoutUser",
        value : this.state.username
      });
  
      axios.defaults.withCredentials = true;
  
      let res = await this.sendRequest(server_main_url + logout_url, 
      payload
      ); 
      
      cookies.removeItem("loginCookie");
      this.setState({ logged : false, username : "", chatList : [], id : null});
      this.forceUpdate();
  
  
    }
     //--------------------------------------------------------/////
    async signup( Username ){
        let payload = JSON.stringify({
                                      request : "registerUser",
                                      value : Username
                                    });
  
      axios.defaults.withCredentials = true;
  
      let res = await this.sendRequest(server_main_url + signup_url, 
        payload
        );             
  
        let response = res.data;
        if( response.value == "ok"){
            alert("YOU'RE CORRECTLY REGISTERED");
        }else{
            alert(response.data);
        }
  
  
  
    }
  
   //--------------------------------------------------------/////
    async requestUsersList(){
        
        let payload = JSON.stringify({
          request : "usersList",
          value : ""
        });
  
        axios.defaults.withCredentials = true;
        const res = await this.sendRequest(server_main_url + usersList_url, 
        payload
        ).then(
          result =>{
            this.setState({ 
              chatList : result.data.data  });
  
  
          }
        );
        this.forceUpdate();
        
        
    }


    async fetchMessages(roomId){
        let payload = JSON.stringify({
          request : "fetchMessagesFromRoom",
          value : roomId
        });
    
        axios.defaults.withCredentials = true;
        const res = await this.sendRequest(server_main_url + fetchMessages_url, 
                  payload
                  );
    
    
          if(res.data.value == "ok"){
              return res.data.data;
          
          }else{
              return null;
          }
    
    
      }
    
    
    
    
      async selectChat(UserBID, UserName){
        let payload = JSON.stringify({
          request : "startChat",
          value : [this.state.id, UserBID]
        });
    
        axios.defaults.withCredentials = true;
        const res = await this.sendRequest(server_main_url + startChat_url, 
                  payload
                  );
        this.setState({
                    actualChat : null
        });
    
        if(res.data.value == "ok"){
            let messages = this.fetchMessages(res.data.data).then(
                resp =>{
                  console.log(resp);
                  this.setState({
                              actualChat : {
                                name : UserName,
                                id : res.data.data,
                                messages : resp
                              }
                  });
    
                  this.forceUpdate();
                }
    
            );
    
        }
      
    
    
      }
      
      async sendMessage(messageContent){
        let payload = JSON.stringify({
          request : "sendMessage",
          value : {
            roomId : " ",
            userId : this.state.id,
            message: messageContent
          }
        });
    
        axios.defaults.withCredentials = true;
        const res = await this.sendRequest(server_main_url , 
                  payload
                  );
      
        if(res.data.value == "ok"){
        }
    
      }









}