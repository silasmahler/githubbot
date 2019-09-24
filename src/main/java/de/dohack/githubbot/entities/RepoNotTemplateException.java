package de.dohack.githubbot.entities;

public class RepoNotTemplateException extends RuntimeException {
    public RepoNotTemplateException(String message) {
        super(message);
    }
}
