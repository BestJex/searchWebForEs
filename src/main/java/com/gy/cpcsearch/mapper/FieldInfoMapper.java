package com.gy.cpcsearch.mapper;

import com.gy.cpcsearch.entity.FieldInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FieldInfoMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(FieldInfo record);

    int insertSelective(FieldInfo record);

    FieldInfo selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(FieldInfo record);

    int updateByPrimaryKey(FieldInfo record);

    List<FieldInfo> findAll();

    List<String> findFieldName();

    List<String> getFieldNameByTable(@Param("tableName") String tableName);

    String getDesByFieldAndTable(@Param("tableName") String tableName,@Param("fieldName") String fieldName);

}