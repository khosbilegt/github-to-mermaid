package mn.khosbilegt.client.dto;

public class CommitDetails {
    private String url;
    private GitUser author;
    private GitUser committer;
    private String message;
    private int commentCount;
    private Tree tree;
    private Verification verification;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public GitUser getAuthor() {
        return author;
    }

    public void setAuthor(GitUser author) {
        this.author = author;
    }

    public GitUser getCommitter() {
        return committer;
    }

    public void setCommitter(GitUser committer) {
        this.committer = committer;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public Tree getTree() {
        return tree;
    }

    public void setTree(Tree tree) {
        this.tree = tree;
    }

    public Verification getVerification() {
        return verification;
    }

    public void setVerification(Verification verification) {
        this.verification = verification;
    }
}