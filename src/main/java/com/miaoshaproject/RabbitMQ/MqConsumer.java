package com.miaoshaproject.RabbitMQ;

import com.alibaba.fastjson.JSON;
import com.miaoshaproject.RabbitMQ.model.DecreaseStockModel;
import com.miaoshaproject.dao.ItemStockDOMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MqConsumer {

    @Autowired
    private ItemStockDOMapper itemStockDOMapper;

    @RabbitListener(queues = "decreaseSockQueue")
    public void decreaseSockGet(String message) throws Exception{
        System.out.println("接收到的消息---"+message);
        DecreaseStockModel decreaseStockModel = JSON.parseObject(message.toString(), DecreaseStockModel.class);
        System.out.println("decreaseStockModel---"+decreaseStockModel);
        //根据itemId 和 amount更新库存
        itemStockDOMapper.decreaseStock(decreaseStockModel.getItemId(),decreaseStockModel.getAmount());

    }

}
