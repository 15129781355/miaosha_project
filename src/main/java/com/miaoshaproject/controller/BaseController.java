package com.miaoshaproject.controller;

import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.response.CommonReturnType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;

public class BaseController {

    /**
     * application/x-www-form-urlencoded
     * ajax请求中content-type：application/x-www-form-urlencoded代表参数以键值对传递给后台
     * 如@RequestParam("param") String param；也可以用类接收User user
     *
     * ajax请求中content-type：application/json代表参数以json字符串传递给后台，
     * 如：@RequestBody Map<String, Object> map 也可以使用类接收@RequestBody User user
     *
     */
    public static final String CONTENT_TYPE_FORMED = "application/x-www-form-urlencoded";


    //定义exceptionHandler解决Exception异常
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Object handlerException(HttpServletRequest request, Exception ex){

        HashMap<String,Object> responseData = new HashMap<String,Object>();
        if(ex instanceof BusinessException){
            BusinessException businessException = (BusinessException)ex;
            //设置data信息
            responseData.put("errorCode",businessException.getErrorCode());
            responseData.put("errorMsg",businessException.getErrorMsg());

        }else {
            //设置data信息
            responseData.put("errorCode", EmBusinessError.UNKNOWN_ERROR.getErrorCode());
            responseData.put("errorMsg",EmBusinessError.UNKNOWN_ERROR.getErrorMsg());

        }
        return CommonReturnType.create(responseData,"fail");
    }


}
