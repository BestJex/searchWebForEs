package com.gy.cpcsearch.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpHost;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.*;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Liqifeng
 * 2020年2月14日 11点37分
 * 当前类为Elasticsearch工具函数类
 * 主要包括数据批量插入，数据查询，数据更新等操作
 */

@Component
public class ElasticUtil {


    public RestHighLevelClient client;

    public RestHighLevelClient getClient(){
        return client;
    }

//    @Autowired
//    public ElasticUtil(ConfUtil confUtil){
//        client = new RestHighLevelClient(
//                RestClient.builder(
//                        new HttpHost(confUtil.esHost,confUtil.esPort, "http")
//                ));
//
//    }

    public ElasticUtil(){
        client = new RestHighLevelClient(
                RestClient.builder(
//                        new HttpHost(confUtil.host,confUtil.port, "http")
//                        new HttpHost("wx.puhom.com",29200, "http")
                        new HttpHost("cdh1",9200, "http")
                ));
    }


    public JSONArray searchMoreRange(String index, JSONObject searchJson, JSONArray rangeArray, int page, int pageSize){
        //构建搜索客户端
        SearchRequest searchRequest = new SearchRequest(index);
//        searchRequest.routing("routing"); // 设置 routing 参数
//        searchRequest.preference("_local");  // 配置搜索时偏爱使用本地分片，默认是使用随机分片
        //构建搜索请求
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //遍历查询条件
        BoolQueryBuilder qb = QueryBuilders.boolQuery();
        for(String keyString : searchJson.keySet()){
            Object searchValue = searchJson.get(keyString);
            QueryBuilder queryBuilder = null;
            //如果搜索对象为String类型，且模糊查询开启
            queryBuilder=QueryBuilders.matchQuery(keyString, searchValue);
            qb = qb.must(queryBuilder);
        }
        searchSourceBuilder.query(qb);
        for(int i =0;i<rangeArray.size();i++){
            JSONObject rangeJson = rangeArray.getJSONObject(i);
            RangeQueryBuilder rangeQueryBuilder = getRange(rangeJson);
            if(rangeQueryBuilder!=null){
                qb.must(rangeQueryBuilder);
            }

        }
        //如果存在分页参数，则开启分页
        if(page > 0 && pageSize > 0){
            searchSourceBuilder.from((page-1)*pageSize);
            searchSourceBuilder.size(pageSize);
        } else{
            searchSourceBuilder.from(0);
            searchSourceBuilder.size(10);
        }
        searchSourceBuilder.sort("_id");
        searchRequest.source(searchSourceBuilder);
        JSONArray resultArray = new JSONArray();
        try {
            //提交搜索请求
            RestHighLevelClient client = getClient();
            RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
            builder.setHttpAsyncResponseConsumerFactory(new HttpAsyncResponseConsumerFactory.HeapBufferedResponseConsumerFactory(1048576000));
            SearchResponse searchResponse = client.search(searchRequest,builder.build());
            SearchHits hits = searchResponse.getHits();
            SearchHit[] searchHits = hits.getHits();

            //遍历返回的数据
            for(SearchHit  hit: searchHits){
                String hitString = hit.getSourceAsString();
                resultArray.add(JSON.parseObject(hitString));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resultArray;
    }

    /**
     * 根据范围参数构建range请求
     * @param rangeJson 范围参数
     * @return 构建好的请求
     */
    public RangeQueryBuilder getRange(JSONObject rangeJson){
        String rangeString = rangeJson.getString("rangeString");
        if(rangeString!=null){
            RangeQueryBuilder rangeQuery = null;
            try{
                long minValue = rangeJson.getLong("minValue");
                rangeQuery = QueryBuilders.rangeQuery(rangeString);
                if(minValue!=0){
                    rangeQuery.from(minValue);
                }
            } catch (Exception ignored){
            }
            try{
                long maxValue = rangeJson.getLong("maxValue");
                if(rangeQuery==null){
                    rangeQuery = QueryBuilders.rangeQuery(rangeString);
                }
                if(maxValue!=0){
                    rangeQuery.to(maxValue);
                }
            } catch (Exception ignored){
            }
            return rangeQuery;
        } else{
            return null;
        }
    }

    /**
     * 异步方式创建es中的索引
     * @param index 索引名称
     * @param share 分区数量
     * @param replicas 副本数量
     */
    public boolean createIndex(String index, int share, int replicas,int fieldNum)  {
        //创建索引请求
        CreateIndexRequest indexRequest = new CreateIndexRequest(index);
        //创建索引的详细信息
        indexRequest.settings(Settings.builder().put("index.number_of_shards",share)
                .put("index.number_of_replicas", replicas)
                .put("analysis.analyzer.default.tokenizer","standard")
                .put("index.mapping.total_fields.limit",fieldNum)
                .put("index.max_result_window",20000000)
        );
//        try {
//            JSONObject jsonObject = new JSONObject();
//            jsonObject.put("max_result_window",20000000);
//            HttpUtil.sendPut("http://cdh1:9200/*/_settings",jsonObject.toString());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        RestHighLevelClient client = getClient();
        try {
            client.indices().create(indexRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * 异步方式创建es中的索引
     * @param index 索引名称
     */
    public boolean createIndex(String index){
        return createIndex(index,5,0,1000);
    }

    /**
     * 获取索引中的数据总条数，并将搜索参数默认为null
     * @param index 索引名称
     * @return 数据条数
     */
    public int getCount(String index){
        return getCount(index,new JSONObject(), new JSONObject());
    }

    public long getCount(String index,JSONObject jsonObject){
        return getCount(index, jsonObject, new JSONObject());
    }


    /**
     * 模糊搜索条数
     * @param index 索引
     * @param searchJson 查询条件
     * @return 查询到的数据
     */
    public int getCountMohu(String index, JSONObject searchJson){
        //构建搜索客户端
        SearchRequest searchRequest = new SearchRequest(index);
        //构建搜索请求
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //遍历查询条件
        BoolQueryBuilder qb = QueryBuilders.boolQuery();
        for(String keyString : searchJson.keySet()){
            Object searchValue = searchJson.get(keyString);
            QueryBuilder queryBuilder = null;
            //如果搜索对象为String类型，且模糊查询开启
            queryBuilder=QueryBuilders.matchQuery(keyString, searchValue).fuzziness(Fuzziness.AUTO);
            qb = qb.must(queryBuilder);
        }
        searchSourceBuilder.query(qb);
        searchRequest.source(searchSourceBuilder.size(10000));
        try {
            //提交搜索请求
            RestHighLevelClient client = getClient();
            SearchResponse searchResponse = client.search(searchRequest,RequestOptions.DEFAULT);
            SearchHits hits = searchResponse.getHits();

            return hits.getHits().length;
            //遍历返回的数据
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 获取索引中的数据总条数
     * @param index 索引名称
     * @param searchJson 搜索参数
     * @return 数据条数
     */
    public int getCount(String index,JSONObject searchJson,JSONObject rangeJson){
        CountRequest countRequest = new CountRequest(index);
        CountResponse countResponse = null;
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder qb = QueryBuilders.boolQuery();
        if(searchJson.keySet().size()!=0){
            for(String keyString : searchJson.keySet()){
                Object searchValue = searchJson.get(keyString);
                qb = qb.must(QueryBuilders.matchQuery(keyString,searchValue));
            }
        }
        if(rangeJson.keySet().size()!=0){
            RangeQueryBuilder rangeQueryBuilder = getRange(rangeJson);
            if(rangeQueryBuilder!=null){
                qb.must(rangeQueryBuilder);
            }
        }
        searchSourceBuilder.query(qb);
        countRequest.source(searchSourceBuilder);
        try {
            RestHighLevelClient client = getClient();
            countResponse = client.count(countRequest,RequestOptions.DEFAULT);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return (int)countResponse.getCount();
    }

    /**
     * 将搜索到的数据根据时间字段进行排序
     * @param index 索引名称
     * @param timeKey 要排序的时间字段
     * @param zheng true为正序，false为倒序
     * @param page 当前页数
     * @param pageSize 页面数据条数
     * @return
     */
    public JSONArray searchByTime(String index, JSONObject searchJson,String timeKey,boolean zheng,int page, int pageSize){
        JSONArray resultArray = new JSONArray();
        SearchRequest searchRequest = new SearchRequest(index);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder qb = QueryBuilders.boolQuery();
        for(String keyString : searchJson.keySet()){
            Object searchValue = searchJson.get(keyString);
            QueryBuilder queryBuilder = null;
            //如果搜索对象为String类型，且模糊查询开启
            queryBuilder=QueryBuilders.matchQuery(keyString, searchValue);
            qb = qb.must(queryBuilder);
        }
        searchSourceBuilder.query(qb);
        //如果存在分页参数，则开启分页
        if(page > 0 && pageSize > 0){
            searchSourceBuilder.from((page-1)*pageSize);
            searchSourceBuilder.size(pageSize);
        } else{
            searchSourceBuilder.from(0);
            searchSourceBuilder.size(10);
        }
        if(zheng){
            searchSourceBuilder.sort(timeKey, SortOrder.DESC);
        } else{
            searchSourceBuilder.sort(timeKey, SortOrder.ASC);
        }
        searchRequest.source(searchSourceBuilder);
        RestHighLevelClient client = getClient();
        SearchResponse searchResponse = null;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            SearchHits hits = searchResponse.getHits();
            SearchHit[] searchHits = hits.getHits();
            //遍历返回的数据
            for(SearchHit  hit: searchHits){
                String hitString = hit.getSourceAsString();
                resultArray.add(JSON.parseObject(hitString));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resultArray;
    }

    /**
     * 根据查询条件在表搜索数据
     * @param index 索引
     * @param searchJson 查询条件
     * @param page 按照页数查询
     * @param pageSize 限制查询数量
     * @param mohu 是否开启模糊查询
     * @return 查询到的数据
     */
    public JSONArray searchDoc(String index, JSONObject searchJson, JSONObject rangeJson,boolean mohu , int page, int pageSize, String[] excludeFields){
        //构建搜索客户端
        SearchRequest searchRequest = new SearchRequest(index);
//        searchRequest.routing("routing"); // 设置 routing 参数
//        searchRequest.preference("_local");  // 配置搜索时偏爱使用本地分片，默认是使用随机分片
        //构建搜索请求
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //遍历查询条件
        BoolQueryBuilder qb = QueryBuilders.boolQuery();
        for(String keyString : searchJson.keySet()){
            Object searchValue = searchJson.get(keyString);
            QueryBuilder queryBuilder = null;
            //如果搜索对象为String类型，且模糊查询开启
            if(searchValue instanceof String &&mohu){
                queryBuilder=QueryBuilders.matchQuery(keyString, searchValue).fuzziness(Fuzziness.AUTO);
            } else{
                queryBuilder=QueryBuilders.matchQuery(keyString, searchValue);

            }
            qb = qb.must(queryBuilder);
        }
        if(excludeFields!=null){
            searchSourceBuilder.fetchSource(null, excludeFields);
        }
        searchSourceBuilder.query(qb);
        RangeQueryBuilder rangeQueryBuilder = getRange(rangeJson);
        if(rangeQueryBuilder!=null){
            qb.must(rangeQueryBuilder);
        }
        //如果存在分页参数，则开启分页
        if(page > 0 && pageSize > 0){
            searchSourceBuilder.from((page-1)*pageSize);
            searchSourceBuilder.size(pageSize);
        } else{
            searchSourceBuilder.from(0);
            searchSourceBuilder.size(10);
        }
        searchSourceBuilder.sort("_id");
        searchRequest.source(searchSourceBuilder);
        JSONArray resultArray = new JSONArray();
        try {
            //提交搜索请求
            RestHighLevelClient client = getClient();
            RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
            builder.setHttpAsyncResponseConsumerFactory(new HttpAsyncResponseConsumerFactory.HeapBufferedResponseConsumerFactory(1048576000));
            SearchResponse searchResponse = client.search(searchRequest,builder.build());
//            SearchResponse searchResponse = client.search(searchRequest,RequestOptions.DEFAULT);
            SearchHits hits = searchResponse.getHits();
            SearchHit[] searchHits = hits.getHits();
            //遍历返回的数据
            for(SearchHit  hit: searchHits){
                String hitString = hit.getSourceAsString();
                resultArray.add(JSON.parseObject(hitString));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resultArray;
    }

    public JSONArray searchDocID(String index, JSONObject searchJson, JSONObject rangeJson,int page, int pageSize){
        //构建搜索客户端
        SearchRequest searchRequest = new SearchRequest(index);
//        searchRequest.routing("routing"); // 设置 routing 参数
//        searchRequest.preference("_local");  // 配置搜索时偏爱使用本地分片，默认是使用随机分片
        //构建搜索请求
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //遍历查询条件
        BoolQueryBuilder qb = QueryBuilders.boolQuery();
        for(String keyString : searchJson.keySet()){
            Object searchValue = searchJson.get(keyString);
            QueryBuilder queryBuilder = null;
            //如果搜索对象为String类型，且模糊查询开启
            queryBuilder=QueryBuilders.matchQuery(keyString, searchValue);
            qb = qb.must(queryBuilder);
        }
        searchSourceBuilder.query(qb);
        RangeQueryBuilder rangeQueryBuilder = getRange(rangeJson);
        if(rangeQueryBuilder!=null){
            qb.must(rangeQueryBuilder);
        }
        //如果存在分页参数，则开启分页
        if(page > 0 && pageSize > 0){
            searchSourceBuilder.from((page-1)*pageSize);
            searchSourceBuilder.size(pageSize);
        } else{
            searchSourceBuilder.from(0);
            searchSourceBuilder.size(10);
        }
        searchSourceBuilder.sort("_id");
        searchRequest.source(searchSourceBuilder);
        JSONArray resultArray = new JSONArray();
        try {
            //提交搜索请求
            RestHighLevelClient client = getClient();
            SearchResponse searchResponse = client.search(searchRequest,RequestOptions.DEFAULT);
            SearchHits hits = searchResponse.getHits();
            SearchHit[] searchHits = hits.getHits();
            //遍历返回的数据
            for(SearchHit  hit: searchHits){
                String hitString = hit.getId();
                resultArray.add(hitString);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resultArray;
    }

    public JSONArray searchDocID(String index, JSONObject searchJson,int page, int pageSize){
        return searchDocID(index,searchJson,new JSONObject(),page,pageSize);
    }

    public JSONArray searchOrDoc(String index, JSONObject searchJson,int page, int pageSize){
        //构建搜索客户端
        SearchRequest searchRequest = new SearchRequest(index);
//        searchRequest.routing("routing"); // 设置 routing 参数
//        searchRequest.preference("_local");  // 配置搜索时偏爱使用本地分片，默认是使用随机分片
        //构建搜索请求
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //遍历查询条件
        BoolQueryBuilder qb = QueryBuilders.boolQuery();
        for(String keyString : searchJson.keySet()){
            Object searchValue = searchJson.get(keyString);
            QueryBuilder queryBuilder = null;
            //如果搜索对象为String类型，且模糊查询开启
            queryBuilder=QueryBuilders.matchQuery(keyString, searchValue);
            qb = qb.should(queryBuilder);
        }
        searchSourceBuilder.query(qb);
        //如果存在分页参数，则开启分页
        if(page > 0 && pageSize > 0){
            searchSourceBuilder.from((page-1)*pageSize);
            searchSourceBuilder.size(pageSize);
        } else{
            searchSourceBuilder.from(0);
            searchSourceBuilder.size(10);
        }
        searchSourceBuilder.sort("_id");
        searchRequest.source(searchSourceBuilder);
        JSONArray resultArray = new JSONArray();
        try {
            //提交搜索请求
            RestHighLevelClient client = getClient();
            RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
            builder.setHttpAsyncResponseConsumerFactory(new HttpAsyncResponseConsumerFactory.HeapBufferedResponseConsumerFactory(1048576000));
            SearchResponse searchResponse = client.search(searchRequest,builder.build());
            SearchHits hits = searchResponse.getHits();
            SearchHit[] searchHits = hits.getHits();
            //遍历返回的数据
            for(SearchHit  hit: searchHits){
                String hitString = hit.getSourceAsString();
                resultArray.add(JSON.parseObject(hitString));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resultArray;
    }

    public JSONArray searchDoc(String index, JSONObject searchJson,boolean mohu , int page, int pageSize, String[] excludeFields){
        return searchDoc(index,searchJson,new JSONObject(),mohu,page,pageSize,excludeFields);
    }

    public JSONArray searchDoc(String index, JSONObject searchJson,boolean mohu ){
        return searchDoc(index,searchJson,new JSONObject(),false, 0,0,null);
    }

    /**
     * 通过重载searchOneDoc方法，将分页参数默认为0
     */
    public JSONArray searchDoc(String index, JSONObject searchJson,JSONObject rangeJson, int page, int pageSize){
        return searchDoc(index,searchJson,rangeJson,false,page,pageSize,null);
    }

    /**
     * 通过重载searchOneDoc方法，将分页参数默认为0
     */
    public JSONArray searchDoc(String index, JSONObject searchJson, int page, int pageSize){
        return searchDoc(index,searchJson,new JSONObject(),false,page,pageSize,null);
    }


    /**
     * 获取表中全量数据
     * @param index
     * @return
     */
    public JSONArray searchDoc(String index){
        return searchDoc(index,new JSONObject(),1,getCount(index));
    }

    public JSONObject searchOneDoc(String index,JSONObject searchJson){
        JSONArray jsonArray = searchDoc(index,searchJson,1,1);
        if(jsonArray.size()>0){
            return jsonArray.getJSONObject(0);
        } else{
            return null;
        }
    }

    /**
     * 通过重载searchOneDoc方法，将分页参数默认为0
     */
    public JSONArray searchDoc(String index, JSONObject searchJson, boolean mohu,int page, int pageSize){
        return searchDoc(index,searchJson,mohu,page,pageSize,null);
    }

    /**
     * 通过重载searchOneDoc方法，将分页参数默认为0
     */
    public JSONArray searchDoc(String index, JSONObject searchJson, boolean mohu,String [] excludeFields){
        return searchDoc(index,searchJson,mohu,0,0,excludeFields);
    }

    /**
     * 查询当前数据是否存在库中且只有一个
     * @param index 索引
     * @param searchJson 查询条件
     * @return 如果存在返回true，不存在返回false
     */
    public boolean existsAndOne(String index, JSONObject searchJson){
        JSONArray resultArray = searchDoc(index,searchJson,false,null);
        if(resultArray.size()>1){
            try {
                throw new Exception("当前库中此数据超过两条");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return resultArray.size()==1;
    }

    /**
     * 根据ID获取到库中的指定记录
     * @param index 索引
     * @param id id
     * @return 返回此条记录的JSON格式的信息
     */
    public JSONObject getById(String index,String id)  {
        GetRequest getRequest = new GetRequest(index,id);
        GetResponse getResponse = null;
        try {
            RestHighLevelClient client = getClient();
            getResponse = client.get(getRequest, RequestOptions.DEFAULT);

            return JSON.parseObject(getResponse.getSourceAsString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 删除具体文档
     * @param index 索引
     * @param id 文档id
     * @return 操作结果
     */
    public boolean deleteOneDoc(String index,String id){
        DeleteRequest deleteRequest = new DeleteRequest(index,id);
        try {
            RestHighLevelClient client = getClient();
            client.delete(deleteRequest,RequestOptions.DEFAULT);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * 创建es中的文档信息
     * @param index 索引
     * @param idString 存放具体id的key名称
     * @param jsonObject 具体文档内容
     */
    public boolean insertOneDoc(String index, String idString, JSONObject jsonObject){
        //构建文档详细信息
        IndexRequest indexRequest = new IndexRequest();
        indexRequest.index(index);
        String id = jsonObject.getString(idString);
        indexRequest.id(id);
        //构建文档可选设置

        //进行索引插入操作
        indexRequest.source(jsonObject.toString(), XContentType.JSON);
        try {
            RestHighLevelClient client = getClient();
            IndexResponse indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);
            if (indexResponse.getResult() == DocWriteResponse.Result.UPDATED) {
                throw new Exception("新增文档失败，库中已存在此文档");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }
    public boolean insertOneDocError(String index, JSONObject jsonObject){
        //构建文档详细信息
        IndexRequest indexRequest = new IndexRequest();
        indexRequest.index(index);
        String id = SnowIdUtils.uniqueLong();
        indexRequest.id(id);
        //构建文档可选设置

        //进行索引插入操作
        indexRequest.source(jsonObject.toString(), XContentType.JSON);
        try {
            RestHighLevelClient client = getClient();
            IndexResponse indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);
            if (indexResponse.getResult() == DocWriteResponse.Result.UPDATED) {
                throw new Exception("新增文档失败，库中已存在此文档");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * 更新elasticsearch中具体字段的信息
     * @param index 索引
     * @param idString id
     * @param jsonObject 待更新的信息
     * @return true
     */
    public boolean updateOneDoc(String index, String idString, JSONObject jsonObject){
        String id = jsonObject.getString(idString);
        UpdateRequest updateRequest = new UpdateRequest(index,id);
        updateRequest.doc(jsonObject, XContentType.JSON);
        try {
            RestHighLevelClient client = getClient();
            client.update(updateRequest,RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public void insertDocBulk(String index,JSONArray jsonArray,String id,int batch){
        JSONArray resultArray = new JSONArray();
        for(int i = 0;i<jsonArray.size();i++){
            JSONObject oneJson = jsonArray.getJSONObject(i);
            resultArray.add(oneJson);
            if(resultArray.size()>=batch){
                insertDocBulk(index,resultArray,id);
                resultArray.clear();
                System.out.println(index+"插入数量："+i);
            }
        }
        if(resultArray.size()!=0){
            insertDocBulk(index,resultArray,id);
        }
    }

    public void updateDocBulk(String index,JSONArray jsonArray,String id,int batch){
        JSONArray resultArray = new JSONArray();
        for(int i = 0;i<jsonArray.size();i++){
            JSONObject oneJson = jsonArray.getJSONObject(i);
            resultArray.add(oneJson);
            if(resultArray.size()>=batch){
                updateDocBulk(index,resultArray,id);
                resultArray.clear();
                System.out.println(index+"更新数量："+i);
            }
        }
        if(resultArray.size()!=0){
            updateDocBulk(index,resultArray,id);
        }
    }

    public void deleteDocBulk(String index,JSONArray jsonArray,int batch){
        JSONArray resultArray = new JSONArray();
        for(int i = 0;i<jsonArray.size();i++){
            String id = jsonArray.getString(i);
            resultArray.add(id);
            if(resultArray.size()>=batch){
                deleteDocBulk(index,resultArray);
                resultArray.clear();
                System.out.println(index+"删除数量："+i);
            }
        }
        if(resultArray.size()!=0){
            deleteDocBulk(index,resultArray);
        }
    }

    public boolean deleteDocBulk(String index,JSONArray jsonArray){
        BulkRequest bulkRequest = new BulkRequest();
        for(int i=0;i<jsonArray.size();i++){
            String uid = jsonArray.getString(i);
            bulkRequest.add(new DeleteRequest().index(index).id(uid));
        }
        try {
            client.bulk(bulkRequest,RequestOptions.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public boolean updateDocBulk(String index,JSONArray jsonArray,String id){
        BulkRequest bulkRequest = new BulkRequest();
        for(int i=0;i<jsonArray.size();i++){
            JSONObject jsonObject = (JSONObject) jsonArray.get(i);
            String uid = jsonObject.getString(id);
            bulkRequest.add(new UpdateRequest().index(index).id(uid).doc(jsonObject.toString(), XContentType.JSON));
        }
        try {
            client.bulk(bulkRequest,RequestOptions.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * 异步方式创建es中的索引
     * @param index 索引名称
     */
    public boolean deleteIndex(String index){
        //创建索引请求
        DeleteIndexRequest indexRequest = new DeleteIndexRequest(index);
        RestHighLevelClient client = getClient();
        try {
            client.indices().delete(indexRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * 向es中批量插入数据
     * @param index 索引信息
     * @param jsonArray 数据列表
     * @return 插入状态
     */
    public boolean insertDocBulk(String index, JSONArray jsonArray,String idString){
        BulkRequest bulkRequest = new BulkRequest();
        for(int i=0;i<jsonArray.size();i++){
            JSONObject jsonObject = (JSONObject) jsonArray.get(i);
            String uid = jsonObject.getString(idString);
            bulkRequest.add(new IndexRequest().index(index).id(uid).source(jsonObject.toString(), XContentType.JSON));
        }
        try {
            client.bulk(bulkRequest,RequestOptions.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public void close(){
        try {
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 此方法筛选数据中是否包含指定字段
     * @param indexString 索引名称
     * @param fieldArray 返回数据中必须存在的字段名称，如果不存在，则数据不返回
     * @param withOutArray 返回数据中必须不存在的字段名称，如果字段存在，则数据不返回
     * @param page 当前页数
     * @param pageSize 当前页面数据量
     * @return 经过筛选后的数据
     */
    public JSONArray filterByField(String indexString,JSONArray fieldArray,JSONArray withOutArray,int page,int pageSize){
        JSONArray resultArray = new JSONArray();
        SearchRequest searchRequest = new SearchRequest(indexString);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        for(int i =0;i<fieldArray.size();i++){
            String fileName = fieldArray.getString(i);
            QueryBuilder queryBuilder = QueryBuilders.existsQuery(fileName);
            boolQueryBuilder.must(queryBuilder);
        }
        for(int i =0;i<withOutArray.size();i++){
            String fileName = withOutArray.getString(i);
            QueryBuilder queryBuilder = QueryBuilders.existsQuery(fileName);
            boolQueryBuilder.mustNot(queryBuilder);
        }
        searchSourceBuilder.query(boolQueryBuilder);
        searchRequest.source(searchSourceBuilder);

        if(page > 0 && pageSize > 0){
            searchSourceBuilder.from((page-1)*pageSize);
            searchSourceBuilder.size(pageSize);
        } else{
            searchSourceBuilder.from(0);
            searchSourceBuilder.size(10);
        }
        try {
            //提交搜索请求
            RestHighLevelClient client = getClient();
            SearchResponse searchResponse = client.search(searchRequest,RequestOptions.DEFAULT);
            SearchHits hits = searchResponse.getHits();
            SearchHit[] searchHits = hits.getHits();
            //遍历返回的数据
            for(SearchHit  hit: searchHits){
                String hitString = hit.getSourceAsString();
                resultArray.add(JSON.parseObject(hitString));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resultArray;
    }

    public void deleteBySearchJson(String indexName,JSONObject searchJson, JSONObject rangeJson){
        int count = getCount(indexName,searchJson,rangeJson);
        JSONArray jsonArray = searchDocID(indexName,searchJson,rangeJson,1,count);
        deleteDocBulk(indexName,jsonArray);
    }

    public void deleteBySearchJson(String indexName,JSONObject searchJson){
        deleteBySearchJson(indexName,searchJson,new JSONObject());
    }


        /**
         * 获取所有索引的名称和第一个别名
         * @return json格式结果
         */
    public JSONObject getAllIndexName(){
        GetIndexRequest getIndexRequest = new GetIndexRequest("*");
        GetIndexResponse getIndexResponse = null;
        try {
            getIndexResponse = client.indices().get(getIndexRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Map<String, List<AliasMetaData>> indices = getIndexResponse.getAliases();
        JSONObject resultJson = new JSONObject();
        for(String indexString:indices.keySet()){
            List<AliasMetaData> oneAlias = indices.get(indexString);
            if(oneAlias.size()!=0){
                resultJson.put(indexString,oneAlias.get(0).getAlias());
            } else{
                resultJson.put(indexString,"");
            }
        }
        return resultJson;
    }

    public void move(String oldIndexName,String indexName,String idKey){
        int count = (int) getCount(oldIndexName);

        System.out.println(count);
        try{
            deleteIndex(indexName);
            createIndex(indexName);
        } catch (Exception ignored){
        }
        int num = count/5000;
        if(count%5000!=0){
            num = num+1;
        }
        for(int i =0;i<=num;i++){
            JSONArray jsonArray = searchDoc(oldIndexName,new JSONObject(),i,5000);
            insertDocBulk(indexName,jsonArray,idKey);
        }
    }

    public static void main(String[] args){
    }
}
