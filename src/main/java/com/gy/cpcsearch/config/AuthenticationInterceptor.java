package com.gy.cpcsearch.config;

import com.alibaba.fastjson.JSONObject;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

/**
 * 拦截器
 * @author qiaoyn
 * @date 2019/06/14
 */
public class AuthenticationInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object object) throws Exception {
        String menuid =  httpServletRequest.getQueryString();
        System.out.println(menuid);
        try {
            if(menuid.contains("menuid=150371841087979520")){
                return true;
            }
        } catch (Exception e){
            sendError(httpServletResponse,"forbidden");
            return false;
        }
        sendError(httpServletResponse,"forbidden");
        return false;
    }

    private void sendError(HttpServletResponse httpServletResponse, String errorInfo) throws IOException {
        PrintWriter out = httpServletResponse.getWriter();
        JSONObject res = new JSONObject();
        res.put("code", "403");
        res.put("Result", errorInfo);
        out.append(res.toString());
//        System.out.println(errorInfo);
    }
    public static JSONObject getRequestJson(HttpServletRequest httpServletRequest) throws Exception {
        ServletInputStream fileInputStream = httpServletRequest.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(fileInputStream));
        String str = "";
        StringBuilder wholeStr = new StringBuilder();
        while((str = reader.readLine()) != null){//一行一行的读取body体里面的内容；
            wholeStr.append(str);
        }
        String re = wholeStr.toString();
        return JSONObject.parseObject(re);
    }
}

