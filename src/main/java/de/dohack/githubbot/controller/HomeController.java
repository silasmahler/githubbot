package de.dohack.githubbot.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.dohack.githubbot.entities.RepoNameInUseException;
import de.dohack.githubbot.entities.InvitationState;
import de.dohack.githubbot.entities.Repository;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@Controller
@Configuration
@PropertySource("classpath:secretConfig.properties")
public class HomeController {

    @Value("${secret.config.bearer}")
    private String bearer;

    private HttpHeaders authHeaders;
    private HttpHeaders authBaptisteHeaders;
    private HttpHeaders authDazzlerHeaders;

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
    public ModelAndView createRepository(@ModelAttribute Repository repository, BindingResult bindingResult, Principal principal) {
        ModelAndView mv = new ModelAndView();
        mv.addObject("username", setUsername(principal));

        try {
            repository = createRepo(repository);
        } catch (RepoNameInUseException e) {
            bindingResult.addError(new ObjectError("repoName", e.getMessage()));
        }
        if (repository == null) {
            bindingResult.addError(new ObjectError("repoName", "There were problems creating your repo."));
        }

        if (repository != null && repository.isCreated()) {
            Map<String, InvitationState> states = new HashMap<>();
            states.put("creator", inviteUserToOrganization(repository.getCreator()));
            addUserAsCollaborator(repository.getCreator(), repository.getRepoName());
            states.put("teammateOne", handleTeammate(repository.getTeammateOne(), repository, "teammateOne", bindingResult));
            states.put("teammateTwo", handleTeammate(repository.getTeammateTwo(), repository, "teammateTwo", bindingResult));
            states.put("teammateThree", handleTeammate(repository.getTeammateThree(), repository, "teammateThree", bindingResult));
            states.put("teammateFour", handleTeammate(repository.getTeammateFour(), repository, "teammateFour", bindingResult));

            if (bindingResult.hasErrors()) {
                mv.setViewName("createRepository");
                return mv;
            }

            mv.setViewName("createRepoSuccess");
            mv.addObject("invitationStates", states);
            mv.addObject("repositoy", repository);
            return mv;
        } else {
            mv.setViewName("createRepository");
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

    private Repository createRepo(Repository repository) throws RepoNameInUseException {
        String get = "https://api.github.com/repos/dohack-githubbot/test-template";
        ResponseEntity<String> getResponse = new RestTemplate().exchange(get, HttpMethod.GET, new HttpEntity(authBaptisteHeaders), String.class);

        if (!"true".equals(getFieldFromResponseString(getResponse, "is_template"))) {
            return null;
        }

        String urlOverHttps = "https://api.github.com/repos/dohack-githubbot/" + repository.getRepoName();
        ResponseEntity<String> response = new RestTemplate().exchange(urlOverHttps, HttpMethod.GET, new HttpEntity(authBaptisteHeaders), String.class);

        repository.setUrl(getFieldFromResponseString(response, "html_url"));
        repository.setCreated(repository.getUrl() != null && !"".equals(repository.getUrl()));
        return repository;

        /*try {
            if (!response.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                throw new RepoNameInUseException(repository.getRepoName() + " is already in use.");
            }

            String urlOverHttps = "https://api.github.com/repos/dohack-githubbot/test-template/generate";
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("owner", "dohack-githubbot");
            jsonObject.put("name", repository.getRepoName());
            jsonObject.put("description", repository.getDescription());
            jsonObject.put("private", false);

            HttpEntity<String> httpEntity = new HttpEntity<String>(jsonObject.toString(), authPreviewHeaders);
            ResponseEntity<String> response = new RestTemplate().exchange(urlOverHttps, HttpMethod.POST, httpEntity, String.class);

            repository.setUrl(getFieldFromResponseString(response, "html_url", HttpStatus.CREATED));
            return repository;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;*/
    }

    private boolean checkUsername(String username) {
        String urlOverHttps = "https://api.github.com/users/" + username;
        ResponseEntity<String> response
                = new RestTemplate().exchange(urlOverHttps, HttpMethod.GET, new HttpEntity(authHeaders), String.class);

        return username.equals(getFieldFromResponseString(response, "login"));
    }

    /**
     * To prevent abuse, the authenticated user is limited to 50 organization invitations per 24 hour period. If the
     * organization is more than one month old or on a paid plan, the limit is 500 invitations per 24 hour period.
     *
     * @param username username to invite to the organization
     * @return true if the invitation status is pending, false if the user already is a member of the organization
     */
    private InvitationState inviteUserToOrganization(String username) {
        String urlOverHttps = "https://api.github.com/orgs/dohack-githubbot/memberships/" + username;
        ResponseEntity<String> response
                = new RestTemplate().exchange(urlOverHttps, HttpMethod.PUT, new HttpEntity(authHeaders), String.class);

        switch (getFieldFromResponseString(response, "state")) {
            case "pending":
                return InvitationState.PENDING;
            case "active":
                return InvitationState.ACCEPTED;
            default:
                return InvitationState.NONE;
        }
    }

    private void addUserAsCollaborator(String username, String repoName) {
        String urlOverHttps = "https://api.github.com/repos/dohack-githubbot/" + repoName + "/collaborators/" + username;
        ResponseEntity<String> response = new RestTemplate().exchange(urlOverHttps, HttpMethod.PUT, new HttpEntity(authDazzlerHeaders), String.class);
    }

    private String getFieldFromResponseString(ResponseEntity<String> response, String fieldName) {
        return getFieldFromResponseString(response, fieldName, HttpStatus.OK);
    }

    private String getFieldFromResponseString(ResponseEntity<String> response, String fieldName, HttpStatus status) {
        if (response.getStatusCode().equals(status)) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                Map<String, Object> map = mapper.readValue(response.getBody(), new TypeReference<Map<String, Object>>() {
                });
                return map.get(fieldName).toString();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private InvitationState handleTeammate(String teammateName, Repository repository, String attributeName, BindingResult bindingResult) {
        InvitationState state = InvitationState.NONE;
        if (!"".equals(teammateName) && checkUsername(teammateName)) {
            state = inviteUserToOrganization(teammateName);
            addUserAsCollaborator(teammateName, repository.getRepoName());
        } else {
            if (!"".equals(teammateName)) {
                bindingResult.addError(new ObjectError(attributeName, "GitHub name " + teammateName + " does not exist."));
            }
            switch (attributeName) {
                case "teammateOne": repository.setTeammateOne(null); break;
                case "teammateTwo": repository.setTeammateTwo(null); break;
                case "teammateThree": repository.setTeammateThree(null); break;
                case "teammateFour": repository.setTeammateFour(null); break;
                default: break;
            }
        }
        return state;
    }
}
