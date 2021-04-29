package com.metlife.agent.zuul.security;

import com.metlife.agent.commons.msg.ResponseData;
import com.metlife.agent.commons.security.feign.SecurityClient;
import com.metlife.agent.commons.security.model.SecurityModel;
import com.metlife.agent.commons.vo.CacheDictVo;
import com.metlife.agent.commons.vo.MeterInterfaceVo;
import com.metlife.agent.commons.vo.UserLogVo;
import com.metlife.agent.security.client.ISecurityProvideClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @Author HanKeQi
 * @Date 2020/12/26 下午3:51
 * @Version 1.0
 **/
@Component
public class SecurityClientImpl implements SecurityClient {

    @Autowired
    private ISecurityProvideClient securityProvideClient;

    @Override
    public String getAppType() {
        return securityProvideClient.getAppType();
    }

    @Override
    public ResponseData<SecurityModel> findUserByUsernameAndOrgType(String username, String orgType) {
        ResponseData<SecurityModel> responseData = securityProvideClient.findUserByUsernameAndOrgType(username, orgType);
        return responseData;
    }

    @Override
    public Set<String> findResourceByUsername(String username) {
        return securityProvideClient.findResourceByUsername(username);
    }

    @Override
    public Set<String> findRoleByUserName(String username) {
        return securityProvideClient.findRoleByUserName(username);
    }

    @Override
    public void lockUser(String userId, String username, String name, Date lockTime) {

    }

    @Override
    public void unLockUser(String userId, String username, String name) {

    }

    @Override
    public String getDictValue(String code) {
        return securityProvideClient.getDictValue(code);
    }

    @Override
    public List<String> listDictCode(String parentCode) {
        return securityProvideClient.listDictCode(parentCode);
    }

    @Override
    public Boolean getMiddleToken(String username, String password) {
        return securityProvideClient.getMiddleToken(username, password);
    }

    @Override
    public ResponseData<MeterInterfaceVo> insertOrUpdate(MeterInterfaceVo meterInterfaceVo) {
        return securityProvideClient.insertOrUpdate(meterInterfaceVo);
    }

    @Override
    public ResponseData<MeterInterfaceVo> getAppTimeStamp(String appTimeStamp) {
        return securityProvideClient.getAppTimeStamp(appTimeStamp);
    }

    @Override
    public void log(UserLogVo vo) {
        securityProvideClient.log(vo);
    }

//    @Override
//    public CacheDictVo getCacheDictVo(String code) {
//        return securityProvideClient.getCacheDictVo(code);
//    }
//
//    @Override
//    public List<CacheDictVo> listCacheDictVo(String parentCode) {
//        return securityProvideClient.listCacheDictVo(parentCode);
//    }
}
