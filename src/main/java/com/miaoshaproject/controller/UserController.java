package com.miaoshaproject.controller;

import com.miaoshaproject.controller.viewobject.UserVO;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.response.CommonReturnType;
import com.miaoshaproject.service.impl.UserServiceImpl;
import com.miaoshaproject.service.model.UserModel;
import org.apache.tomcat.util.security.MD5Encoder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import sun.security.provider.MD5;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Controller("user")
@RequestMapping("/user")
@CrossOrigin(allowCredentials = "true",allowedHeaders = "*")  //允许跨域文件请求
public class UserController extends BaseController{

    @Autowired
    UserServiceImpl userService;


    //单例对象但是底层有ThreadLocal的map，多线程下安全
    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private RedisTemplate redisTemplate;


    //用户登陆接口
    @RequestMapping(value = "/login",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType login(@RequestParam(name = "telphone") String telephone,
                                  @RequestParam(name = "password") String password) throws BusinessException{
        //入参检验
        if(StringUtils.isEmpty(telephone) || StringUtils.isEmpty(password)){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }
        //用户登陆服务，校验用户登陆是否合法
        UserModel userModel = userService.validateLogin(telephone, getMD5String(password));
        //将用户登陆凭证加入到session内

        //基于token存储sessionid的形式，生成uuid代替即可全局唯一
        String uuidToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(uuidToken,userModel);
        //不设置失效时间，使用js的localstory是永不失效，需要js控制的。
//        redisTemplate.expire(uuidToken,1, TimeUnit.HOURS);

        //基于cookie存储sessionid的形式（spring底层使用的redis存储session）   redis上设置了一个小时过期！！！
        this.httpServletRequest.getSession().setAttribute("IS_LOGIN",true);
        this.httpServletRequest.getSession().setAttribute("LOGIN_USER",userModel);
        return CommonReturnType.create(uuidToken); //传给前端页面,用localStorage进行存储

    }

    //用户注册接口
    @RequestMapping(value = "/register",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType register(@RequestParam(name = "telphone")String telephone,
                                    @RequestParam(name = "otpCode")String otpCode,
                                    @RequestParam(name = "name")String name,
                                    @RequestParam(name = "gender")Integer gender,
                                    @RequestParam(name = "password")String password,
                                    @RequestParam(name = "age")Integer age) throws BusinessException{
        //验证手机和对应的otpcode相符合
        String inSessionOtpCode = (String) httpServletRequest.getSession().getAttribute(telephone);
        if(!inSessionOtpCode.equals(otpCode)){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"短信验证码不正确");
        }
        //用户的注册流程
        UserModel userModel = new UserModel();
        userModel.setName(name);
        userModel.setTelphone(telephone);
        userModel.setAge(age);
        userModel.setGender(gender.byteValue());
        userModel.setRegisterMode("byphone");
        userModel.setEncrptPassword(getMD5String(password));
        userService.register(userModel);
        return CommonReturnType.create(null);
    }



    //用户获取otp短信接口   请求是consumes消费方，响应是providers生产方
    @RequestMapping(value = "/getotp",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType getOtp(@RequestParam(name = "telphone") String telephone){
        //按照一定规则生成OTP验证码
        Random random = new Random();
        int randomInt = random.nextInt(9999);
        randomInt+=10000;
        String otpCode = String.valueOf(randomInt);
        //将OTP验证码与对应用户手机关联，使用httpSession的方式绑定手机号和code
        httpServletRequest.getSession().setAttribute(telephone,otpCode);
        //将OTP验证码通过短信发送给用户，省略
        System.out.println("telephone:"+telephone+" &otpCode:"+otpCode);
        return CommonReturnType.create(null);
    }


    @RequestMapping("/get")
    @ResponseBody  //返回json串
    public CommonReturnType getUser(@RequestParam(name="id") Integer id) throws BusinessException{
        UserModel userModel = userService.getUserById(id);
        //用户信息不存在
        if(userModel==null){
            throw new BusinessException(EmBusinessError.USER_NOT_EXIST);
        }
        //转化为用户VO
        UserVO userVO = convertFromModel(userModel);
        //返回通用对象
        return CommonReturnType.create(userVO);
    }

    private UserVO convertFromModel(UserModel userModel){
        if(userModel==null){
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(userModel,userVO);
        return userVO;
    }

    public static String getMD5String(String str) {
        try {
            // 生成一个MD5加密计算摘要
            MessageDigest md = MessageDigest.getInstance("MD5");
            // 计算md5函数
            md.update(str.getBytes());
            // digest()最后确定返回md5 hash值，返回值为8位字符串。因为md5 hash值是16位的hex值，实际上就是8位的字符
            // BigInteger函数则将8位的字符串转换成16位hex值，用字符串来表示；得到字符串形式的hash值
            //一个byte是八位二进制，也就是2位十六进制字符（2的8次方等于16的2次方）
            return new BigInteger(1, md.digest()).toString(16);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }




}
