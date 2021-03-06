package com.javasampleapproach.rabbitmq.producer;


import com.javasampleapproach.rabbitmq.model.ScheduleJob;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class Producer {

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Value("${jsa.rabbitmq.exchange}")
    private String exchange;

    @Value("${jsa.rabbitmq.routingkey}")
    private String routingkey;

//    public void producejob(ScheduleJob job) {
//        amqpTemplate.convertAndSend(exchange, routingkey, job);
//        System.out.println("Send msg = " + job);
//    }

    public void producejob(String exchange, String routingKey, ScheduleJob job) {
        amqpTemplate.convertAndSend(exchange, routingKey, job);
        System.out.println("Send msg = " + job);
    }


    public Producer() {
    }

    @RabbitListener(queues = "${jsa.rabbitmq.queue}")
    public void recievedMessage(String s) {

        System.out.println(s);

    }

}