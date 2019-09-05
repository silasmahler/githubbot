package de.dohack.githubbot.backend;

public class RepoNameInUseException extends RuntimeException {
    public RepoNameInUseException(String message) {
        super(message);
    }
}
