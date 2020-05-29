package com.github.jeuxjeux20.loupsgarous.game.lobby;

public class MaximumWorldCountReachedException extends CannotCreateWorldException {
    private final int maximumWorldCount;

    public MaximumWorldCountReachedException(int maximumWorldCount) {
        this.maximumWorldCount = maximumWorldCount;
    }

    public MaximumWorldCountReachedException(String message, int maximumWorldCount) {
        super(message);
        this.maximumWorldCount = maximumWorldCount;
    }

    public MaximumWorldCountReachedException(String message, Throwable cause, int maximumWorldCount) {
        super(message, cause);
        this.maximumWorldCount = maximumWorldCount;
    }

    public MaximumWorldCountReachedException(Throwable cause, int maximumWorldCount) {
        super(cause);
        this.maximumWorldCount = maximumWorldCount;
    }

    public int getMaximumWorldCount() {
        return maximumWorldCount;
    }
}
