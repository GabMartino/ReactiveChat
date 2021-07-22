

import React, {Component } from 'react';
import userIcon from '../../assets/user.png';
import groupIcon from '../../assets/group.png';
import '../../style/animations.css';
export default class ChatPlaceHolder extends Component {

    constructor(props){
        super(props);

    }
    render() {
        return(
            <li className={this.props.pendingMessage ? "blink active" : "active" } onClick={ () => this.props.selectChat(this.props.type == "CHAT" ? this.props.roomID : this.props.userID, this.props.type)}>

            <div class="d-flex bd-highlight">
                    <div class="img_cont">
                        <img src={!this.props.isGroup ? userIcon : groupIcon} class="rounded-circle user_img" />
                        {
                             <span class={this.props.isOnline ? "online_icon" : "offline_icon"}></span>
                            }
                        
                    </div>
                    <div class="user_info">
                        <div>
                            <span>{this.props.name}</span>
                        </div>
                            
                               <p>{this.props.pendingMessage ? "New Message" : ""}</p>
                      
                        
                    </div>
                    
                </div>

            </li>
			
        )
      }





}