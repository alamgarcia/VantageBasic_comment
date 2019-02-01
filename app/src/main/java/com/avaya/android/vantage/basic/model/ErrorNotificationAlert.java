package com.avaya.android.vantage.basic.model;

/**
 * Error notification alert item.
 */
public class ErrorNotificationAlert {

    private String title;
    private String message;

    /**
     * Constructor
     *
     * @param title   error title
     * @param message error message
     */
    public ErrorNotificationAlert(String title, String message) {
        this.title = title;
        this.message = message;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return message;
    }

}
