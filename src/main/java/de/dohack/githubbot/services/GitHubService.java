package de.dohack.githubbot.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.dohack.githubbot.entities.InvitationState;
import de.dohack.githubbot.entities.RepoNameInUseException;
import de.dohack.githubbot.entities.RepoNotTemplateException;
import de.dohack.githubbot.entities.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.security.Principal;
import java.util.Map;

@Service
public class GitHubService {

    private Logger logger = LoggerFactory.getLogger(GitHubService.class);

    @Value("${secret.config.bearer}")
    private String bearer;
    @Value("${secret.config.organization-name}")
    private String organizationName;
    @Value("${secret.config.template-repository}")
    private String templateRepo;
    @Value("${secret.config.template-owner}")
    private String templateOwner;

    private HttpHeaders authHeaders;
    private HttpHeaders authBaptisteHeaders;
    private HttpHeaders authDazzlerHeaders;
    private HttpHeaders authHellcatHeaders;

    @PostConstruct
    public void init() {
        this.authHeaders = new HttpHeaders() {{
            set("Authorization", "Bearer " + bearer);
        }};
        this.authBaptisteHeaders = new HttpHeaders() {{
            set("Authorization", "Bearer " + bearer);
            set("Accept", "application/vnd.github.baptiste-preview+json");
        }};
        this.authDazzlerHeaders = new HttpHeaders() {{
            set("Authorization", "Bearer " + bearer);
            set("Accept", "application/vnd.github.dazzler-preview+json");
        }};
        this.authHellcatHeaders = new HttpHeaders() {{
            set("Authorization", "Bearer " + bearer);
            set("Accept", "application/vnd.github.hellcat-preview+json");
        }};
    }

    public String setUsername(Principal principal) {
        String username = "anonymous";
        if (principal != null) {
            OAuth2AuthenticationToken token = ((OAuth2AuthenticationToken) principal);
            username = token.getPrincipal().getAttributes().get("login").toString();
        }
        return username;
    }

    public void createRepo(Repository repository) throws RepoNameInUseException, RepoNotTemplateException, JSONException {
        String get = "https://api.github.com/repos/" + organizationName + "/" + templateRepo;
        ResponseEntity<String> getResponse = new RestTemplate().exchange(get, HttpMethod.GET, new HttpEntity(authBaptisteHeaders), String.class);

        if (!"true".equals(getFieldFromResponseString(getResponse, "is_template"))) {
            throw new RepoNotTemplateException(templateRepo + " is not a template repository");
        }

        String findRepoUrl = "https://api.github.com/repos/" + organizationName + "/" + repository.getRepoName();
        try {
            ResponseEntity<String> findRepoResponse = new RestTemplate().exchange(findRepoUrl, HttpMethod.GET, new HttpEntity(authBaptisteHeaders), String.class);
            if (!findRepoResponse.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                throw new RepoNameInUseException(repository.getRepoName() + " is already in use.");
            }
            logger.debug("This should not happen.");
        } catch (HttpClientErrorException ex) {
            if (!ex.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                throw new RepoNameInUseException(repository.getRepoName() + " is already in use.");
            } else {
                String urlOverHttps = "https://api.github.com/repos/" + organizationName + "/" + templateRepo + "/generate";
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("owner", organizationName);
                jsonObject.put("name", repository.getRepoName());
                jsonObject.put("description", repository.getDescription());
                jsonObject.put("private", false);

                HttpEntity<String> httpEntity = new HttpEntity<String>(jsonObject.toString(), authBaptisteHeaders);
                ResponseEntity<String> response = new RestTemplate().exchange(urlOverHttps, HttpMethod.POST, httpEntity, String.class);

                repository.setUrl(getFieldFromResponseString(response, "html_url", HttpStatus.CREATED));
                repository.setCreated(true);
            }
        }
    }

    public boolean checkUsername(String username) {
        String urlOverHttps = "https://api.github.com/users/" + username;
        try {
            ResponseEntity<String> response
                    = new RestTemplate().exchange(urlOverHttps, HttpMethod.GET, new HttpEntity(authHeaders), String.class);

            logger.debug("check username " + username + ": exists");
            return username.equals(getFieldFromResponseString(response, "login"));
        } catch (HttpClientErrorException ex) {
            logger.debug("check username " + username + ": does not exist");
            return false;
        }
    }

    /**
     * To prevent abuse, the authenticated user is limited to 50 organization invitations per 24 hour period. If the
     * organization is more than one month old or on a paid plan, the limit is 500 invitations per 24 hour period.
     *
     * @param username username to invite to the organization
     * @return true if the invitation status is pending, false if the user already is a member of the organization
     */
    public InvitationState inviteUserToOrganization(String username) {
        ResponseEntity<String> response;
        String urlOverHttps = "https://api.github.com/orgs/" + organizationName + "/memberships/" + username;
        try {
            response = new RestTemplate().exchange(urlOverHttps, HttpMethod.GET, new HttpEntity(authHeaders), String.class);
            logger.debug(username + " is already in organization");
        } catch (HttpClientErrorException ex) {
            response = new RestTemplate().exchange(urlOverHttps, HttpMethod.PUT, new HttpEntity(authHeaders), String.class);
            logger.debug("invite " + username + " to organization");
        }
        switch (getFieldFromResponseString(response, "state")) {
            case "pending":
                return InvitationState.PENDING;
            case "active":
            case "admin":
                return InvitationState.ACCEPTED;
            default:
                return InvitationState.NONE;
        }
    }

    public void addUserAsCollaborator(String username, String repoName, String role) throws JSONException {
        String urlOverHttps = "https://api.github.com/repos/" + organizationName + "/" + repoName + "/collaborators/" + username;
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("permission", role);
        HttpEntity<String> httpEntity = new HttpEntity<String>(jsonObject.toString(), authDazzlerHeaders);
        ResponseEntity<String> response = new RestTemplate().exchange(urlOverHttps, HttpMethod.PUT, httpEntity, String.class);
    }

    public InvitationState handleTeammate(String teammateName, Repository repository) throws JSONException {
        InvitationState state = inviteUserToOrganization(teammateName);
        addUserAsCollaborator(teammateName, repository.getRepoName(), "push");
        logger.debug("Invite sent to: " + teammateName + " with push permission.");
        return state;
    }

    private String getFieldFromResponseString(ResponseEntity<String> response, String fieldName) {
        return getFieldFromResponseString(response, fieldName, HttpStatus.OK);
    }

    private String getFieldFromResponseString(ResponseEntity<String> response, String fieldName, HttpStatus status) {
        if (response.getStatusCode().equals(status)) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                Map<String, Object> map = mapper.readValue(response.getBody(), new TypeReference<Map<String, Object>>() {});
                return map.get(fieldName).toString();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
