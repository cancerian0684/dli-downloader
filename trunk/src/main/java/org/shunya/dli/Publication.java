package org.shunya.dli;

public class Publication {
    private String title;
    private String barcode;
    private String author;
    private String language;
    private String subject;
    private String pages;
    private String year;
    private String url;
    private boolean present;
    private String localPath;
    private String searchText;
    private String summary;

    public Publication(String title, String barcode, String author, String language, String subject, String pages, String year) {
        this.title = title;
        this.barcode = barcode;
        this.author = author;
        this.language = language;
        this.subject = subject;
        this.pages = pages;
        this.year = year;
    }

    public String getTitle() {
        return title;
    }

    public String getBarcode() {
        return barcode;
    }

    public String getAuthor() {
        return author;
    }

    public String getLanguage() {
        return language;
    }

    public String getSubject() {
        return subject;
    }

    public String getPages() {
        return pages;
    }

    public String getYear() {
        return year;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public boolean isPresent() {
        return present;
    }

    public void setPresent(boolean present) {
        this.present = present;
    }

    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    @Override
    public String toString() {
        if (summary == null) {
            StringBuilder sbf = new StringBuilder(barcode + " - " + title);
            if (Utils.checkNotNull(author))
                sbf.append(", " + author);
            if (Utils.checkNotNull(pages))
                sbf.append(", " + pages + "p");
            if (Utils.checkNotNull(subject))
                sbf.append(", " + subject);
            if (Utils.checkNotNull(language))
                sbf.append(", " + language);
            if (Utils.checkNotNull(year))
                sbf.append(" (" + getYear() + ")");
            summary = sbf.toString();
        }
        return summary;
    }
}
