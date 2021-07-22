


import React, {Component } from 'react';
export default class UserUtils extends Component {



    render() {
        return(
            <div class="user_utils">
              
               
                <div class="profile_pic ">
                    <img src="https://static.turbosquid.com/Preview/001292/481/WV/_D.jpg" class="rounded-circle user_img" alt="image not available"/>
                   
                </div>
                <div class="user_info">
                        <span>{this.props.username}</span>
                        
                </div>
                <div class="user_utils_logout" onClick={this.props.logout}>
                   LOGOUT
                </div>
                    

            </div>
        )
      }





}