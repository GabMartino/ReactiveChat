package reactivechat.websocketscontrollers;

import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.jms.JMSDestinationDefinition;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;

//@LocalBean
@JMSDestinationDefinition(
        name="java:global/queue/simpleQ",
        interfaceName="javax.jms.Queue",
        destinationName = "simpleQ"
)
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup",
                propertyValue = "java:global/queue/simpleQ"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue")
})
public class AsyncEventDispatcher implements MessageListener {


    @Inject
    @WSJMSMessage
    Event<Message> jmsEvent;

    @Override
    public void onMessage(Message message) {
        jmsEvent.fire( message);

    }
}
