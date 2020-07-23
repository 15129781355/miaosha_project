package com.miaoshaproject.config;


import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {


    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory("localhost",5672);
        connectionFactory.setUsername("guest");
        connectionFactory.setPassword("guest");
        connectionFactory.setVirtualHost("miaosha");
        //是否开启消息确认机制，发送端在发送完成后会进行回调
        //connectionFactory.setPublisherConfirms(true);
        return connectionFactory;
    }
    @Bean//减库存交换机
    public DirectExchange decreaseSockExchange() {
        return new DirectExchange("directExchange");
    }

    @Bean
    public Queue decreaseSockQueue() {
        //名字  是否持久化
        return new Queue("decreaseSockQueue", false);
    }
    @Bean
    public Binding binding() {
        //绑定一个队列  to: 绑定到哪个交换机上面 with：绑定的路由键（routingKey）
        return BindingBuilder.bind(decreaseSockQueue()).to(decreaseSockExchange()).with("direct.key");
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        //注意  这个ConnectionFactory 是使用javaconfig方式配置连接的时候才需要传入的
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        //设置message的属性
        //还可以设置message的type转换器，objectEntity-->json/map
        return template;
    }
}
