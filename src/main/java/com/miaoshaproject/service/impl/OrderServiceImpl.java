package com.miaoshaproject.service.impl;

import com.miaoshaproject.RabbitMQ.MqProducer;
import com.miaoshaproject.dao.OrderDOMapper;
import com.miaoshaproject.dao.SequenceDOMapper;
import com.miaoshaproject.dataobject.OrderDO;
import com.miaoshaproject.dataobject.SequenceDO;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.service.ItemService;
import com.miaoshaproject.service.OrderService;
import com.miaoshaproject.service.UserService;
import com.miaoshaproject.service.model.ItemModel;
import com.miaoshaproject.service.model.OrderModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    ItemService itemService;

    @Autowired
    UserService userService;

    @Autowired
    OrderDOMapper orderDOMapper;

    @Autowired
    SequenceDOMapper sequenceDOMapper;

    @Autowired
    MqProducer mqProducer;

    @Autowired
    RedisTemplate redisTemplate;

    @Transactional
    @Override
    public OrderModel createOrder(Integer userId, Integer itemId,Integer promoId, Integer amount) throws BusinessException {
        //校验下单状态，商品是否存在，用户是否合法，购买数量是否正确
        ItemModel itemModel = itemService.getItemByIdCache(itemId);//可以缓存起来
        if(amount<=0 || amount>99){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"数量信息不正确");
        }

        //检测库存是否足够,售完的标识
        if(redisTemplate.hasKey("promo_item_stock_invalid_" + itemId)){
            //更新商品详情缓存
            itemModel = itemService.getItemById(itemId);
            redisTemplate.opsForValue().set("item_"+itemId,itemModel);
            redisTemplate.opsForValue().set("item_validate_"+itemId,itemModel);
            throw new BusinessException(EmBusinessError.STOCK_NOTENOUGH,"库存不足，下单失败");
        }

        //下单减库存
        boolean res = itemService.decreaseStock(itemId, amount);
        if (!res){
            throw new BusinessException(EmBusinessError.STOCK_NOTENOUGH);
        }
        //订单入库
        OrderModel orderModel = new OrderModel();
        orderModel.setUserId(userId);
        orderModel.setItemId(itemId);
        orderModel.setAmount(amount); //需要判断是否是秒杀活动价格
        orderModel.setItemPrice( promoId==null ? itemModel.getPrice() : itemModel.getPromoModel().getPromoItemPrice());
        orderModel.setOrderPrice(promoId==null ? itemModel.getPrice().multiply(new BigDecimal(amount)) : itemModel.getPromoModel().getPromoItemPrice().multiply(new BigDecimal(amount)) );
        orderModel.setPromoId(promoId);
        OrderDO orderDO = convertOrderDOFromOrderModel(orderModel);
        orderDO.setId(generateOrderNo());
        //如果以下两步失败，再将redis和mysql里的库存加上来即可！！！
        //还可以更简便，进行异步发送消息，以下两步全部完成在实际的发送消息！！！
        orderDOMapper.insertSelective(orderDO);
        //加上商品销量
        itemService.increaseSales(itemId,amount);


        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                //全部成功才会实际同步到mysql中！！！
                mqProducer.decreaseSockSend(itemId,amount);
            }
        });
        //返回前端
        return orderModel;
    }

    //生成交易流水号
    @Transactional(propagation = Propagation.REQUIRES_NEW) //重新开启新的事务，sequence唯一，只能用一次
    private String generateOrderNo(){
        //订单号16位
        //前八位时间信息，年月日
        StringBuilder stringBuilder = new StringBuilder();
        LocalDateTime date = LocalDateTime.now();
        String data = date.format(DateTimeFormatter.ISO_DATE).replace("-", "");
        stringBuilder.append(data);
        //中间六位自增序列
        SequenceDO sequenceDO = sequenceDOMapper.getSequenceByName("order_info");
        Integer sequence = sequenceDO.getCurrentValue();
        sequenceDO.setCurrentValue(sequenceDO.getCurrentValue()+sequenceDO.getStep());//不考虑最大值
        sequenceDOMapper.updateByPrimaryKeySelective(sequenceDO);
        String sequenceStr = String.valueOf(sequence);
        for(int i=0;i<6-sequenceStr.length();i++){
            stringBuilder.append(0);
        }
        stringBuilder.append(sequenceStr);
        //最后两位分库分表位
        stringBuilder.append("00");
        return stringBuilder.toString();
    }

    private OrderDO convertOrderDOFromOrderModel(OrderModel orderModel){
        OrderDO orderDO = new OrderDO();
        BeanUtils.copyProperties(orderModel,orderDO); //值的类型不同不会进行设置
        orderDO.setItemPrice(orderModel.getItemPrice().doubleValue());
        orderDO.setOrderPrice(orderModel.getOrderPrice().doubleValue());
        return orderDO;
    }
}
