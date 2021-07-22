


import React, {Component } from 'react';
import Message from './message';

export default class MessageInput extends Component {


    constructor(props) {
      super(props);
    }
  
    _handleKeyDown = (e) =>{
    
        if (e.key == 'Enter' || e.type == "click") {
          if(e.target.id == "message_input"){
            
            this.props.sendMessage(e.target.value, this.props.destination);
            e.target.value = "";
          }else if(e.target.id == "clickInput" || e.target.parentElement.id == "clickInput" || e.target.parentElement.parentElement.id == "clickInput"){
              
              this.props.sendMessage(document.getElementById("message_input").value , this.props.destination);
              
              document.getElementById("message_input").value = "";
          }
         
        }
      }

    render() {
        return(
            <div class="card-footer">
                <div class="input-group">
                    <div class="input-group-append">
                        <span class="input-group-text attach_btn"><i class="fas fa-paperclip"></i></span>
                    </div>
                    <input id="message_input" class="form-control type_msg" placeholder="Type your message..." onKeyDown={this._handleKeyDown}></input>
                    <div id="clickInput" class="input-group-append" onClick={this._handleKeyDown}>
                        <span class="input-group-text send_btn"><i class="fas fa-location-arrow"></i></span>
                    </div>
                </div>
            </div>


        )
      }





}