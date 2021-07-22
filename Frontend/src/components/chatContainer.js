


import React, {Component } from 'react';
import ChatContainerBody from './subcomponents/chatContainerBody';
import ChatContainerHeader from './subcomponents/chatContainerHeader';
import MessageInput from './subcomponents/messageInput';
export default class ChatContainer extends Component {

    state = {
        roomName : this.props.roomName,
        roomId : this.props.roomId,
        messages : this.props.roomMessages


    }

    render() {
        return(
           <div class="col-md-8 col-xl-6 chat">
                <div class="card">	
                {
                    this.props.roomName  ? 
                    <React.Fragment>
                    < ChatContainerHeader roomName={this.props.roomName} />
                    < ChatContainerBody messages={this.props.roomMessages} myID={this.props.myID}/>
                    < MessageInput sendMessage={this.props.sendMessage} destination={this.props.roomId}/>
                    </React.Fragment>
                    :
                    ''
                }
                </div>

            </div>


        )
      }





}