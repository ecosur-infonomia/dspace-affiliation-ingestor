package mx.ecosur.infonomia.dspace;

import org.apache.abdera.model.Element;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.sword2.*;
import org.swordapp.server.*;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

/**
 * SwordAffiliatingIngester
 *
 * Affiliates an Item with a group of collections passed in as part of the
 * sword deposit.
 *
 */
public class SwordAffiliatingIngester implements SwordEntryIngester {

    /* Default ingester */
    private SimpleDCEntryIngester defaultIngester;

    @Override
    public DepositResult ingest(Context context, Deposit deposit, DSpaceObject dSpaceObject,
                VerboseDescription verboseDescription)
            throws DSpaceSwordException, SwordError, SwordAuthException, SwordServerException
    {
        /* Check and see if we need to use the defaultIngester on this deposit */
        if (!deposit.getSwordEntry().getEntry().getContent().contains("affiliate")) {
            defaultIngester = new SimpleDCEntryIngester();
            return defaultIngester.ingest(context, deposit, dSpaceObject, verboseDescription);
        }

        try {
            String entryId = deposit.getSwordEntry().getEntry().getId().toASCIIString();
            /* Decode id */
            int endTag = entryId.lastIndexOf("/");
            int id = Integer.parseInt(entryId.substring(endTag));
            Item item = Item.find(context, id);
            return affiliate(context, item, deposit);
        } catch (SQLException e) {
            throw new DSpaceSwordException(e);
        }
    }

    @Override
    public DepositResult ingest(Context context, Deposit deposit, DSpaceObject dSpaceObject,
                VerboseDescription verboseDescription, DepositResult depositResult, boolean b)
            throws DSpaceSwordException, SwordError, SwordAuthException, SwordServerException
    {
        /* Check and see if we need to use the defaultIngester on this deposit */
        if (!deposit.getSwordEntry().getEntry().getContent().contains("affiliate")) {
            defaultIngester = new SimpleDCEntryIngester();
            return defaultIngester.ingest(context, deposit, dSpaceObject, verboseDescription, depositResult, b);
        }

        if (depositResult != null) {
            return affiliate(context, depositResult.getItem(), deposit);
        } else {
            throw new DSpaceSwordException("Empty DepositResult! Unable to proceed.");
        }
    }

    private Collection binarySearch(Collection[] collections, String collection) {
        return collections [binarySearch(collections, collection, 0, collections.length)];
    }

    private int binarySearch (Collection[] collections, String key, int s, int e) {
        if (s >= e)
            return -1;
        /* Do the binary search */
        int m = e/2;
        if (collections[m].getName().compareTo(key) == -1) {
           return binarySearch(collections, key, s, m);
        } else if (collections[m].getName().compareTo(key) == 1) {
            return binarySearch(collections, key, m + 1, e);
        } else {
            return m;  // we found our key!
        }
    }

    private DepositResult affiliate(Context context, Item item, Deposit deposit) throws SwordServerException, SwordAuthException {
        try {
            Collection[] collections = Collection.findAll(context);
            Arrays.sort(collections, new CollectionsComparator());

            /* Get the XML contained within the deposit */
            SwordEntry se = deposit.getSwordEntry();
            List<Element> e = se.getEntry().getElements();
            /* Within the entry, is just one element, the affiliate element, and its
               set of collections. So, the plug-in only works with this top element.
             */
            Element affiliate = e.iterator().next();  // list.head()
            if (affiliate != null) {
                List<Element> elements = affiliate.getElements();
                for (Element element : elements) {
                    String name = element.getAttributeValue("name");
                    Collection collection = binarySearch(collections, name);
                    if (!item.isOwningCollection(collection)) {
                        try {
                            collection.addItem(item);
                            collection.update();
                        } catch (AuthorizeException except) {
                            System.out.println(except.getMessage());
                            except.printStackTrace();
                            throw except;
                        }
                    }
                }
            }

            /* Result processing */
            DepositResult ret = new DepositResult();
            ret.setItem(item);
            ret.setTreatment("Added item to requested non-owning collections.");
            return ret;

        } catch (SQLException e) {
            throw new SwordServerException(e);
        } catch (AuthorizeException e) {
            throw new SwordAuthException(e);
        }
    }
}
