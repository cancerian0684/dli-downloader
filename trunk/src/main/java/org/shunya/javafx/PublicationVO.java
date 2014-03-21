package org.shunya.javafx;

import javafx.beans.property.*;
import org.shunya.dli.Publication;

public class PublicationVO {
    private final StringProperty title;
    private final IntegerProperty seq;
    private final Publication publication;
    private final BooleanProperty local;

    public PublicationVO(String barcode, String author, String title, int seq, Publication publication, boolean local) {
        this.publication = publication;
        this.seq = new SimpleIntegerProperty(seq);
        this.title = new SimpleStringProperty(barcode + "-" + title + ", " + author);
        this.local = new SimpleBooleanProperty(local);
    }

    public StringProperty titleProperty() { return title; }

    public IntegerProperty seqProperty() { return seq; }

    public BooleanProperty localProperty() {return local; }

    public Publication getPublication() {
        return publication;
    }
}