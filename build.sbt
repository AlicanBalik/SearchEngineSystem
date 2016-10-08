name := "SearchEngine"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "javax.inject" % "javax.inject" % "1",
  "org.webjars" % "webjars-play" % "2.1.0",
  "org.webjars" % "bootstrap" % "2.3.2" exclude("org.webjars", "jquery"),
  "org.webjars" % "jquery" % "1.8.3",
  "org.apache.hadoop" % "hadoop-core" % "1.2.1",
  "org.apache.hbase" % "hbase" % "0.94.27",
  "org.slf4j" % "slf4j-log4j12" % "1.7.5",
  "org.apache.solr" % "solr-solrj" % "5.5.0",
  "org.apache.pdfbox" % "pdfbox-app" % "2.0.0",
  "org.apache.pdfbox" % "fontbox" % "2.0.0",
  "org.apache.commons" % "commons-email" % "1.4",
  javaJdbc,
  javaEbean,
  cache
)     

play.Project.playJavaSettings
