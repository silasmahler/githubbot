package de.dohack.githubbot.entities;

public class RepoNameInUseException extends RuntimeException {
    public RepoNameInUseException(String message) {
        super(message);
    }
}
