


import React, {Component } from 'react';

import UserMessage from './userMessage';
import UserMessageImage from './userMessageImage';
export default class Message extends Component {
    constructor(props){
        super(props);
      
    }

    render() {

      
        return(
            
            <div  class={
                
                    this.props.alignment == 'left' ? 'd-flex justify-content-start mb-4' : 'd-flex justify-content-end mb-4'
                }
                >

                {
                    this.props.alignment == 'right' ?
                    <React.Fragment>
                    < UserMessage alignment={this.props.alignment} content={this.props.content}/>
                    <UserMessageImage />
                    </React.Fragment>
                    :
                    <React.Fragment>
                    <UserMessageImage />
                    < UserMessage alignment={this.props.alignment} content={this.props.content}/>
                    </React.Fragment>
                    
                }
                
               
            </div>
            


        )
      }





}