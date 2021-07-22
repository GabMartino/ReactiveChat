import logo from './logo.svg';
import cookies from "js-cookies";
import React, { Component } from 'react';
import axios from 'axios';
import './App.css';
import ChatList from './components/chatLIst';
import ChatContainer from './components/chatContainer';
import NewUserLogIn from './components/newUserLogIn';
import UserUtils from './components/UserUtils';
import { w3cwebsocket as W3CWebSocket } from "websocket";


const server_main_url = window.location.href;
const server_main_url_socket = window.location.href.replace("http","ws");

//const server_main_url = "http://localhost:8080/ReactiveChat/";
//const server_main_url_socket = "ws://localhost:8080/ReactiveChat/";
const login_url = "login";
const logout_url = "logout";
const signup_url = "signup";
const fetch_chat_url = "fetchChat";
const startChat_url = "startchat";
const send_message_url = "sendMessage";
const chat_end_point_url = "chatEndpoint";
const usersList_url = "usersList";
class App extends Component {
  
  state = {
    logged : false,
    id : null,
    username : null,
    chatList : [],
    actualChat : {},
    chatMessageList : []

  }

  client = null;
  constructor(){
    super();
    
    this.login = this.login.bind(this);
    this.signup = this.signup.bind(this);
    this.logout = this.logout.bind(this);
    this.selectChat = this.selectChat.bind(this);
    this.sendMessage = this.sendMessage.bind(this);
  }

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
      this.requireChatsAndUsers();
      this.setWebSocket();
      this.forceUpdate();
      }else{
        cookies.removeItem("loginCookie");
      }
    }
   
  setWebSocket(){
    this.client = new W3CWebSocket(server_main_url_socket + chat_end_point_url + "/" + this.state.id);
    
    this.client.onopen = () => {
      console.log('WebSocket Client Connected' + this.state.id);
    };
    this.client.onerror = (error) => {
      console.log(error);
    };
    this.client.oneclose = (error) => {
      console.log(error);
    };
    this.client.onmessage = (message) => {
              console.log(JSON.parse(message.data));
              let msg = JSON.parse(message.data);

              console.log(msg.event)
              switch(msg.event) {

                 case "new_message" :           let temp = this.state.chatMessageList;
                                                for(let i= 0; i< temp.length; i++){
                                                    if(temp[i].roomID == msg.roomID){
                                                        temp[i].messages.push({
                                                          msg : msg.msg.replace(/['"]+/g, ''),
                                                          userID : msg.userID
                                                        })
                                                        console.log(msg)
                                                        break;
                                                    }
                                                }
                                                this.setState({
                                                  chatMessageList : temp
                                                })
                                                if(this.state.actualChat.roomID != msg.roomID){
                                                  let temp = this.state.chatList;
                                                  for(let i= 0; i< temp.length; i++){
                                                      if(temp[i].roomID == msg.roomID){
                                                          temp[i].pendingMessage = true;
                                                          
                                                          break;
                                                      }
                                                  }
                                                  this.setState({
                                                    chatList : temp
                                                  })
                                                }
                                                
                                                this.forceUpdate();
                                              
                                                break;
                case "chat_refresh" :               this.requireChatsAndUsers();
                                                this.forceUpdate();
                                                break;
               

                case this.default: console.log(msg);
 
              }


            };


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
        this.requireChatsAndUsers();
        this.setWebSocket();
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
    this.setState({ logged : false,
                      id : null,
                      username : null,
                      chatList : [],
                      actualChat : {},
                      chatMessageList : []
                });
    this.client.close();
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

  async requireChatsAndUsers(){
    let chats = await this.requireChats();
    let users = await this.requireUsers();
    
    Array.prototype.forEach.call(users, elem =>{
      elem.type = "USER";
      elem.username = elem.name.replace(/['"]+/g, '');
      elem.userID = elem.id;

    })

    console.log(users);
    //remove my place holder
    let index = users.findIndex(elem => elem.userID == this.state.id);
    users.splice(index,1);

    //remove duplicated user from already created chats
    for(let i = 0; i < chats.length ; i++){

      let index =  users.findIndex(user => user.userID == chats[i].userID)
      if(index != -1){
        users.splice(index, 1);
      }

    }
    let chat_without_messages = []
    Array.prototype.forEach.call( chats, elem =>{
      chat_without_messages.push({ 
        type: "CHAT",
        username : elem.username.replace(/['"]+/g, ''),
        userID : elem.userID,
        roomID : elem.roomID,
        pendingMessage : false
      })
    })
    let chatlist = chat_without_messages.concat(users);
    chatlist.sort(function(a,b){ return a.username > b.username});
    this.setState({
      chatList : chatlist,
      chatMessageList : chats
    })


  }
  async requireUsers(){
    let payload = JSON.stringify({
      request : "usersList",
      value : ""
    });

    axios.defaults.withCredentials = true;
    const res = await this.sendRequest(server_main_url + usersList_url, payload);
    return res.data.data;
    
  }

 //--------------------------------------------------------/////
  async requireChats(){
      
      let payload = JSON.stringify({
        request : "fetchChat",
        value : this.state.id
      });

      axios.defaults.withCredentials = true;
      const res = await this.sendRequest(server_main_url + fetch_chat_url,payload)
      return res.data.data
    
  }
 
  async selectChat(room_user_id, type){
    if(type == "CHAT"){
        let chat = this.state.chatMessageList.find( elem => elem.roomID == room_user_id);
        this.setState({
          actualChat : chat
        });
        let temp = this.state.chatList;
        for(let i = 0; i< temp.length;i++ ){
          if(temp[i].roomID == room_user_id){
              temp[i].pendingMessage = false;
          }
        }
        this.setState({
          chatList : temp
        })
       
        this.forceUpdate();
    }else{

      let payload = JSON.stringify({
        request : "startChat",
        value : [this.state.id, room_user_id]
      });
      axios.defaults.withCredentials = true;
      const res = await this.sendRequest(server_main_url + startChat_url,payload);
      let result = res.data.data;
      if(res.data.value == "ok"){
        await this.requireChatsAndUsers();
        let chat = this.state.chatMessageList.find( elem => {return elem.roomID == result;});
        
        this.setState({
          actualChat : chat
        });
        let temp = this.state.chatList;
        for(let i = 0; i< temp.length;i++ ){
          if(temp[i].roomID == chat.roomID){
              temp[i].pendingMessage = false;
          }
        }
        this.setState({
          chatList : temp
        })
        this.forceUpdate();
      }
      
    }


  }
  
  async sendMessage(messageContent, destinationID){
    let payload = JSON.stringify({
      request : "sendMessage",
      value : {
        destination : destinationID,
        source : this.state.id,
        message: messageContent,
        date: new Date().toDateString(),
        time: new Date().toLocaleTimeString()
      }
    });
    this.client.send(payload);
    console.log("sending message"+messageContent)

    let temp = this.state.chatMessageList;
    let chatIndex = temp.findIndex(elem => (elem.roomID == destinationID));
    let chat = temp[chatIndex];
    chat.messages.push({
      userID: this.state.id,
      msg : messageContent
    })
    
    this.setState({
      chatMessageList : temp
    })

  }

  componentWillMount(){
    this.loginWithCookies();
  }
  render() {

        return (
          <div className="App">
            <div class="container-fluid h-100">
              <div class="row justify-content-center h-100">
                { this.state.logged == false ? 
                        < NewUserLogIn  login={this.login} signup={this.signup}/>
                        :
                        <React.Fragment>
                          < ChatList       myID={this.state.id} 
                                                          chatList = {this.state.chatList} 
                                                          selectChat={this.selectChat}/>
                          < ChatContainer  roomName={this.state.actualChat ? (this.state.actualChat.username ? this.state.actualChat.username.replace(/['"]+/g, ''): null) : null} 
                                          roomId={this.state.actualChat ? this.state.actualChat.roomID : null} 
                                          roomMessages={this.state.actualChat ? this.state.actualChat.messages : null }
                                          sendMessage={this.sendMessage}
                                          myID={this.state.id} />
                          < UserUtils logout={this.logout} username={this.state.username} />
                          </React.Fragment>
                        }
              </div>
            </div>
          </div>
        )
  }
}

export default App;
