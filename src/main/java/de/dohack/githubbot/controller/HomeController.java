package de.dohack.githubbot.controller;

import de.dohack.githubbot.entities.InvitationState;
import de.dohack.githubbot.entities.RepoNameInUseException;
import de.dohack.githubbot.entities.RepoNotTemplateException;
import de.dohack.githubbot.entities.Repository;
import de.dohack.githubbot.services.GitHubService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.validation.Valid;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@Controller
@Configuration
@PropertySource("classpath:secretConfig.properties")
public class HomeController {

    private Logger logger = LoggerFactory.getLogger(HomeController.class);

    private final GitHubService gitHubService;

    @Autowired
    public HomeController(GitHubService gitHubService) {
        this.gitHubService = gitHubService;
    }

    @GetMapping(path = {"/", "/index"})
    public String getIndexPage(Principal principal, Model model) {
        model.addAttribute("username", gitHubService.setUsername(principal));
        return "index";
    }

    @GetMapping("/createRepository")
    public String getCreateRepositoryPage(Principal principal, Model model) {
        String username = gitHubService.setUsername(principal);
        model.addAttribute("username", username);

        Repository repository = new Repository();
        repository.setCreator(username);
        model.addAttribute("repository", repository);
        return "createRepository";
    }

    @PostMapping("/createRepository")
    public ModelAndView createRepository(@Valid Repository repository, BindingResult bindingResult, Principal principal) {
        ModelAndView mv = new ModelAndView();
        mv.addObject("username", gitHubService.setUsername(principal));
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

        if (!repository.getTeammateOne().equals("") && !(teammateOneExists = gitHubService.checkUsername(repository.getTeammateOne()))) {
            bindingResult.addError(new FieldError("repository", "teammateOne", "GitHub name " + repository.getTeammateOne() + " not found."));
        }
        if (!repository.getTeammateTwo().equals("") && !(teammateTwoExists = gitHubService.checkUsername(repository.getTeammateTwo()))) {
            bindingResult.addError(new FieldError("repository", "teammateTwo", "GitHub name " + repository.getTeammateTwo() + " not found."));
        }
        if (!repository.getTeammateThree().equals("") && !(teammateThreeExists = gitHubService.checkUsername(repository.getTeammateThree()))) {
            bindingResult.addError(new FieldError("repository", "teammateThree", "GitHub name " + repository.getTeammateThree() + " not found."));
        }
        if (!repository.getTeammateFour().equals("") && !(teammateFourExists = gitHubService.checkUsername(repository.getTeammateFour()))) {
            bindingResult.addError(new FieldError("repository", "teammateFour", "GitHub name " + repository.getTeammateFour() + " not found."));
        }
        if (bindingResult.hasErrors()) {
            logger.error("errors");
            mv.setViewName("createRepository");
            return mv;
        }

        try {
            gitHubService.createRepo(repository);
            logger.debug("Repository created: " + repository.getRepoName());
        } catch (RepoNameInUseException | RepoNotTemplateException | NullPointerException | JSONException e) {
            logger.error("error: " + e.getClass() + ": " + e.getMessage());
        }

        if (repository.isCreated()) {
            Map<String, InvitationState> states = new HashMap<>();

            try {
                states.put("creator", gitHubService.inviteUserToOrganization(repository.getCreator()));
                gitHubService.addUserAsCollaborator(repository.getCreator(), repository.getRepoName(), "admin");
                logger.debug("Invite sent to: " + repository.getCreator() + " with admin permission.");

                if (teammateOneExists) {
                    states.put("teammateOne", gitHubService.handleTeammate(repository.getTeammateOne(), repository));
                } else {
                    repository.setTeammateOne(null);
                }
                if (teammateTwoExists) {
                    states.put("teammateTwo", gitHubService.handleTeammate(repository.getTeammateTwo(), repository));
                } else {
                    repository.setTeammateTwo(null);
                }
                if (teammateThreeExists) {
                    states.put("teammateThree", gitHubService.handleTeammate(repository.getTeammateThree(), repository));
                } else {
                    repository.setTeammateThree(null);
                }
                if (teammateFourExists) {
                    states.put("teammateFour", gitHubService.handleTeammate(repository.getTeammateFour(), repository));
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
}
