package mx.ecosur.infonomia.dspace;

import org.dspace.content.Collection;

import java.util.Comparator;

/**
 */
public class CollectionsComparator implements Comparator<Collection> {

    @Override
    public int compare(Collection c1, Collection c2) {
        return c1.getName().compareTo(c2.getName());
    }
}
