/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
/**
 * <!DOCTYPE HTML PUBLIC "-//IETF//DTD HTML 2.0//EN">
 * <html><head>
 * <title>301 Moved Permanently</title>
 * </head><body>
 * <h1>Moved Permanently</h1>
 * <p>The document has moved <a href="https://svn.duraspace.org/dspace/licenses/LICENSE_HEADER">here</a>.</p>
 * </body></html>
 */
package mx.ecosur.infonomia.dspace;

import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.sword2.*;
import org.swordapp.server.*;

public class ExtendedMetadataEntryIngester extends SimpleDCEntryIngester implements SwordEntryIngester
{

	@Override
    public DepositResult ingestToItem(Context context, Deposit deposit, Item item, VerboseDescription verboseDescription, DepositResult result, boolean replace)
            throws DSpaceSwordException, SwordError, SwordAuthException, SwordServerException
    {
        result = super.ingestToItem(context, deposit, item, verboseDescription, result, replace);
        item = result.getItem();
        /* Add any Metadata registered in DSpace */
        ExtendCommand command = new ExtendCommand();
        item = command.execute(context, deposit, item).getItem();
		result.setItem(item);
		return result;
	}

    @Override
	public DepositResult ingestToCollection(Context context, Deposit deposit, Collection collection, VerboseDescription verboseDescription, DepositResult result)
            throws DSpaceSwordException, SwordError, SwordAuthException, SwordServerException
    {
            result = super.ingestToCollection(context, deposit, collection, verboseDescription, result);
            Item item = result.getItem();
            /* Add any Metadata registered in DSpace */
            ExtendCommand command = new ExtendCommand();
            item = command.execute(context, deposit, item).getItem();
            setUpdatedDate(item, verboseDescription);
			verboseDescription.append("Ingest successful");
			verboseDescription.append("Custom metadata registered in dspace appended to item with internal identifier: "
                    + item.getID());
			result.setItem(item);
			return result;
    }
}
