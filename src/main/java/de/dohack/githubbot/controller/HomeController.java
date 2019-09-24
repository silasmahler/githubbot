package de.dohack.githubbot.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.dohack.githubbot.entities.InvitationState;
import de.dohack.githubbot.entities.RepoNameInUseException;
import de.dohack.githubbot.entities.RepoNotTemplateException;
import de.dohack.githubbot.entities.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.PostConstruct;
import javax.validation.Valid;
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Controller
@Configuration
@PropertySource("classpath:secretConfig.properties")
public class HomeController {

    private Logger logger = LoggerFactory.getLogger(HomeController.class);

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

    @GetMapping(path = {"/", "/index"})
    public String getIndexPage(Principal principal, Model model) {
        model.addAttribute("username", setUsername(principal));
        return "index";
    }

    @GetMapping("/createRepository")
    public String getCreateRepositoryPage(Principal principal, Model model) {
        String username = setUsername(principal);
        model.addAttribute("username", username);

        Repository repository = new Repository();
        repository.setCreator(username);
        model.addAttribute("repository", repository);
        return "createRepository";
    }

    @PostMapping("/createRepository")
    public ModelAndView createRepository(@Valid Repository repository, BindingResult bindingResult, Principal principal) {
        ModelAndView mv = new ModelAndView();
        mv.addObject("username", setUsername(principal));
        logger.debug("Create Repository: " + repository);

        if (bindingResult.hasErrors()) {
            logger.error("errors");
            mv.setViewName("createRepository");
            return mv;
        }
        boolean teammateOneExists = false;
        boolean teammateTwoExists = false;
        boolean teammateThreeExists = false;
        boolean teammateFourExists = false;

        if (!repository.getTeammateOne().equals("") && !(teammateOneExists = checkUsername(repository.getTeammateOne()))) {
            bindingResult.addError(new FieldError("repository", "teammateOne", "GitHub name " + repository.getTeammateOne() + " not found."));
        }
        if (!repository.getTeammateTwo().equals("") && !(teammateTwoExists = checkUsername(repository.getTeammateTwo()))) {
            bindingResult.addError(new FieldError("repository", "teammateTwo", "GitHub name " + repository.getTeammateTwo() + " not found."));
        }
        if (!repository.getTeammateThree().equals("") && !(teammateThreeExists = checkUsername(repository.getTeammateThree()))) {
            bindingResult.addError(new FieldError("repository", "teammateThree", "GitHub name " + repository.getTeammateThree() + " not found."));
        }
        if (!repository.getTeammateFour().equals("") && !(teammateFourExists = checkUsername(repository.getTeammateFour()))) {
            bindingResult.addError(new FieldError("repository", "teammateFour", "GitHub name " + repository.getTeammateFour() + " not found."));
        }
        if (bindingResult.hasErrors()) {
            logger.error("errors");
            mv.setViewName("createRepository");
            return mv;
        }

        try {
            createRepo(repository);
            logger.debug("Repository created: " + repository.getRepoName());
        } catch (RepoNameInUseException | RepoNotTemplateException | NullPointerException | JSONException e) {
            logger.error("error: " + e.getClass() + ": " + e.getMessage());
        }

        if (repository.isCreated()) {
            Map<String, InvitationState> states = new HashMap<>();

            try {
                states.put("creator", inviteUserToOrganization(repository.getCreator()));
                addUserAsCollaborator(repository.getCreator(), repository.getRepoName(), "admin");
                logger.debug("Invite sent to: " + repository.getCreator() + " with admin permission.");

                if (teammateOneExists) {
                    states.put("teammateOne", handleTeammate(repository.getTeammateOne(), repository));
                } else {
                    repository.setTeammateOne(null);
                }
                if (teammateTwoExists) {
                    states.put("teammateTwo", handleTeammate(repository.getTeammateTwo(), repository));
                } else {
                    repository.setTeammateTwo(null);
                }
                if (teammateThreeExists) {
                    states.put("teammateThree", handleTeammate(repository.getTeammateThree(), repository));
                } else {
                    repository.setTeammateThree(null);
                }
                if (teammateFourExists) {
                    states.put("teammateFour", handleTeammate(repository.getTeammateFour(), repository));
                } else {
                    repository.setTeammateFour(null);
                }
            } catch (JSONException e) {
                logger.error("JSONException: " + e.getMessage());
            }

            if (bindingResult.hasErrors()) {
                logger.error("bindingErrors:");
                bindingResult.getAllErrors().forEach(objectError -> logger.error("errors: " + objectError.getDefaultMessage()));
                mv.setViewName("createRepository");
                return mv;
            }

            mv.setViewName("createRepoSuccess");
            mv.addObject("invitationStates", states);
            mv.addObject("repositoy", repository);
            logger.debug("Creation of " + repository.getRepoName() + " successful");
            return mv;
        } else {
            mv.setViewName("createRepository");
            logger.error("errors");
            return mv;
        }
    }

    private String setUsername(Principal principal) {
        String username = "anonymous";
        if (principal != null) {
            OAuth2AuthenticationToken token = ((OAuth2AuthenticationToken) principal);
            username = token.getPrincipal().getAttributes().get("login").toString();
        }
        return username;
    }

    private void createRepo(Repository repository) throws RepoNameInUseException, RepoNotTemplateException, JSONException {
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

    private boolean checkUsername(String username) {
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
    private InvitationState inviteUserToOrganization(String username) {
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

    private void addUserAsCollaborator(String username, String repoName, String role) throws JSONException {
        String urlOverHttps = "https://api.github.com/repos/" + organizationName + "/" + repoName + "/collaborators/" + username;
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("permission", role);
        HttpEntity<String> httpEntity = new HttpEntity<String>(jsonObject.toString(), authDazzlerHeaders);
        ResponseEntity<String> response = new RestTemplate().exchange(urlOverHttps, HttpMethod.PUT, httpEntity, String.class);
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

    private InvitationState handleTeammate(String teammateName, Repository repository) throws JSONException {
        InvitationState state = inviteUserToOrganization(teammateName);
        addUserAsCollaborator(teammateName, repository.getRepoName(), "push");
        logger.debug("Invite sent to: " + teammateName + " with push permission.");
        return state;
    }
}
