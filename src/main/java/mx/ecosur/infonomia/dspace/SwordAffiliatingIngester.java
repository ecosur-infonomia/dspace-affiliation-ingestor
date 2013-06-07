package mx.ecosur.infonomia.dspace;

import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.sword2.*;
import org.swordapp.server.*;

import java.sql.SQLException;

/**
 * SwordAffiliatingIngester
 *
 * Affiliates an Item with a set of collections passed in with the
 * sword update deposit.
 *
 * Otherwise, invokes the default Entry ingestion behavior coded
 * within the ExtendedMetadataEntryIngester (default ingester).
 *
 * @author "Andrew Waterman" <awaterma@ecosur.mx>
 */
public class SwordAffiliatingIngester implements SwordEntryIngester {

    private final AffiliateCommand command = new AffiliateCommand();

    /* Default ingester. */
    private ExtendedMetadataEntryIngester ingester;

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
                return command.execute(context, deposit, item);
            } catch (SQLException e) {
                throw new DSpaceSwordException(e);
            }
        } else {
            ingester = new ExtendedMetadataEntryIngester();
            return ingester.ingest(context, deposit, dSpaceObject, verboseDescription);
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
            return command.execute(context, deposit, item);

        } else {
            ingester = new ExtendedMetadataEntryIngester();
            return ingester.ingest(context, deposit, dSpaceObject, verboseDescription, depositResult, b);
        }
    }
}