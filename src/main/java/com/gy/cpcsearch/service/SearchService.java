package com.gy.cpcsearch.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Liqifeng
 * 用于处理搜索数据
 */
@Service
public class SearchService {
    @Autowired
    FieldInfoService fieldInfoService;

    @Autowired
    TableInfoService tableInfoService;

    /**
     * 将前端传过来的搜索条件组合切片
     * @param inputString 搜索条件
     * @return 组合后的条件
     */
    JSONObject splitString(String inputString){
        JSONObject resultJson = new JSONObject();
        String andSplit = "&&";
        String orSplit = "\\|\\|";
        JSONArray andArray = new JSONArray();
        JSONArray orArray = new JSONArray();
        String [] andSplitList = inputString.split(andSplit);
        for(int i =0;i<andSplitList.length;i++){
            String out = andSplitList[i];
            String[] orSplitList = out.split(orSplit);
            if(i!=0&&i!=andSplitList.length-1){
                for(int j = 0;j<orSplitList.length;j++){
                    if(j==0||j==orSplitList.length-1){
                        andArray.add(orSplitList[j]);
                    }
                    if(orSplitList.length>1){
                        orArray.add(orSplitList[j]);
                    }
                }
            } else if (i==0){
                for(int j = 0;j<orSplitList.length;j++){
                    if(j==orSplitList.length-1&&andSplitList.length>1){
                        andArray.add(orSplitList[j]);
                    }
                    if(orSplitList.length>1){
                        orArray.add(orSplitList[j]);
                    }
                }
            } else if(i==andSplitList.length-1){
                for(int j = 0;j<orSplitList.length;j++){
                    if(j==0){
                        andArray.add(orSplitList[j]);
                    }
                    if(orSplitList.length>1){
                        orArray.add(orSplitList[j]);
                    }
                }
            }
        }
        resultJson.put("and",andArray);
        resultJson.put("or",orArray);
        return resultJson;
    }

    public String getJsonStringRec(JSONObject jsonObject, String keyString){
        return getJsonStringRec(jsonObject,keyString,0);
    }

    public String getJsonStringRec(JSONObject jsonObject, String keyString, int i){
        String key = keyString.split("\\.")[i];
        try {
            JSONObject resultJson = jsonObject.getJSONObject(key);
            return getJsonStringRec(resultJson,keyString,i+1);
        } catch (Exception e){
            String outString = jsonObject.getString(key);
            try{
                JSONArray jsonArray = JSONArray.parseArray(outString);
                String [] keyArray = keyString.split("\\.");
                return jsonArray.getJSONObject(0).getString(keyArray[keyArray.length-1]);
            } catch (Exception e2){
                return jsonObject.getString(key);
            }
        }
    }


    /**
     * 通过递归的方式获取json中的字符串
     * @param jsonObject json数据
     * @param keyString 键值
     * @return 具体字段值
     */
//    private String getString(JSONObject jsonObject, String keyString){
//        return getString(jsonObject,keyString,0);
//    }
//
//    private String getString(JSONObject jsonObject, String keyString, int i){
//        String key = keyString.split("\\.")[i];
//        try {
//            JSONObject resultJson = jsonObject.getJSONObject(key);
//            return getString(resultJson,keyString,i+1);
//        } catch (Exception e){
//            return jsonObject.getString(key);
//        }
//    }

    /**
     * 调整数据格式
     * @param jsonArray 原始数据格式
     * @return 调整过的数据格式
     */
    private JSONArray getResultArray(JSONArray jsonArray){
        JSONArray resultAyyay = new JSONArray();
        for(int i = 0;i<jsonArray.size();i++){
            JSONObject dataJson = new JSONObject();
            //获取单独的一条数据
            JSONObject oneJson = jsonArray.getJSONObject(i);
            //获取数据中的详情数据
            JSONObject sourceJson = oneJson.getJSONObject("_source");
            //获取索引名称
            String indexName = oneJson.getString("_index");
            String aliasName = tableInfoService.getAliasByName(indexName);
            //根据索引名称查找别名
            List<String> fieldNames = fieldInfoService.getFieldNameByTable(indexName);
            JSONArray valueArray = new JSONArray();
            //获取每个字段的值
            for(String fieldName :fieldNames){
                String fieldValue = getJsonStringRec(sourceJson,fieldName);
                if(fieldValue!=null){
                    JSONObject valueJson = new JSONObject();
                    valueJson.put("data_name",fieldInfoService.getDesByFieldAndTable(indexName,fieldName));
                    if(fieldValue.length()>=46){
                        fieldValue = fieldValue.substring(0,46)+"...";
                    }
                    valueJson.put("data_value",fieldValue);
                    valueArray.add(valueJson);
                }
            }
            String idString = oneJson.getString("_id");
            dataJson.put("id",idString);
            dataJson.put("value",valueArray);
            dataJson.put("source_name",aliasName);
            resultAyyay.add(dataJson);
        }
        return resultAyyay;
    }


    /**
     * 调整结果字段格式
     * @param indexName 索引名称
     * @param searchValue 搜索词
     * @param page 页数
     * @param pageSize 数据量
     * @return 结果数据
     */
    public JSONObject getResultJson(String indexName,String searchValue, Integer page, Integer pageSize,String startTime,String endTime,String sortString){
        JSONObject searchResult = search(indexName,searchValue,page,pageSize,startTime,endTime,sortString);
        JSONObject resultJson = new JSONObject();
        //如果当前库表为空，则返回以下信息
        if(searchResult==null){
            resultJson.put("count",0);
            resultJson.put("page_size",0);
            resultJson.put("result_data",new JSONArray());
            return resultJson;
        }
        int count = searchResult.getJSONObject("total").getInteger("value");
        resultJson.put("count",count);
        JSONArray jsonArray = searchResult.getJSONArray("hits");
        resultJson.put("page_size",jsonArray.size());
        JSONArray resultArray = getResultArray(jsonArray);
        resultJson.put("result_data",resultArray);
        return resultJson;
    }


    public String getSearchString(String indexName,String searchString, int from , int pageSize, String startTime,String endTime,String sortString){
        List<String> fieldList = fieldInfoService.getFieldNameByTable(indexName);
        JSONObject searchJson = new JSONObject();
        searchJson.put("from",from);
        searchJson.put("size",pageSize);
        searchJson.put("track_total_hits",true);
        JSONObject queryJson = new JSONObject();
        JSONObject boolJson = new JSONObject();
        JSONArray zuheArray = new JSONArray();
        //构建搜索条件
        if((searchString.contains("&&")||searchString.contains("||"))&&!(searchString.contains("&&")&&searchString.contains("||"))){
            if(searchString.contains("&&")){
                String[] splitString = searchString.split("&&");
                for(String string:splitString){
                    JSONObject queryString = new JSONObject();
                    JSONObject queryValue = new JSONObject();
                    queryValue.put("query","\""+string+"\"");
                    queryString.put("query_string",queryValue);
                    zuheArray.add(queryString);
                }
                boolJson.put("must",zuheArray);
            }
            if(searchString.contains("||")){
                String[] splitString = searchString.split("\\|\\|");
                for(String string:splitString){
                    JSONObject queryString = new JSONObject();
                    JSONObject queryValue = new JSONObject();
                    queryValue.put("query","\""+string+"\"");
                    queryString.put("query_string",queryValue);
                    zuheArray.add(queryString);
                }
                boolJson.put("should",zuheArray);
            }
        } else{
            if(searchString.trim().equals("")){
                boolJson.put("must",zuheArray);
            } else{
                JSONObject queryString = new JSONObject();
                JSONObject queryValue = new JSONObject();
                queryValue.put("query","\""+searchString+"\"");
                queryString.put("query_string",queryValue);
                zuheArray.add(queryString);
                boolJson.put("must",zuheArray);
            }
        }
        JSONObject timeFilterJson = null;
        if((startTime!=null&&!startTime.equals(""))&&(endTime!=null&&!endTime.equals(""))){
            String timeString = "{\"range\": {\"es_version_data.standard_timestamp\": {\"gt\":startTime,\"lt\":endTime}}}";
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date startDate = null;
            Date endDate = null;
            try {
                startDate = simpleDateFormat.parse(startTime);
                endDate = simpleDateFormat.parse(endTime);
                timeString = timeString.replace("startTime",Long.toString(startDate.getTime()));
                timeString = timeString.replace("endTime",Long.toString(endDate.getTime()));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            timeFilterJson = JSON.parseObject(timeString);
        }
        if((startTime==null||startTime.equals(""))&&(endTime!=null&&!endTime.equals(""))){
            String timeString = "{\"range\": {\"es_version_data.standard_timestamp\": {\"lt\":endTime}}}";
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date endDate = null;
            try {
                endDate = simpleDateFormat.parse(endTime);
                timeString = timeString.replace("endTime",Long.toString(endDate.getTime()));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            timeFilterJson = JSON.parseObject(timeString);
        }
        if((startTime!=null&&!startTime.equals(""))&&(endTime==null||endTime.equals(""))){
            String timeString = "{\"range\": {\"es_version_data.standard_timestamp\": {\"gt\":startTime}}}";
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date startDate = null;
            try {
                startDate = simpleDateFormat.parse(startTime);
                timeString = timeString.replace("startTime",Long.toString(startDate.getTime()));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            timeFilterJson = JSON.parseObject(timeString);
        }
        if(timeFilterJson!=null){
            JSONArray mustArray = boolJson.getJSONArray("must");
            if (mustArray == null || mustArray.size() == 0) {
                JSONArray jsonArray = new JSONArray();
                jsonArray.add(timeFilterJson);
                boolJson.put("must",jsonArray);
            } else{
                mustArray.add(timeFilterJson);
            }
        }
        queryJson.put("bool",boolJson);
        searchJson.put("query",queryJson);
//        String timeString = tableInfoService.getTimeFieldByName(indexName);
//        if(timeString!=null){
//            String paixuString = "[{\""+timeString+"\":{\"order\":\"desc\"}}]";
//            JSONArray paixuArray = (JSONArray) JSONArray.parse(paixuString);
//            searchJson.put("sort",paixuArray);
//        }
        if(sortString==null||sortString.equals("")){
            sortString = "desc";
        }
        String paixuString = "[{\"es_version_data.standard_timestamp\":{\"order\":\""+sortString+"\"}}]";
        JSONArray paixuArray = (JSONArray) JSONArray.parse(paixuString);
        searchJson.put("sort",paixuArray);
        searchJson.put("_source",fieldList);
        return searchJson.toString();
    }

    /**
     * 根据数据库名称与搜索字段获取数据
     * @param indexName 索引名称
     * @param searchValue 搜索词
     * @param page 当前页数
     * @param pageSize 数据量
     * @return 搜索结果
     */
    public JSONObject search(String indexName,String searchValue,Integer page,Integer pageSize,String startTime,String endTime,String desc){
        int from = 0;
        if((page!=null&&pageSize!=null)&&(page > 0 && pageSize > 0)){
            from = ((page-1)*pageSize);
        } else{
            pageSize = 10;
        }

        Unirest.setTimeouts(0, 0);
        String searchJson = getSearchString(indexName,searchValue,from,pageSize,startTime,endTime,desc);
        String urlString = "";
        if(indexName!=null&&!indexName.equals("")){
            urlString = String.format("http://cdh1:9200/%s/_search",indexName);

        } else{
            String allTableName = tableInfoService.findAllName().stream().collect(Collectors.joining(","));
            urlString = "http://cdh1:9200/"+allTableName+"/_search";
        }
        HttpResponse<String> response = null;
        try {
            response = Unirest.post(urlString)
                    .header("Content-Type", "application/json")
                    .body(searchJson).asString();
            return JSONObject.parseObject(response.getBody()).getJSONObject("hits");
        } catch (UnirestException e) {
            e.printStackTrace();
        }
        return null;
    }

}
