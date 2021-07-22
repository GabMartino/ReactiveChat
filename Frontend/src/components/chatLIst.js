


import React, {Component } from 'react';
import ChatPlaceHolder from './subcomponents/chatPlaceHolder';
import ChatSearcher from './subcomponents/chatSearcher';
export default class ChatList extends Component {
    state = {
        list : []
    }
    constructor(props){
        super(props);
    }
    
    
    componentWillReceiveProps(){
        this.forceUpdate();
    }
    componentWillMount(props){
        this.forceUpdate();
    }
    render() {
        return(
            <div class="col-md-4 col-xl-3 chat">
                <div class="card mb-sm-3 mb-md-0 contacts_card">
                    <div class="card-header">
						< ChatSearcher addNewGroup={this.props.addNewGroup}/>
                       
					</div>
                    <div class="card-body contacts_body">
                        <ui class="contacts">

                            {
                                this.props.chatList.map((elem,i) => 
                                    < ChatPlaceHolder   
                                                key={i}
                                                name={elem.username.replace(/['"]+/g, '')}
                                                userID= {elem.userID}
                                                roomID= {elem.roomID}
                                                type = {elem.type}
                                                selectChat={this.props.selectChat}
                                                pendingMessage={elem.pendingMessage}/>
                                )
                            }
                        </ui>
                    </div>
                </div>

            </div>
        )
      }





}