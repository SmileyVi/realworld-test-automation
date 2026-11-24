package io.realworld.qa.support;

public enum TestAccountRole {
    ARTICLE_AUTHOR("article author"),
    ARTICLE_READER("article reader"),
    NON_AUTHOR("non-author"),
    LOGIN_USER("login user"),
    PROFILE_OWNER("profile owner"),
    NEW_USER("new user");

    private final String label;

    TestAccountRole(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
