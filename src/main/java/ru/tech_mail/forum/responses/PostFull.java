package ru.tech_mail.forum.responses;

public class PostFull extends Post {
    private int likes;
    private int dislikes;
    private int points;

    public PostFull(int id, String message, String forumShortName, String userEmail, Integer parent,
                    Integer threadId, Integer likes, Integer dislikes, Integer points, boolean isDeleted,
                    boolean isSpam, boolean isEdited, boolean isApproved, boolean isHighlighted, String date) {
        super(id, message, forumShortName, userEmail, parent, threadId, isDeleted, isSpam,
                isEdited, isApproved, isHighlighted, date);
        this.likes = likes;
        this.dislikes = dislikes;
        this.points = points;
    }

    public int getLikes() {
        return likes;
    }

    public int getDislikes() {
        return dislikes;
    }

    public int getPoints() {
        return points;
    }

}
