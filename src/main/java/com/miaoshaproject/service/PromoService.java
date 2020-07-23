package com.miaoshaproject.service;

import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.service.model.PromoModel;

public interface PromoService {
    //获取是否有即将开始或者正在进行的秒杀活动
    PromoModel getPromoByItemId(Integer itemId)throws BusinessException;

    //活动发布
    void publishPromo(Integer promoId) throws BusinessException;

    //生成秒杀用的令牌，一般前端做防刷，以及用户正确性的验证
    String generateSecondKillToken(Integer userId, Integer itemId,Integer promoId) throws BusinessException;
}
