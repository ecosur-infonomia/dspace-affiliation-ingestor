package mx.ecosur.infonomia.dspace;

import org.apache.abdera.model.Element;
import org.apache.abdera.model.Entry;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;
import org.dspace.core.Context;
import org.dspace.sword2.DepositResult;
import org.swordapp.server.Deposit;
import org.swordapp.server.SwordServerException;

import java.sql.SQLException;
import java.util.List;
import java.util.TreeMap;

/**
 * Extends an Item's Metadata with registered metadata pushed up in the deposit.
 */
public class ExtendCommand {

    public DepositResult execute(Context context, Deposit deposit, Item item) throws SwordServerException {
        Entry entry = deposit.getSwordEntry().getEntry();
        try {
            /* Load all custom namespaces */
            MetadataSchema[] schemas = MetadataSchema.findAll(context);
            TreeMap<String,Integer> map = new TreeMap<String,Integer>();
            for (int i = 0; i < schemas.length; i++) {
                map.put(schemas[i].getNamespace(), i);
            }
            /* Load any custom registered Schemas and check sword Entries */
            List<Element> extensions = entry.getExtensions();
            for (Element element : extensions)
            {
                if (map.get(element.getQName().getNamespaceURI()) != null)
                {
                    MetadataSchema schema = schemas [map.get(element.getQName().getNamespaceURI())];
                    String schemaName = schema.getName();
                    String field;
                    if (element.getAttributeValue("element") == null) {
                        field = element.getQName().getLocalPart();
                    } else {
                        field = element.getAttributeValue("element");
                    }
                    String qualifier = element.getAttributeValue("qualifier");
                    String value = element.getText();
                    String language = element.getLanguage();
                    if (language == null) { language = ""; }
                    item.addMetadata(schemaName, field, qualifier, language, value);
                }
            }

            item.update();
            DepositResult result = new DepositResult();
            result.setItem(item);
            result.setTreatment("Extended Item with additional Metadata.");
            return result;

        } catch (SQLException e) {
            throw new SwordServerException(e);
        } catch (AuthorizeException e) {
            throw new SwordServerException(e);
        }
    }
}
