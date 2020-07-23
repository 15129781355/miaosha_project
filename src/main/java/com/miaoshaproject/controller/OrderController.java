package com.miaoshaproject.controller;


import com.google.common.util.concurrent.RateLimiter;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.response.CommonReturnType;
import com.miaoshaproject.service.OrderService;
import com.miaoshaproject.service.PromoService;
import com.miaoshaproject.service.model.UserModel;
import com.sun.javafx.tk.TKClipboard;
import com.sun.scenario.effect.impl.prism.PrMergePeer;
import org.apache.catalina.User;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.swing.plaf.basic.BasicTreeUI;
import java.util.concurrent.*;

@Controller("order")
@RequestMapping("/order")
@CrossOrigin(allowCredentials = "true",allowedHeaders = "*")  //允许跨域文件请求
public class OrderController extends BaseController {

    @Autowired
    OrderService orderService;

    @Autowired
    HttpServletRequest httpServletRequest;

    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    PromoService promoService;

    ExecutorService executorService;

    RateLimiter orderRateLimiter;

    @PostConstruct
    public void init(){
        executorService = Executors.newFixedThreadPool(20);  //20个线程大小，使用linkedblockingqueue队列存储任务
        orderRateLimiter = RateLimiter.create(300);
    }

    //生成秒杀令牌
    @RequestMapping(value = "/generatetoken",method = {RequestMethod.POST},consumes={CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType generatetoken(@RequestParam(name="itemId")Integer itemId,
                                          @RequestParam(name="promoId")Integer promoId) throws BusinessException {
        //根据token获取用户信息
        String token = httpServletRequest.getParameterMap().get("token")[0];
        if(StringUtils.isEmpty(token)){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户还未登陆，不能下单");
        }
        //获取用户的登陆信息
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);//浏览器本地缓存和redis的缓存要相同，不然出bug
        if(userModel == null){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户还未登陆，不能下单");
        }
        //获取秒杀访问令牌
        String promoToken = promoService.generateSecondKillToken(userModel.getId(),itemId,promoId);

        if(promoToken == null){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"生成令牌失败");
        }
        //返回对应的结果
        return CommonReturnType.create(promoToken);
    }


    //封装下单请求
    @RequestMapping(value = "/createorder",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType createOrder(@RequestParam(name = "itemId")Integer itemId,
                                        @RequestParam(name = "amount")Integer amount,
                                        @RequestParam(name = "token")String token,
                                        @RequestParam(name = "promoToken",required = false)String promoToken,
                                        @RequestParam(name = "promoId")Integer promoId)throws BusinessException{

        //限流
        if (orderRateLimiter.acquire()<=0){
            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR,"系统繁忙，请稍后再试！");
        }


        //通过url/表单里存储的token来验证
        if(StringUtils.isEmpty(token)){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户还未登陆");
        }
        UserModel loginUser = (UserModel)redisTemplate.opsForValue().get(token);
        if(loginUser==null){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户还未登陆");
        }

        //秒杀活动根据需要判断秒杀令牌
        if(promoToken!=null){
            String inRedisPromoToken = (String) redisTemplate.opsForValue().get("promo_token_"+promoId+"_"+loginUser.getId()+"_"+itemId);
            if(!inRedisPromoToken.equals(promoToken)){
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"秒杀令牌校验失败");
            }
        }

        //通过cookie里的sessionid方式来判断
        Boolean isLogin = (Boolean)httpServletRequest.getSession().getAttribute("IS_LOGIN");
        if(isLogin==null || !isLogin){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户还未登陆");
        }
        loginUser = (UserModel)httpServletRequest.getSession().getAttribute("LOGIN_USER");
        Integer userId = loginUser.getId();




        //同步调用线程池的submit方法
        //拥塞窗口为20的等待队列，用来队列化泄洪
        Future<?> future = executorService.submit(new Callable<Object>() {
            @Override
            public Object call() throws BusinessException{
                orderService.createOrder(userId, itemId, promoId, amount);
                return null;
            }
        });
        try {
            future.get();
        } catch (InterruptedException e) {
            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR);
        } catch (ExecutionException e) {
            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR);
        }

        return CommonReturnType.create(null);
    }

}
