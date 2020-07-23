package com.miaoshaproject.service;

import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.service.model.ItemModel;

import java.util.List;

public interface ItemService {
    //创建商品
    ItemModel createItem(ItemModel itemModel) throws BusinessException;

    //商品列表浏览
    List<ItemModel> listItem() throws BusinessException;

    //商品详情浏览
    ItemModel getItemById(Integer id) throws BusinessException;

    //库存扣减
    boolean decreaseStock(Integer itemId,Integer amount)throws BusinessException;

    //商品销量增加
    void increaseSales(Integer itemId,Integer amount)throws BusinessException;


    //验证item和promo的缓存模型
    ItemModel getItemByIdCache(Integer id) throws BusinessException;

}
