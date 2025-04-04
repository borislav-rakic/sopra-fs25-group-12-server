package ch.uzh.ifi.hase.soprafs24.rest.dto;

import ch.uzh.ifi.hase.soprafs24.constant.FriendshipStatus;

public class FriendshipStatusDTO {

    private FriendshipStatus status;
    private boolean initiatedByCurrentUser;

    public FriendshipStatus getStatus() {
        return status;
    }

    public void setStatus(FriendshipStatus status) {
        this.status = status;
    }

    public boolean isInitiatedByCurrentUser() {
        return initiatedByCurrentUser;
    }

    public void setInitiatedByCurrentUser(boolean initiatedByCurrentUser) {
        this.initiatedByCurrentUser = initiatedByCurrentUser;
    }
}
