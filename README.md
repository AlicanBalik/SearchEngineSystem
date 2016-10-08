The system allows users to perform various searches(by Author, Keyword, ISBN etc...) over digital library(PDF books and articles). Users have ability to see aggregated statistics over digital library. 
Functions to add(contribute) and rate book(s) are also available.

The system is compound of three modules:
1) Console application for bulk insert of huge number of PDF books. The application first parses all PDF documents, their details and store them into database. The application is also responsible for maintaining SOLR index.
2) Web application to provide ability to search digital books, download them. It is also possible to add(contribute) books using the web application. Book rating and feedback are also supported.
3) SOLR index which allows fuzzy search over book content for elastic search similar to Google search. ( in progress)

Technologies: 
For indexing data: Apache SOLR.
Web Application: Developed by using Java Play Framework with Ajax components.
Console Application: Developed by using Java Programming Language.
Database for storing book data: Apache HBASE.
