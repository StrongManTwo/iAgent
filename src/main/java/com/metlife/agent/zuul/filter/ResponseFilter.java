package com.metlife.agent.zuul.filter;

import com.google.common.base.Strings;
import com.metlife.agent.commons.exception.ExceptionMsg;
import com.metlife.agent.commons.helper.ServletHelper;
import com.metlife.agent.commons.msg.ResponseData;
import com.metlife.agent.commons.security.SecurityConstants;
import com.metlife.agent.commons.security.feign.SecurityClient;
import com.metlife.agent.commons.vo.MeterInterfaceVo;
import com.metlife.agent.security.client.ISecurityProvideClient;
import com.metlife.agent.security.po.MeterInterface;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.util.Map;

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
        return FilterConstants.PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return FilterConstants.SEND_RESPONSE_FILTER_ORDER-2;
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
        ctx.getResponseDataStream();
        Map<String, String> zuulRequestHeaders = ctx.getZuulRequestHeaders();
        String jMeterFlag = zuulRequestHeaders.get(SecurityConstants.J_METER_FLAG.toLowerCase());
        String aesTimestamp = zuulRequestHeaders.get(SecurityConstants.AES_TIMESTAMP.toLowerCase());
        if (!Strings.isNullOrEmpty(jMeterFlag)){
            try {
                ResponseData<MeterInterfaceVo> meterInterfaceResponseData = securityClient.getAppTimeStamp(aesTimestamp);
                if (!ExceptionMsg.SUCCESS.getCode().equals(meterInterfaceResponseData.getCode())){
                    log.error("not not aes_timestamp = {} msg = {}", aesTimestamp,
                            meterInterfaceResponseData.getMsg());
                    return null;

                }
                MeterInterfaceVo meterInterface = meterInterfaceResponseData.getData();
                meterInterface.setResponseTimeStamp(String.valueOf(System.currentTimeMillis()));
                ResponseData<MeterInterfaceVo> meterInterfaceUpdateResponseData = securityClient.insertOrUpdate(meterInterface);
                if (!ExceptionMsg.SUCCESS.getCode().equals(meterInterfaceUpdateResponseData.getCode())){
                    log.error("update meterInterfaceUpdateResponseData msg = {}", meterInterfaceUpdateResponseData.getMsg());
                    return null;

                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return null;
    }
}
