name := "oauthplayground"

version := "1.0"

scalaVersion := "2.11.7"

  //"org.eclipse.jetty" % "jetty-webapp" % "7.6.0.v20120127" % "container",
libraryDependencies += "javax.servlet" % "javax.servlet-api" % "3.1.0"

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.4.4"

enablePlugins(JettyPlugin)
