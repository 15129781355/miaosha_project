package com.miaoshaproject.service.impl;

import com.miaoshaproject.dao.PromoDOMapper;
import com.miaoshaproject.dataobject.PromoDO;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.service.ItemService;
import com.miaoshaproject.service.PromoService;
import com.miaoshaproject.service.UserService;
import com.miaoshaproject.service.model.ItemModel;
import com.miaoshaproject.service.model.PromoModel;
import com.miaoshaproject.service.model.UserModel;
import jdk.nashorn.internal.runtime.regexp.joni.constants.OPSize;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class PromoServiceImpl implements PromoService {

    @Autowired
    private PromoDOMapper promoDOMapper;

    @Autowired
    private ItemService itemService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserService userService;

    @Override
    public PromoModel getPromoByItemId(Integer itemId) throws BusinessException {
        //获取商品的秒杀活动信息
        PromoDO promoDO = promoDOMapper.selectByItemId(itemId);
        //类型转换
        PromoModel promoModel = convertPromoModelFromPromoDO(promoDO);
        //判断是否还有秒杀活动
        if(promoModel==null){
            return null;
        }
        //判断当前时间是否秒杀活动即将开始或正在进行
        DateTime nowDateTime = new DateTime();
        if(promoModel.getStartDate().isAfterNow()){
            promoModel.setStatus(1);//秒杀活动还未进行
        }else if(promoModel.getEndDate().isBeforeNow()){
            promoModel.setStatus(3);//秒杀活动已经结束
        }else {
            promoModel.setStatus(2);//秒杀活动正在进行
        }
        return promoModel;
    }

    //发布秒杀商品，库存同步缓存，设置秒杀令牌
    @Override
    public void publishPromo(Integer promoId) throws BusinessException{
        //获取活动
        PromoDO promoDO = promoDOMapper.selectByPrimaryKey(promoId);
        if(promoDO==null || promoDO.getItemId().intValue()==0){
            return;
        }
        ItemModel itemModel = itemService.getItemById(promoDO.getItemId());
        //以下缓存都需要在商品卖完或者秒杀活动结束之后，清除掉。
         //将库存同步到redis内
        redisTemplate.opsForValue().set("promo_item_stock_"+itemModel.getId(),itemModel.getStock());
        //设置令牌数量
        redisTemplate.opsForValue().set("promo_door_count_"+promoId,itemModel.getStock()*5);
    }

    //生成秒杀令牌
    @Override
    public  String  generateSecondKillToken(Integer userId, Integer itemId,Integer promoId) throws BusinessException {
        //检查令牌数量
        Integer promoDoorCount = redisTemplate.opsForValue().increment("promo_door_count_" + promoId,-1).intValue();
        if(promoDoorCount==0){
            throw new BusinessException(EmBusinessError.STOCK_NOTENOUGH,"库存不足,下单失败");
        }


        //校验下单状态，商品是否存在，用户是否合法，购买数量是否正确
        ItemModel itemModel = itemService.getItemByIdCache(itemId);//可以缓存起来
        if(itemModel==null){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"商品信息不存在");
        }
        UserModel userModel = userService.getUserById(userId);//可以缓存起来，没做
        if(userModel==null){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"用户信息不存在");
        }

        //校验活动信息
        if(promoId!=null){
            if(promoId!=itemModel.getPromoModel().getId()){
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"活动信息不正确");
            }else if(itemModel.getPromoModel().getStatus()!=2) {
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"活动还未开始");
            }
        }

        //获取商品的秒杀活动信息
        PromoDO promoDO = promoDOMapper.selectByPrimaryKey(promoId);
        //类型转换
        PromoModel promoModel = convertPromoModelFromPromoDO(promoDO);
        //判断是否还有秒杀活动
        if(promoModel==null){
            return null;
        }
        //判断当前时间是否秒杀活动即将开始或正在进行
        DateTime nowDateTime = new DateTime();
        if(promoModel.getStartDate().isAfterNow()){
            promoModel.setStatus(1);//秒杀活动还未进行
        }else if(promoModel.getEndDate().isBeforeNow()){
            promoModel.setStatus(3);//秒杀活动已经结束
        }else {
            promoModel.setStatus(2);//秒杀活动正在进行
        }
        if(promoModel.getStatus().intValue()!=2){
            return null;
        }


        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set("promo_token_"+promoId+"_"+userId+"_"+itemId,token);
        redisTemplate.expire("promo_token_"+promoId+"_"+userId+"_"+itemId,5, TimeUnit.MINUTES);
        return token;
    }

    private PromoModel convertPromoModelFromPromoDO(PromoDO promoDO){
        if(promoDO==null){
            return null;
        }
        PromoModel promoModel = new PromoModel();
        BeanUtils.copyProperties(promoDO,promoModel);
        //转换类型不匹配的属性
        promoModel.setPromoItemPrice(new BigDecimal(promoDO.getPromoItemPrice()));
        promoModel.setStartDate(new DateTime(promoDO.getStartDate()));
        promoModel.setEndDate(new DateTime(promoDO.getEndDate()));
        return promoModel;
    }
}
