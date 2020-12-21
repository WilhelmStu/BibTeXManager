package org.wst.model;

public class TableEntry {
    private String keyword;
    private String type;
    private String title;
    private String author;
    private String year;

    public enum  Error {
        NONE,
        FILE_NOT_FOUND,
        FILE_READ_ERROR,
        NO_ENTRIES_FOUND

    }
   Error error = Error.NONE;


    public TableEntry(Error error) {
        this.error = error;
    }

    public TableEntry(String keyword, String type, String title, String author, String year) {
        this.keyword = keyword;
        this.type = type;
        this.title = title;
        this.author = author;
        this.year = year;
    }

    public TableEntry(String keyword, String title, String author, String year) {
        this.keyword = keyword;
        this.title = title;
        this.author = author;
        this.year = year;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public Error getError() {
        return error;
    }
}
