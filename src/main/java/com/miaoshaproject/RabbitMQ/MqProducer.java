package com.miaoshaproject.RabbitMQ;

import com.alibaba.fastjson.JSON;
import com.miaoshaproject.RabbitMQ.model.DecreaseStockModel;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class MqProducer {

    @Autowired
    RabbitTemplate rabbitTemplate;

    public void decreaseSockSend(Integer itemId,Integer amount) {

        DecreaseStockModel decreaseStockModel = new DecreaseStockModel();
        decreaseStockModel.setAmount(amount);
        decreaseStockModel.setItemId(itemId);
        //参数介绍： 交换机名字，路由建， 消息内容
        rabbitTemplate.convertAndSend("directExchange", "direct.key", JSON.toJSONString(decreaseStockModel));
    }


}
