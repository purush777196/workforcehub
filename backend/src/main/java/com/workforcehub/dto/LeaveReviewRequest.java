package com.workforcehub.dto;

/**
 * Request payload for approving or rejecting a leave application.
 */
public class LeaveReviewRequest {

    private String reviewComment;

    public LeaveReviewRequest() {
    }

    public LeaveReviewRequest(String reviewComment) {
        this.reviewComment = reviewComment;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }
}
