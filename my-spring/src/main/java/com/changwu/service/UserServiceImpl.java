package com.changwu.service;

import com.changwu.anno.CAutowired;
import com.changwu.anno.CService;
import com.changwu.dao.DaoImpl1;

/**
 * @Author: Changwu
 * @Date: 2019/10/10 18:34
 */
@CService("myService1")
public class UserServiceImpl implements UserService {

    @CAutowired
    DaoImpl1 daoImpl1111;

    @Override
    public void find() {
        daoImpl1111.query();
    }

    public void setDaoImpl(DaoImpl1 daoImpl1){
        this.daoImpl1111=daoImpl1;
    }
}
