package com.gy.cpcsearch;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gy.cpcsearch.service.FieldInfoService;
import com.gy.cpcsearch.service.TableInfoService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

@SpringBootTest
class SearchApplicationTests {
    @Autowired
    FieldInfoService fieldInfoService;

//    @Autowired
//    SearchService testService;

    @Autowired
    TableInfoService tableInfoService;

    @Test
    void contextLoads() {
        JSONArray jsonArray = tableInfoService.getAliasType();
        System.out.println(jsonArray);
    }

}
