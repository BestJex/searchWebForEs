package com.gy.cpcsearch.service;

import com.gy.cpcsearch.entity.FieldInfo;
import com.gy.cpcsearch.mapper.FieldInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FieldInfoService {
    @Autowired
    FieldInfoMapper fieldInfoMapper;

    public List<FieldInfo> findAll(){
        return fieldInfoMapper.findAll();
    }

    public List<String> findFieldName(){
        return fieldInfoMapper.findFieldName();
    }

    List<String> getFieldNameByTable(String tableName){
        if(tableName==null||tableName.equals("")){
            return fieldInfoMapper.findFieldName();
        }
        if(tableName.contains(",")){
            List<String> tablesList = new ArrayList<>();
            for(String oneTableName:tableName.split(",")){
                tablesList.addAll(fieldInfoMapper.getFieldNameByTable(oneTableName));
            }
            return tablesList.stream().distinct().collect(Collectors.toList());
        }
        return fieldInfoMapper.getFieldNameByTable(tableName);
    }

    String getDesByFieldAndTable(String tableName,String fieldName){
        return fieldInfoMapper.getDesByFieldAndTable(tableName,fieldName);
    };

}
