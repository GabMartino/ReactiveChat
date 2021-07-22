


import React, {Component } from 'react';
import logoImage from '../assets/user.png';
export default class NewUserLogIn extends Component {


  constructor(props){
    super(props );
  }
  _handleKeyDown = (e) => {
    
    if (e.key == 'Enter' || e.type == "click") {
      if(e.target.name == "register" || e.target.name == "registerButton" || e.currentTarget.id == "registerButton" || e.currentTarget.parentNode.id == "registerButton"){
        
        this.props.signup(document.getElementsByName("register")[0].value);
      }else if( e.target.name == "login" || e.target.name == "loginButton" || e.currentTarget.id== "loginButton" || e.currentTarget.parentNode.id== "loginButton"){
        this.props.login(document.getElementsByName("login")[0].value);
      }
     
    }
  }

    render() {
        return(
            <div className="col-md-4 col-xl-3 chat">
              <div className="card_small mb-sm-3 mb-md-0 contacts_card container">
              <div className="img_log_in">
									<img src={logoImage} className="rounded-circle user_img_big"/>
          
							</div>
                Register New user
                <div className="input-group">
                    <input name="register" className="form-control username_input" onKeyDown={this._handleKeyDown} placeholder="Type your new Username "/>
                    <div id= "registerButton" className="input-group-append" onClick={this._handleKeyDown}>
                        <span className="input-group-text send_btn" ><i class="fas fa-location-arrow" ></i></span>
                    </div>
                </div>
                Log In
                <div className="input-group" >
                    <input name="login" className="form-control username_input" onKeyDown={this._handleKeyDown} placeholder="Type your username"/>
                    <div id= "loginButton" className="input-group-append" onClick={this._handleKeyDown}>
                        <span className="input-group-text send_btn" ><i class="fas fa-location-arrow" ></i></span>
                    </div>
                </div>
              </div>
            </div>
        )
      }





}