package org.wst.model;

/**
 * Table entry class for the TableView in the App
 * Every row in the table requires an entry
 * The entries are also used to store additional information's
 * like URL, DOI and type OR an Error if there is a problem during creation
 */
public class TableEntry {
    private String keyword;
    private String type;
    private String title;
    private String author;
    private String year;
    private String url;
    private String doi;

    public enum Error {
        NONE,
        FILE_NOT_FOUND,
        FILE_READ_ERROR,
        NO_ENTRIES_FOUND
    }

    Error error = Error.NONE;


    public TableEntry(Error error) {
        this.error = error;
    }

    public TableEntry(String keyword, String type, String title, String author, String year, String url, String doi) {
        this.keyword = keyword;
        this.type = type;
        this.title = title;
        this.author = author;
        this.year = year;
        this.url = url;
        this.doi = doi;
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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }
}
