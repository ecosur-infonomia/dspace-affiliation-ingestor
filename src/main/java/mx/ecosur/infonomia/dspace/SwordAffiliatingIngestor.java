package mx.ecosur.infonomia.dspace;

import org.apache.abdera.model.Element;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.sword2.DSpaceSwordException;
import org.dspace.sword2.DepositResult;
import org.dspace.sword2.SwordEntryIngester;
import org.dspace.sword2.VerboseDescription;
import org.swordapp.server.*;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

/**
 * SwordAffiliatingIngestor
 *
 * Affiliates an Item with a group of collections passed in as part of the
 * sword deposit.
 *
 */
public class SwordAffiliatingIngestor implements SwordEntryIngester {
    @Override
    public DepositResult ingest(Context context, Deposit deposit, DSpaceObject dSpaceObject,
                VerboseDescription verboseDescription)
            throws DSpaceSwordException, SwordError, SwordAuthException, SwordServerException
    {
        int did = dSpaceObject.getID();

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
        if (collections[s].getName() == key)
            return s;
        /* Do the binary search */
        int m = e/2;
        if (collections[m].getName().compareTo(key) < m) {
           return binarySearch(collections, key, s, m);
        } else {
            return binarySearch(collections, key, m + 1, e);
        }
    }

    private DepositResult affiliate(Context context, Item item, Deposit deposit) throws SwordServerException, SwordAuthException {
        try {
            Collection[] collections = Collection.findAll(context);
            Arrays.sort(collections, new CollectionsComparator());

            /* Get the XML contained within the deposit */
            SwordEntry se = deposit.getSwordEntry();
            List<Element> elements = se.getEntry().getElements();
            for (Element element : elements) {
                String name = element.getAttributeValue("name");
                Collection collection = binarySearch(collections, name);
                collection.addItem(item);
            }

            /* Result processing */
            DepositResult ret = new DepositResult();
            ret.setItem(item);
            ret.setTreatment("Added collections to item.");
            return ret;

        } catch (SQLException e) {
            throw new SwordServerException(e);
        } catch (AuthorizeException e) {
            throw new SwordAuthException(e);
        }
    }
}
