package com.fulan.agent.zuul.security;

import com.fulan.agent.commons.msg.ResponseData;
import com.fulan.agent.commons.security.feign.SecurityClient;
import com.fulan.agent.commons.security.model.SecurityModel;
import com.fulan.agent.security.client.ISecurityProvideClient;
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
        return null;
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
}
