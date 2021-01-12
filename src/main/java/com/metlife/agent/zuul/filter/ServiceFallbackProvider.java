package com.metlife.agent.zuul.filter;

import com.metlife.agent.commons.exception.ExceptionMsg;
import com.metlife.agent.commons.helper.JsonHelper;
import com.metlife.agent.commons.msg.ResponseData;
import com.netflix.hystrix.exception.HystrixTimeoutException;
import org.springframework.cloud.netflix.zuul.filters.route.FallbackProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * 服务熔断降级
 * @Author HanKeQi
 * @Date 2021/1/7 下午2:10
 * @Version 1.0
 **/
@Component
public class ServiceFallbackProvider implements FallbackProvider {
    @Override
    public String getRoute() {
        return "*";
    }

    @Override
    public ClientHttpResponse fallbackResponse(String route, Throwable cause) {
        if (cause instanceof HystrixTimeoutException) {
            return response(504);
        } else {
            return this.fallbackResponse();
        }
    }

    public ClientHttpResponse fallbackResponse() {
        return this.response(500);
    }


    private ClientHttpResponse response(final int status) {
        return new ClientHttpResponse() {
            @Override
            public HttpStatus getStatusCode() {
                return HttpStatus.SERVICE_UNAVAILABLE;
            }

            @Override
            public int getRawStatusCode() {
                return HttpStatus.SERVICE_UNAVAILABLE.value();
            }

            @Override
            public String getStatusText() {
                return HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase();
            }

            @Override
            public void close() {
            }

            @Override
            public InputStream getBody() {
                String str;
                if (status == 504) {
                    str= JsonHelper.toJSONString(ResponseData.newInstanceError(ExceptionMsg.DEFAULT_ERROR.getCode(), "当前服务未注册，请稍后再重试"));
                }else{
                    str= JsonHelper.toJSONString(ResponseData.newInstanceError(ExceptionMsg.DEFAULT_ERROR.getCode(),"当前服务未启动，请启动再重试"));
                }
                return new ByteArrayInputStream(str.getBytes());
            }

            @Override
            public HttpHeaders getHeaders() {
                // headers设定
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                return headers;
            }
        };
    }
}
