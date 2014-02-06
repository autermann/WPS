package org.n52.wps.algorithm.descriptor;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * @author Christian Autermann
 * @param <B> the type of this builder
 */
public abstract class DescriptorBuilder<B extends DescriptorBuilder<B>> {

    private final String identifier;
    private String title;
    private String abstrakt; // want 'abstract' but it's a java keyword

    protected DescriptorBuilder(String identifier) {
        checkArgument(!(identifier == null || identifier.isEmpty()),
                      "identifier may not be null or an empty String");
        this.identifier = identifier;
    }

    public B title(String title) {
        this.title = title;
        return self();
    }

    // want 'abstract' but it's a java keyword
    public B abstrakt(String abstrakt) {
        this.abstrakt = abstrakt;
        return self();
    }

    protected String getIdentifier() {
        return identifier;
    }

    protected String getTitle() {
        return title;
    }

    protected String getAbstrakt() {
        return abstrakt;
    }

    protected abstract B self();

}
