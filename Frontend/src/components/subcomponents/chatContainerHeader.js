


import React, {Component } from 'react';
import userIcon from '../../assets/user.png';
import groupIcon from '../../assets/group.png';

export default class ChatContainerHeader extends Component {



    render() {
        return(
           
                <div class="card-header msg_head">
							<div class="d-flex bd-highlight">
								<div class="img_cont">
									<img src={this.props.isGroup ? groupIcon : userIcon} class="rounded-circle user_img"/>
									{this.props.isGroup ? <span class="online_icon"></span> : ''}
									
									
								</div>
								<div class="user_info">
									<span>Chat with {this.props.roomName}</span>
									<p>1767 Messages</p>
								</div>
								<div class="video_cam">
									<span><i class="fas fa-video"></i></span>
									<span><i class="fas fa-phone"></i></span>
								</div>
							</div>
							<span id="action_menu_btn"><i class="fas fa-ellipsis-v"></i></span>
							<div class="action_menu">
								<ul>
									<li><i class="fas fa-user-circle"></i> View profile</li>
									<li><i class="fas fa-users"></i> Add to close friends</li>
									<li><i class="fas fa-plus"></i> Add to group</li>
									<li><i class="fas fa-ban"></i> Block</li>
								</ul>
							</div>
						</div>
        


        )
      }





}