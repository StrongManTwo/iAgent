package com.metlife.agent.zuul.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @Author HanKeQi
 * @Date 2021/1/5 下午4:37
 * @Version 1.0
 **/
@Component
@Slf4j
public class CorsFilter implements Filter {


    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        String url = request.getHeader("Origin");
        response.setHeader("Access-Control-Allow-Origin", url);
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Max-Age", "3600");
        response.setHeader("Access-Control-Allow-Headers", "x-requested-with,Accept,Origin,Content-Type,LastModified,Cookie,UTOKEN,CLIENT,REQUESTNO,DEVICEID");
        log.debug("*********************************过滤器被使用**************************");
        String method = request.getMethod();
        log.debug("---------------"+method+"----"+url);
        if("OPTIONS".equals(method)){
            response.setStatus(200, "success");
            response.flushBuffer();
            return;
        }
        chain.doFilter(servletRequest, servletResponse);

    }
}
