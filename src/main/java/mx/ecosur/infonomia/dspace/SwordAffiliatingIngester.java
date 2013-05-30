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
 * Affiliates an Item with a set of collections passed in with the
 * sword update deposit.
 *
 * Otherwise, invokes the default Entry ingestion behavior coded
 * within the SimpleDCEntryIngester (default ingester).
 *
 * @author "Andrew Waterman" <awaterma@ecosur.mx>
 */
public class SwordAffiliatingIngester implements SwordEntryIngester {

    /* Default ingester. Required due to "Single" Plugin. */
    private SimpleDCEntryIngester defaultIngester;

    @Override
    public DepositResult ingest(Context context, Deposit deposit, DSpaceObject dSpaceObject,
                VerboseDescription verboseDescription)
            throws DSpaceSwordException, SwordError, SwordAuthException, SwordServerException
    {
        if (deposit.getSwordEntry().getEntry().getFirstChild() != null &&
                deposit.getSwordEntry().getEntry().getFirstChild().getQName().getLocalPart() == "affiliate")
        {
            try {
                Item item = Item.find(context, dSpaceObject.getID());
                return affiliate(context, item, deposit);
            } catch (SQLException e) {
                throw new DSpaceSwordException(e);
            }
        } else {
            defaultIngester = new SimpleDCEntryIngester();
            return defaultIngester.ingest(context, deposit, dSpaceObject, verboseDescription);
        }
    }

    @Override
    public DepositResult ingest(Context context, Deposit deposit, DSpaceObject dSpaceObject,
                VerboseDescription verboseDescription, DepositResult depositResult, boolean b)
            throws DSpaceSwordException, SwordError, SwordAuthException, SwordServerException
    {
        if (deposit.getSwordEntry().getEntry().getFirstChild() != null &&
                deposit.getSwordEntry().getEntry().getFirstChild().getQName().getLocalPart() == "affiliate")
        {
            Item item = null;
            /* Try and extract item from deposit result */
            if (depositResult != null) {
                item = depositResult.getItem();
            }

            /* Null item? Load by dspaceObject id */
            if (item == null) {
                try {
                    item = Item.find(context, dSpaceObject.getID());
                } catch (SQLException e) {
                    e.printStackTrace();
                    throw new SwordServerException(e);
                }
            }
            return affiliate(context, item, deposit);

        } else {
            defaultIngester = new SimpleDCEntryIngester();
            return defaultIngester.ingest(context, deposit, dSpaceObject, verboseDescription, depositResult, b);
        }
    }

    private Collection binarySearch(Collection[] collections, String collection) {
        int idx = binarySearch(collections, collection, 0, collections.length);
        if (idx >= 0) {
            return collections [idx];
        } else {
            return null;
        }
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

            /* Workaround for auth issues in context editing */
            boolean ignore = context.ignoreAuthorization();
            context.setIgnoreAuthorization(true);

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
                    if (collection != null && !item.isIn(collection)) {
                        collection.addItem(item);
                        collection.update();
                    }
                }
            }

            /* Reset context */
            context.setIgnoreAuthorization(ignore);

            /* Result processing */
            DepositResult ret = new DepositResult();
            ret.setItem(item);
            ret.setTreatment("Affiliated item to requested available collections.");
            return ret;

        } catch (SQLException e) {
            throw new SwordServerException(e);
        } catch (AuthorizeException e) {
            throw new SwordAuthException(e);
        }
    }
}