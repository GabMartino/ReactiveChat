


import React, {Component } from 'react';


export default class UserMessage extends Component {


    
    render() {

      
        return(
            
            <div class={
                            
                this.props.alignment == 'left' ? 'msg_cotainer' : 'msg_cotainer_send'
                    }>
                {this.props.content}
                <span class="msg_time">8:40 AM, Today</span>
            </div>
            


        )
      }





}