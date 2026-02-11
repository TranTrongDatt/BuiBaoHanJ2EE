package fit.hutech.BuiBaoHan.services;

import java.util.Map;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OAuthService extends DefaultOAuth2UserService {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        
        // Log thông tin user để debug
        Map<String, Object> attributes = oAuth2User.getAttributes();
        log.info("OAuth2 User Attributes: {}", attributes);
        
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        log.info("OAuth2 login - Email: {}, Name: {}", email, name);
        
        return oAuth2User;
    }
}
