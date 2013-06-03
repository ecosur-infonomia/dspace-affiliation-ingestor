package mx.ecosur.infonomia.dspace;

import org.apache.abdera.model.Element;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.sword2.DepositResult;
import org.swordapp.server.Deposit;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordEntry;
import org.swordapp.server.SwordServerException;

import java.sql.SQLException;
import java.util.List;
import java.util.TreeMap;

public class AffiliateCommand {

    DepositResult execute(Context context, Item item, Deposit deposit) throws SwordServerException, SwordAuthException {
        try {
            Collection[] collections = Collection.findAll(context);
            TreeMap<String, Integer> map = new TreeMap<String, Integer>();
            /* Fill the treemap with strings and locations in the array */
            for (int i = 0; i < collections.length; i++) {
                map.put(collections[i].getName(), i);
            }

            /* Workaround for auth issues in context editing */
            boolean ignore = context.ignoreAuthorization();
            context.setIgnoreAuthorization(true);

            /* Get the XML contained within the deposit */
            SwordEntry se = deposit.getSwordEntry();
            List<Element> e = se.getEntry().getElements();

            Element affiliate = e.iterator().next();  // list.head()
            if (affiliate != null) {
                List<Element> elements = affiliate.getElements();
                for (Element element : elements) {
                    String name = element.getAttributeValue("name").trim();
                    if (map.containsKey(name)) {
                        /* TODO: check for collection ownership */
                        Collection c = collections[map.get(name)];
                        if (item.isIn(c)) {
                            continue;
                        }
                        c.addItem(item);
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