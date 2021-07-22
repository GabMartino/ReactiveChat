package reactivechat.websocketscontrollers;

import jakarta.annotation.Resource;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.jms.JMSContext;
import jakarta.jms.Queue;
import jakarta.jms.TextMessage;
import org.json.JSONObject;


@Stateless
@LocalBean
public class QueueSenderSessionBean {

    @Resource(lookup = "java:global/queue/simpleQ")
    private Queue myQueue;

    @Inject
    private JMSContext jmsContext;

    public void sendMessage(JSONObject message) {
        TextMessage msg = jmsContext.createTextMessage(message.toString());
        jmsContext.createProducer().send(myQueue, msg);
    }

}