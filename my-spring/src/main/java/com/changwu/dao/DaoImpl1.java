package com.changwu.dao;

import com.changwu.anno.CDao;
import com.changwu.anno.CService;

/**
 * @Author: Changwu
 * @Date: 2019/10/10 18:32
 */
@CDao
public class DaoImpl1 implements Dao {
    @Override
    public void query() {
        System.out.println("DaoImpl........hahahahahahahahahah..........");
    }
}
