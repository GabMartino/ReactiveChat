
import equal from 'fast-deep-equal';
import React, {Component } from 'react';
import { ScrollView } from 'react-native';
import Message from './message';

export default class ChatContainerBody extends Component {


    constructor(props){
        super(props);
    }
    componentWillMount(){
        this.props.messages.sort((a,b) => a.msgID - b.msgID);
    }
    componentWillReceiveProps(){
        this.props.messages.sort((a,b) => a.msgID - b.msgID);
        this.forceUpdate();
    }
    componentDidUpdate(prevProps){
        if(!equal(this.props.messages, prevProps.messages)){
            this.props.messages.sort((a,b) => a.msgID - b.msgID);
          this.forceUpdate();
        }
    } 
    render() {
        return(
            <div class="card-body msg_card_body">
                <ScrollView
                    ref={ref => {this.scrollView = ref}}
                    onContentSizeChange={() => this.scrollView.scrollToEnd({animated: true})}>
                        {
                        
                        
                        this.props.messages.map((elem, i) => <Message key={i} alignment={elem.userID == this.props.myID ? 'right' : 'left' }  content={elem.msg} />)
                        }
                </ScrollView>
                
            </div>


        )
      }





}