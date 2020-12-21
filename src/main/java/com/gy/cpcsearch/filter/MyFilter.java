package com.gy.cpcsearch.filter;

import com.gy.cpcsearch.config.BodyReaderHttpServletRequestWrapper;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 *  第一种方式:
 *  springboot中做过滤器 , 直接继承filter , 添加component注解即可
 *  第二种方式 -> 如 MyFilter2 与 config包下 . 将第三方过滤器添加入过滤器链中
 *
 *  过滤器的缺点:
 *      1. 只能获取request 与 response对象 . 并不能获取要访问的类与方法.
 *      2. 因为继承的filter是servlet的 , 并不是spring的
 *      3. 可以使用spring提供的拦截器 intercept
 */
//@Component
public class MyFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        System.out.println("*********************************自定义过滤器被使用**************************");
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        ServletRequest requestWrapper = new BodyReaderHttpServletRequestWrapper(httpServletRequest);
        filterChain.doFilter(requestWrapper, servletResponse);
    }

    @Override
    public void destroy() {
        System.out.println("毁灭中...");
    }
}