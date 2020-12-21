package com.gy.cpcsearch.controller;

import com.alibaba.fastjson.JSONObject;
import com.gy.cpcsearch.service.SearchService;
import com.gy.cpcsearch.service.TableInfoService;

import java.util.ArrayList;
import java.util.List;

import com.gy.cpcsearch.utils.ElasticUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/search")
public class SearchController {
    @Autowired
    SearchService searchService;

    @Autowired
    TableInfoService tableInfoService;

    @Autowired
    ElasticUtil elasticUtil;

//    @GetMapping("/")
//    public String getList(@RequestParam(value = "menuid") String menuid){
//        if(!menuid.equals("150371841087979520")){
//            return null;
//        } else{
//            return "true";
//        }
//    }

    @GetMapping("getList")
    public JSONObject getList(@RequestParam(value = "search_value") String searchValue,
                              @RequestParam(value = "source_name", required = false) String sourceName,
                              @RequestParam(value = "page", required = false) Integer startPage,
                              @RequestParam(value = "page_size", required = false) Integer pageSize,
                              @RequestParam(value = "start_time", required = false) String startTime,
                              @RequestParam(value = "end_time", required = false) String endTime,
                              @RequestParam(value = "sort", required = false) String sortString,
                              @RequestParam(value = "menuid") String menuid){
        if(!menuid.equals("150371841087979520")){
            return null;
        }
        if(searchValue.contains("\\")){
            searchValue = searchValue.replace("\\","\\\\");
        }
        String indexName = tableInfoService.getNameByAlias(sourceName);
        if(!sourceName.equals("") && indexName == null){
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("code",-1);
            jsonObject.put("error","当前数据来源不存在");
            return jsonObject;
        }
        if(pageSize>100){
            pageSize = 100;
        }
        JSONObject jsonObject = searchService.getResultJson(indexName,searchValue,startPage,pageSize,startTime,endTime,sortString);
        jsonObject.put("page",1);
        if(startPage!=null&&startPage>0){
            jsonObject.put("page",startPage);
        }
        jsonObject.put("code",1);
//        List<String> allSource = new ArrayList<>();
//        allSource.add("全库");
//        allSource.addAll(tableInfoService.findAllAlias());
//        jsonObject.put("all_source",allSource);
        jsonObject.put("all_source",tableInfoService.getAliasType());
        return jsonObject;
    }

    @GetMapping("getDetail")
    public JSONObject getDetail(@RequestParam(value = "source_name") String sourceName,
                                @RequestParam(value = "id", required = false) String idString,
                                @RequestParam(value = "menuid") String menuid){
        if(!menuid.equals("150371841087979520")){
            return null;
        }
        String indexName = tableInfoService.getNameByAlias(sourceName);
        return elasticUtil.getById(indexName,idString);
    }
}
