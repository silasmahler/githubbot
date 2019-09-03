package de.dohack.githubbot.backend;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;

@Controller
public class LoginController {

    private final Bearer secretConfig;

    @Autowired
    public LoginController(Bearer bearer) {
        this.secretConfig = bearer;
    }

    @GetMapping(path = {"/", "/index"})
    public String getIndexPage(Principal principal, Model model) {
        String username = setUsername(principal);
        model.addAttribute("username", username);
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
    public Repository createRepository(@ModelAttribute Repository repository) {
        // TODO create @repository on GitHub:
        //   check if repoName is not already in use
        //   add creator and teammates to GitHub Organisation
        //   create @repository from template repository
        //   if @repository is created successfully, set created property of @repository to true
        //   return @repository
        System.out.println(repository);
        inviteUserToOrganization(repository.getCreator());

//        /repos/:template_owner/:template_repo/generate

        if (!"".equals(repository.getTeammateOne()) && checkUsername(repository.getTeammateOne())) {
            // TODO add teammateOne as collaborator
        }
        if (!"".equals(repository.getTeammateTwo()) && checkUsername(repository.getTeammateTwo())) {
            // TODO add teammateTwo as collaborator
        }
        if (!"".equals(repository.getTeammateThree()) && checkUsername(repository.getTeammateThree())) {
            // TODO add teammateThree as collaborator
        }
        if (!"".equals(repository.getTeammateFour()) && checkUsername(repository.getTeammateFour())) {
            // TODO add teammateFour as collaborator
        }

        return repository;
    }

    private String setUsername(Principal principal) {
        String username = "anonymous";
        if (principal != null) {
            OAuth2AuthenticationToken token = ((OAuth2AuthenticationToken) principal);
            username = token.getPrincipal().getAttributes().get("login").toString();
        }
        return username;
    }

    private boolean checkUsername(String username) {
        String urlOverHttps = "https://api.github.com/users/" + username;
        ResponseEntity<String> response = new RestTemplate().exchange(urlOverHttps, HttpMethod.GET, null, String.class);

        if (response.getStatusCode().equals(HttpStatus.OK)) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                Map<String, Object> map = mapper.readValue(response.getBody(), new TypeReference<Map<String, Object>>() {
                });
                return map.get("login").toString().equals(username);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }


    /**
     * To prevent abuse, the authenticated user is limited to 50 organization invitations per 24 hour period. If the
     * organization is more than one month old or on a paid plan, the limit is 500 invitations per 24 hour period.
     * @param username
     */
    private void inviteUserToOrganization(String username) {
        //        curl --user "groupowner:password" -X PUT -d "" "https://api.github.com/teams/TEAMID/memberships/USERNAMETOBEADDED"

//        String urlOverHttps = "https://api.github.com/org/dohack-githubbot/memberships/jo2";
        String urlOverHttps = "https://api.github.com/orgs/dohack-githubbot/members";

        System.out.println(secretConfig.getBearer());
        HttpHeaders headers = new HttpHeaders() {{
            String token = "Bearer " + secretConfig.getBearer();
            set( "Authorization", token );
        }};

        ResponseEntity<String> response = new RestTemplate().exchange(urlOverHttps, HttpMethod.GET, new HttpEntity(headers), String.class);

        System.out.println(response);

    }


}
