package com.changwu.service;

import com.changwu.dao.DaoImpl1;

/**
 * @Author: Changwu
 * @Date: 2019/11/8 22:16
 */
public class UserServiceImpl4 implements UserService {

    DaoImpl1 daoImpl;

    @Override
    public void find() {
        this.daoImpl.query();
    }

    public void setDaoImpl(DaoImpl1 daoImpl1) {
        this.daoImpl = daoImpl1;
    }
}
