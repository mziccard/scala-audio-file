name := """scala-audio-file"""

organization := "me.mziccard"

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

javacOptions ++= Seq("-source", "1.7", "-target", "1.7")

scalacOptions ++= Seq("-encoding", "ISO-8859-1")

javacOptions ++= Seq("-encoding", "ISO-8859-1")

scalaVersion := "2.10.4"

crossScalaVersions := Seq("2.10.4", "2.11.4")

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "org.scalamock" %% "scalamock-scalatest-support" % "3.2" % "test"
)

com.typesafe.sbt.SbtGit.versionWithGit

unmanagedSourceDirectories in Compile += (baseDirectory / "lib/jwave/src").value

excludeFilter in unmanagedSources in Compile := 
  HiddenFileFilter || "*Test.java" || "*JWave.java"

fork in run := true
