package org.n52.wps.commons;

import static com.google.common.base.Preconditions.checkArgument;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import com.google.common.base.Objects;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Class to represent different permutations of {@link Format}s.
 *
 * @author Christian Autermann
 */
public class FormatPermutation implements Set<Format> {
    private final Set<String> mimeTypes;
    private final Set<String> encodings;
    private final Set<String> schemas;
    private final int size;

    /**
     * Creates a new Format permutation using the supplied components.
     *
     * @param mimeTypes the mimeTypes to use
     * @param encodings the encodings to use
     * @param schemas   the schemas to use
     */
    public FormatPermutation(Iterable<String> mimeTypes,
                             Iterable<String> encodings,
                             Iterable<String> schemas) {
        checkArgument(mimeTypes != null, "mimeTypes may not be null");
        checkArgument(schemas != null, "schemas may not be null");
        checkArgument(encodings != null, "encodings may not be null");
        this.mimeTypes = Sets.newLinkedHashSet(mimeTypes);
        this.schemas = Sets.newLinkedHashSet(schemas);
        this.encodings = Sets.newLinkedHashSet(encodings);
        if (this.schemas.isEmpty()) {
            this.schemas.add(null);
        }
        if (this.encodings.isEmpty()) {
            this.encodings.add(null);
        }
        this.size = this.mimeTypes.size() *
                    this.encodings.size() *
                    this.schemas.size();
    }

    /**
     * @return the mimeTypes of this permutation
     */
    public Set<String> getMimeTypes() {
        return Collections.unmodifiableSet(mimeTypes);
    }

    /**
     * @return the schemas of this permutation
     */
    public Set<String> getSchemas() {
        return Collections.unmodifiableSet(schemas);
    }

    /**
     * @return the encodings of this permutation
     */
    public Set<String> getEncodings() {
        return Collections.unmodifiableSet(encodings);
    }

    /**
     * @return get the size of the permutation
     */
    public int size() {
        return this.size;
    }

    @Override
    public Iterator<Format> iterator() {
        if (isEmpty()) {
            return Iterators.emptyIterator();
        }
        return new PermutatingIterator<Format>(this.mimeTypes,
                                               this.encodings,
                                               this.schemas) {
            @Override
            protected Format create(Object[] values) {
                return new Format((String) values[0],
                                  (String) values[1],
                                  (String) values[2]);
            }
        };
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getMimeTypes(), getEncodings(), getSchemas());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && getClass() == obj.getClass()) {
            final FormatPermutation that = (FormatPermutation) obj;
            return Objects.equal(this.getMimeTypes(), that.getMimeTypes()) &&
                   Objects.equal(this.getSchemas(), that.getSchemas()) &&
                   Objects.equal(this.getEncodings(), that.getEncodings());
        }
        return false;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("mimeTypes", getMimeTypes())
                .add("encodings", getEncodings())
                .add("schemas", getSchemas())
                .toString();
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean contains(Object o) {
        if (o instanceof Format) {
            Format format = (Format) o;
            return mimeTypes.contains(format.getMimeType().orNull()) &&
                   encodings.contains(format.getEncoding().orNull()) &&
                   schemas.contains(format.getSchema().orNull());
        }
        return false;
    }

    @Override
    public Format[] toArray() {
        Format[] formats = new Format[this.size];
        Iterator<Format> iter = iterator();
        for (int i = 0; i <= this.size; ++i) {
            formats[i] = iter.next();
        }
        return formats;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        T[] r = a.length >= this.size ? a : (T[]) Array.newInstance(a.getClass()
                .getComponentType(), this.size);
        Iterator<Format> it = iterator();
        for (int i = 0; i <= this.size; ++i) {
            r[i] = (T) it.next();
        }
        return r;
    }

    @Override
    public boolean add(Format e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends Format> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    public static void main(String[] args) {
        FormatPermutation f
                = new FormatPermutation(
                        Lists
                        .<String>newArrayList("application/xml", "text/xml"),
                        Lists.<String>newArrayList("UTF-8", "UTF-16", ""),
                        Lists.<String>newArrayList("a", "b", ""));
        System.out.println(f);
        for (Format format : f) {
            System.out.println(format);
        }
    }
}
