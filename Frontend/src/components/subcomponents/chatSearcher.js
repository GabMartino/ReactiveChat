

import React, {Component } from 'react';

export default class ChatSearcher extends Component {

    state = {
        
        showAddGroupModal : false
    }
    

    hideModal = () =>{
        this.setState({
            showAddGroupModal : false
            })
    }
    showModal = () =>{
        this.setState({
            showAddGroupModal : true
            })
        }
    render() {
        return(
            
                <div class="input-group">
                        <input type="text" placeholder="Search..." name="" class="form-control search"/>
                   
                    <div className="input-group-prepend">
                        <span class=" input-group-text search_btn"><i class="fas fa-search "></i></span>
                            
                    </div>
                  
                </div>
			
        )
      }





}