package com.metlife.agent.zuul.filter;

import com.metlife.agent.commons.exception.BusinessException;
import com.metlife.agent.commons.exception.ExceptionMsg;
import com.metlife.agent.commons.msg.ResponseData;
import com.metlife.agent.commons.security.SecurityConstants;
import com.metlife.agent.commons.vo.CacheDictVo;
import com.google.common.base.Strings;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.netflix.zuul.http.ServletInputStreamWrapper;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.Map;
import java.util.UUID;

/**
 * post get加密
 * @Author HanKeQi
 * @Date 2021/1/5 下午3:10
 * @Version 1.0
 **/
@Component
@Slf4j
public class Aes256RequestFilter extends ZuulFilter {

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

    @Override
    public Object run() throws ZuulException {
        //获取Conext对象应用上下文, 从中获取req,res对象
        RequestContext cxt = RequestContext.getCurrentContext();
        HttpServletRequest request = cxt.getRequest();
        String method = request.getMethod();
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
            if (cacheDictVo == null || Strings.isNullOrEmpty(cacheDictVo.getDictValue())){
                return null;
            }

            String sign = request.getParameter(SecurityConstants.PARAM_SIGN);
            String value = request.getParameter(SecurityConstants.PARAM_VALUE);
            if (Strings.isNullOrEmpty(value)){
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
                    str.append(entry.getValue());
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
