package de.dohack.githubbot.backend;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties("secret.config")
@Configuration("secretConfig")
public class Bearer {

    private String bearer;

    public String getBearer() {
        return bearer;
    }

    public void setBearer(String bearer) {
        this.bearer = bearer;
    }
}
