package com.changwu.service;

import com.changwu.anno.CAutowired;
import com.changwu.anno.CService;
import com.changwu.dao.DaoImpl1;

/**
 * @Author: Changwu
 * @Date: 2019/11/8 22:16
 */
public class UserServiceImpl3 implements UserService {

    DaoImpl1 daoImpl;

    public UserServiceImpl3(DaoImpl1 daoImpl) {
        this.daoImpl = daoImpl;
    }

    @Override
    public void find() {
        daoImpl.query();
    }

    public void setDaoImpl(DaoImpl1 daoImpl1) {
        this.daoImpl = daoImpl1;
    }
}
