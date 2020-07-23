package com.miaoshaproject.error;

//包装类设计模式
public class BusinessException extends Exception implements CommonError{

    private CommonError commonError;

    //直接接收EmBusinessError的传参用于构造业务异常
    public BusinessException(CommonError commonError){
        super();
        this.commonError = commonError;
    }

    //接收自定义errorMsg的方式构造业务异常
    public BusinessException(CommonError commonError,String msg){
        super();
        this.commonError = commonError;
        this.commonError.setErrorMsg(msg);
    }

    @Override
    public int getErrorCode() {
        return this.commonError.getErrorCode();
    }

    @Override
    public String getErrorMsg() {
        return this.commonError.getErrorMsg();
    }

    @Override
    public CommonError setErrorMsg(String errorMsg) {
        this.commonError.setErrorMsg(errorMsg);
        return this;
    }
}
