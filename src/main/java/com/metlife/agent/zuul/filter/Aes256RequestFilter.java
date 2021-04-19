package com.metlife.agent.zuul.filter;

import com.google.common.collect.Lists;
import com.metlife.agent.ApplicationContextHolder;
import com.metlife.agent.commons.exception.BusinessException;
import com.metlife.agent.commons.exception.ExceptionMsg;
import com.metlife.agent.commons.helper.*;
import com.metlife.agent.commons.msg.ResponseData;
import com.metlife.agent.commons.security.SecurityConstants;
import com.metlife.agent.commons.security.SecurityProperties;
import com.metlife.agent.commons.security.feign.SecurityClient;
import com.metlife.agent.commons.security.model.SecurityModel;
import com.metlife.agent.commons.vo.CacheDictVo;
import com.google.common.base.Strings;
import com.metlife.agent.commons.vo.MeterInterfaceVo;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.netflix.zuul.http.ServletInputStreamWrapper;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.*;
import java.util.stream.Collectors;

/**
 * post get加密
 * @Author HanKeQi
 * @Date 2021/1/5 下午3:10
 * @Version 1.0
 **/
@Component
@Slf4j
public class Aes256RequestFilter extends ZuulFilter implements ApplicationRunner{

    private static List<String> listPath = Lists.newArrayList();

    @Override
    public String filterType() {
        return FilterConstants.PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        //执行顺序
        return FilterConstants.PRE_DECORATION_FILTER_ORDER;
    }

    @Override
    public boolean shouldFilter() {
        return Boolean.TRUE.booleanValue();
    }

    @Autowired
    private SecurityClient securityClient;

    @Override
    public Object run() throws ZuulException {
        //获取Conext对象应用上下文, 从中获取req,res对象
        RequestContext cxt = RequestContext.getCurrentContext();
        HttpServletRequest request = cxt.getRequest();

        String requestURI = request.getRequestURI();

        //spring boot 工具类
        AntPathMatcher antPathMatcher = new AntPathMatcher();
//        //不加密的URL
        List<CacheDictVo> cacheDictVos = Optional.ofNullable(DictHelper.findByCategoryCode("sec.attachment.path")).orElse(Lists.newArrayList());
        List<CacheDictVo> collectCacheDictVo = cacheDictVos.stream().filter(cacheDictVo -> cacheDictVo != null && antPathMatcher.match(cacheDictVo.getDictValue(), requestURI)).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(collectCacheDictVo)){
            return null;
        }


        String collect = listPath.stream().filter(strURL -> antPathMatcher.match(strURL, requestURI)).collect(Collectors.joining());

       //验证码 如果此处拦截 则需要放开开放URL
       if (Strings.isNullOrEmpty(collect)){
           SecurityModel currentUser = SecurityHelper.getCurrentUser();
           boolean vCode = currentUser.isVCode();
           boolean instruct = currentUser.isInstruct();
           if (vCode){
               setFailZuulCtx(cxt, ResponseData.newInstanceOfExceptionMsg(ExceptionMsg.SMS_DEVICE_NULL));
               return null;
           }
           //用户须知未读
           if (instruct){
               setFailZuulCtx(cxt, ResponseData.newInstanceOfExceptionMsg(ExceptionMsg.USER_INFORM));
               return null;
           }
       }

        String aesTimestamp = "";
        try {
            //aes256.开关是否开启aes256.enabled
            String client = ServletHelper.getHeader(SecurityConstants.CLIENT);
            if (Strings.isNullOrEmpty(client)){
                client = "h5";
//                setFailZuulCtx(cxt, ResponseData.newInstanceError(ExceptionMsg.DEFAULT_ERROR.getCode(), "终端类型不能为空"));
//                return null;
            }
            //统一uuId
            String requestNo = ServletHelper.getHeader(SecurityConstants.REQUEST_NO);
            if (Strings.isNullOrEmpty(requestNo)){
                requestNo = UUID.randomUUID().toString();
            }

            client = client.toLowerCase();
            CacheDictVo cacheDictVo = DictHelper.findByCode(String.format("aes256.%s.enabled", client));
            if (cacheDictVo == null || Strings.isNullOrEmpty(cacheDictVo.getDictValue()) || !Boolean.valueOf(cacheDictVo.getDictValue()).booleanValue()){
                return null;
            }

            String sign = request.getParameter(SecurityConstants.PARAM_SIGN);
            String value = request.getParameter(SecurityConstants.PARAM_VALUE);
            if (Strings.isNullOrEmpty(value)){
                log.error("sign = {}, value = {}", sign, value);
                setFailZuulCtx(cxt, ResponseData.newInstanceOfExceptionMsg(ExceptionMsg.NULL_VALUE_EXCEPTION));
                return null;
            }
            //支持get post json 请求
            String requestData = AES256Helper.decoder(value);
            log.debug("requestData = {}", requestData);
            Map<String, String> map = UrlHelper.getUrlParamsInOrder(requestData);
            StringBuilder str = new StringBuilder();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                if ("timestamp".equals(entry.getKey())) {
                    // timestamp两次append
                    String aesValue = entry.getValue();
                    str.append(aesValue);
                    aesTimestamp = aesValue;
                }
                str.append(entry.getValue());
            }
            //验签出错
            String localMd5Str = Md5Encoder.encodeBit32WithNoSalt(str.toString().getBytes());
            if (Strings.isNullOrEmpty(localMd5Str) || !localMd5Str.equals(sign)) {
                //签名失败
                setFailZuulCtx(cxt, ResponseData.newInstanceError(ExceptionMsg.DEFAULT_ERROR.getCode(), "签名失败"));
                return null;
            }
            //考虑复杂类型
            String jsonStr = JsonHelper.mapToJSONString(map);
            if (!Strings.isNullOrEmpty(jsonStr)) {
                byte[] bytes = jsonStr.getBytes();
                cxt.setRequest(new HttpServletRequestWrapper(request) {
                    @Override
                    public ServletInputStream getInputStream() {
                        return new ServletInputStreamWrapper(bytes);
                    }
                    @Override
                    public int getContentLength() {
                        return bytes.length;
                    }
                    @Override
                    public long getContentLengthLong() {
                        return bytes.length;
                    }
                });
            }
            //进行转码操作
            cxt.addZuulRequestHeader("Content-Type" , MediaType.APPLICATION_JSON_UTF8_VALUE);
            cxt.addZuulRequestHeader(SecurityConstants.REQUEST_NO , requestNo);
            cxt.addZuulRequestHeader(SecurityConstants.CLIENT , client);
            cxt.addZuulRequestHeader(SecurityConstants.HEADER_UTOKEN, ServletHelper.getHeader(SecurityConstants.HEADER_UTOKEN));
            cxt.addZuulRequestHeader(SecurityConstants.AES_TIMESTAMP, aesTimestamp);
            String jMeterFlag = ServletHelper.getHeader(SecurityConstants.J_METER_FLAG);
            log.info("start client = {}, jMeterFlag = {}", client, jMeterFlag);
            if (!Strings.isNullOrEmpty(jMeterFlag)){
                log.info("end client = {}, jMeterFlag = {}", client, jMeterFlag);
                cxt.addZuulRequestHeader(SecurityConstants.J_METER_FLAG, jMeterFlag);
                MeterInterfaceVo meterInterface = new MeterInterfaceVo();
                meterInterface.setAppTimeStamp(aesTimestamp);
                meterInterface.setUrl(requestURI);
                meterInterface.setRequestTimeStamp(String.valueOf(System.currentTimeMillis()));
                meterInterface.setParams(jsonStr);
                ResponseData<MeterInterfaceVo> meterInterfaceResponseData = securityClient.insertOrUpdate(meterInterface);
                if (!ExceptionMsg.SUCCESS.getCode().equals(meterInterfaceResponseData.getCode())){
                    log.error("api-c  aesTimestamp  = {}, currentTimestamp = {}", aesTimestamp, System.currentTimeMillis());
                }
            }

        } catch (Exception e) {
            //自定义异常
            if (e instanceof BusinessException){
                BusinessException businessException = (BusinessException) e;
                setFailZuulCtx(cxt, ResponseData.newInstanceError(businessException.getCode(), businessException.getMessage()));
                return null;
            }
            log.error("message = {}", e.getMessage());
            //否则系统异常
            setFailZuulCtx(cxt, ResponseData.newInstanceOfExceptionMsg(ExceptionMsg.DEFAULT_ERROR));
        }
        return null;
    }

    /**
     * zuul错误返回设置
     * @param ctx
     * @param responseData
     */
    private static void setFailZuulCtx(RequestContext ctx, ResponseData responseData) {
        ctx.setResponseStatusCode(403);
        ctx.setResponseBody(JsonHelper.toJSONString(responseData));
        ctx.getResponse().setContentType("application/json;charset=UTF-8");
        ctx.setSendZuulResponse(false);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        SecurityProperties properties = ApplicationContextHolder.getBean(SecurityProperties.class);
        if (properties == null){
            return;
        }
        List<String> filterChainDefinitions = properties.getFilterChainDefinitions();
        List<String> filterChainCustomDefinitions = Optional.of(properties.getFilterChainCustomDefinitions()).orElse(Lists.newArrayList());
        filterChainDefinitions.forEach(str->{
            String[] split = str.split("=");
            String key = split[0];
            String value = split[1];
            if ("anon".equals(value) || "user".equals(value)){
                filterChainCustomDefinitions.add(key);
            }
        });
        listPath = Collections.unmodifiableList(filterChainCustomDefinitions);
    }

    /**
     * TODO 测试加密
     */
//    public static void main(String[] args) throws Exception{
//        String strs = "id=1234&timestamp=11111"; //加密参数
//        //TODO 加密只机密value
//        Map<String, String> map = UrlHelper.getUrlParamsInOrder(strs);
//        StringBuilder str = new StringBuilder();
//        for (Map.Entry<String, String> entry : map.entrySet()) {
//            if ("timestamp".equals(entry.getKey())) {
//                //TODO  timestamp两次append
//                str.append(entry.getValue());
//            }
//            str.append(entry.getValue());
//        }
//        String sign = Md5Encoder.encodeBit32WithNoSalt(str.toString().getBytes());
//        System.out.println(sign);
//        //TODO aes256 加密所有参数key,value
//        String value = AES256Helper.encode(strs);
//        System.out.println(value);
//    }


}
