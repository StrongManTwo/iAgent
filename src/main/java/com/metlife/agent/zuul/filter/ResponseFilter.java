package com.metlife.agent.zuul.filter;

import com.google.common.base.Strings;
import com.metlife.agent.commons.exception.ExceptionMsg;

import com.metlife.agent.commons.msg.ResponseData;
import com.metlife.agent.commons.security.SecurityConstants;
import com.metlife.agent.commons.security.feign.SecurityClient;
import com.metlife.agent.commons.vo.MeterInterfaceVo;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @Author HanKeQi
 * @Date 2021/4/9 下午5:36
 * @Version 1.0
 **/
@Component
@Slf4j
public class ResponseFilter extends ZuulFilter {
    @Override
    public String filterType() {
        return FilterConstants.POST_TYPE;
    }

    @Override
    public int filterOrder() {
        return FilterConstants.SEND_RESPONSE_FILTER_ORDER-3;
    }

    @Override
    public boolean shouldFilter() {
        return Boolean.TRUE.booleanValue();
    }

    @Autowired
    private SecurityClient securityClient;

    @Override
    public Object run() throws ZuulException {
        RequestContext ctx = RequestContext.getCurrentContext();

        Map<String, String> zuulRequestHeaders = ctx.getZuulRequestHeaders();
        String jMeterFlag = zuulRequestHeaders.get(SecurityConstants.J_METER_FLAG.toLowerCase());
        String aesTimestamp = zuulRequestHeaders.get(SecurityConstants.AES_TIMESTAMP.toLowerCase());
        log.debug("start jMeterFlag = {}", jMeterFlag);
        if (!Strings.isNullOrEmpty(jMeterFlag)){
            InputStream responseDataStream = ctx.getResponseDataStream();
            log.debug("end jMeterFlag = {}", jMeterFlag);
            try {
                String body = StreamUtils.copyToString(responseDataStream, StandardCharsets.UTF_8);
                ctx.setResponseBody(body);

                CompletableFuture.runAsync(()-> {
                    try {
                        ResponseData<MeterInterfaceVo> meterInterfaceResponseData = securityClient.getAppTimeStamp(aesTimestamp);
                        if (!ExceptionMsg.SUCCESS.getCode().equals(meterInterfaceResponseData.getCode())){
                            log.error("not not aes_timestamp = {} msg = {}", aesTimestamp,
                                    meterInterfaceResponseData.getMsg());
                            return;

                        }
                        MeterInterfaceVo meterInterface = meterInterfaceResponseData.getData();
                        meterInterface.setResponseTimeStamp(String.valueOf(System.currentTimeMillis()));
                        ResponseData<MeterInterfaceVo> meterInterfaceUpdateResponseData = securityClient.insertOrUpdate(meterInterface);
                        if (!ExceptionMsg.SUCCESS.getCode().equals(meterInterfaceUpdateResponseData.getCode())){
                            log.error("update meterInterfaceUpdateResponseData msg = {}", meterInterfaceUpdateResponseData.getMsg());
                            return;

                        }
                    }catch (Exception e){

                    }
                });

            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return null;
    }
}
