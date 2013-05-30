AffiliatingIngester
===========================

An ingester for dspace that affiliates a given item with a sequence of collections.

In order to use this Ingester, please do the following:

1) Add this module as a dependency of the Additions module in the main DSpace project:

<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
   <modelVersion>4.0.0</modelVersion>
   <groupId>org.dspace.modules</groupId>
   <artifactId>additions</artifactId>

   ....

   <dependencies>
    ...
   <!-- Introduce the Affiliation ingestor -->
     <dependency>
         <groupId>mx.ecosur.infonomia.dspace</groupId>
         <artifactId>AfiliatingIngester</artifactId>
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

3) Rebuild DSpace from source and redploy with the update goal:

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
item with. Items will be affiliated only one time per collection (no duplicates).