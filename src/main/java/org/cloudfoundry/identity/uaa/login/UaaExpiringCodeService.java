package org.cloudfoundry.identity.uaa.login;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.cloudfoundry.identity.uaa.codestore.ExpiringCode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class UaaExpiringCodeService implements ExpiringCodeService {
    
    @Resource(name="authorizationTemplate")
    private RestTemplate uaaTemplate;
    
    private ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${uaa.url:http://localhost:8080/uaa}")
    private String uaaBaseUrl;
    
    
    @Override
    public String generateCode(Object data, int expiryTime, TimeUnit timeUnit) throws IOException {
        Timestamp exipry = new Timestamp(System.currentTimeMillis()+TimeUnit.MILLISECONDS.convert(expiryTime, timeUnit));
        String dataJsonString = objectMapper.writeValueAsString(data);
        ExpiringCode expiringCode = new ExpiringCode(null, exipry, dataJsonString);
        expiringCode = uaaTemplate.postForObject(uaaBaseUrl + "/Codes", expiringCode, ExpiringCode.class);
        return expiringCode.getCode();
    }

    @Override
    public <T> T verifyCode(Class<T> clazz, String code) throws IOException, CodeNotFoundException {
        try {
            ExpiringCode expiringCode = uaaTemplate.getForObject(uaaBaseUrl + "/Codes/"+ code, ExpiringCode.class);
            return objectMapper.readValue(expiringCode.getData(), clazz);
        } catch (Exception e) {
            throw new CodeNotFoundException();
        }
    }
    
    @Override
    public Map<String,String> verifyCode(String code) throws IOException, CodeNotFoundException {
        try {
            ExpiringCode expiringCode = uaaTemplate.getForObject(uaaBaseUrl + "/Codes/"+ code, ExpiringCode.class);
            return objectMapper.readValue(expiringCode.getData(), new TypeReference<Map<String,String>>() {});
        } catch (Exception e) {
            throw new CodeNotFoundException();
        }
    }

}
