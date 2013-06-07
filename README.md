Ecosur Custom Ingesters
===========================

Custom ingesters for dspace that affiliates a given item with a sequence of collections and
adds custom metadata (defined within DSpace) to the Item being deposited.

In order to use this Ingester, please do the following:

1) Add this module as a dependency of the Additions module in the main DSpace project:

    <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
        <modelVersion>4.0.0</modelVersion>
        <groupId>org.dspace.modules</groupId>
        <artifactId>additions</artifactId>
        ...
        <dependencies>
        ...
         <!-- Introduce the Affiliation ingestor -->
         <dependency>
          <groupId>mx.ecosur.infonomia.dspace</groupId>
          <artifactId>AffiliatingIngester</artifactId>
          <version>1.0-SNAPSHOT</version>
         </dependency>
        </dependencies>

2) Modify the swordv2-server.cfg conatined in the $DSPACE_HOME/config/modules directory:

change:

    plugin.single.org.dspace.sword2.SwordEntryIngester = \
      org.dspace.sword2.SimpleDCEntryIngester
to:

    plugin.single.org.dspace.sword2.SwordEntryIngester = \
      mx.ecosur.infonomia.dspace.SwordAffiliatingIngester

3) Rebuild DSpace from source and redeploy with the update goal:

    $ ant update

4) Post affilations to the server with a user with submitter authorization to both collections,
using the SE-IRI for the Item and with the following XML as part of metadata update
as defined int the Swordv2 specification:

    <?xml version="1.0"?>
    <atom:entry xmlns:atom="http://www.w3.org/2005/Atom"
        xmlns:dc="http://purl.org/dc/terms"
        xmlns:mx="http://www.ecosur.mx/swordv2">
        <mx:affiliate>
            <mx:collection name="Collection2"/>
        </mx:affiliate>
    </atom:entry>

You can list as many collections by name in the XML above as you would like to affiliate the
item with. Items will be affiliated only one time per collection (no duplicates). Non-existant
collections will simply be ignored.

5) Metadata

Custom metadata can be added to the Atom entry posted to the main SwordServer by using this
custom ingester. Simply use the dublin core xmlns to properly post data, as explained in the
SwordV2 documentation. For other kinds of metadata, such as marc, use the "element" attribute
for xml terms in order to affiliate non-parseable element names (such as numbers, "260", as in
marc). The ingester will pickup such metadata, compare the namespace against the registered
namespaces within DSpace, and add the item. 

Note: use of extended metadata requires the application of pull-request 230 to your local
DSpace installation [https://github.com/DSpace/DSpace/pull/230].

