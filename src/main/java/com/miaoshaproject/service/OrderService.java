package com.miaoshaproject.service;

import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.service.model.OrderModel;

public interface OrderService {

    //前端url传过来秒杀活动id
    OrderModel createOrder(Integer userId,Integer itemId,Integer promoId,Integer amount)throws BusinessException;


}
