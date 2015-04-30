import sbt._
import Keys._
import xerial.sbt.Sonatype
import xerial.sbt.Sonatype.SonatypeKeys._

object BuildSettings {

  lazy val parquetVer = "1.6.0rc7"
  lazy val avroVer = "1.7.6"

  val buildSettings = Defaults.defaultSettings ++ Sonatype.sonatypeSettings ++ Seq(
    organization       := "com.nitro",
    profileName        := "com.nitro",
    version            := "0.1",
    scalaVersion       := "2.10.4",
    crossScalaVersions := Seq("2.10.4", "2.11.5"),
    scalacOptions      ++= Seq("-optimise", "-unchecked", "-deprecation"),
    javacOptions       ++= Seq("-source", "1.7", "-target", "1.7"),
    javaOptions        ++= Seq("-Xmx2G", "-Xms256M"),
    pollInterval := 1000,
    fork := true,
    fork in Test := false,
    resolvers ++= Seq("Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
      "Maven Central" at "http://repo1.maven.org",
      "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
      "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases/"
    ),
    libraryDependencies ++= Seq(
      "com.twitter" % "parquet-column" % parquetVer,
      "com.twitter" % "parquet-hadoop" % parquetVer,
      "com.twitter" % "parquet-column" % parquetVer,
      "org.apache.avro" % "avro" % avroVer,
      "org.apache.avro" % "avro-compiler" % avroVer,
      "org.apache.commons" % "commons-io" % "1.3.2"
    )
  )
}

object ParquetAvroExtraBuild extends Build {
  import BuildSettings._

  lazy val macrosSha1 = "11bbf6344f255bf7c3b5236765e298a3a644f579"

  lazy val macros = ProjectRef(uri(s"git://github.com/nevillelyh/parquet-avro-extra.git#$macrosSha1"), "parquet-avro-extra")

  lazy val root: Project = Project(
    "root",
    file("."),
    settings = buildSettings ++ Seq(
      run <<= run in Compile in parquetAvroExamples)
  ).settings(
    publish         := {},
    publishLocal    := {}
  ).aggregate(
    parquetAvroExtra, // macros
    parquetAvroExamples
  )

  lazy val parquetAvroExtra: Project = Project(
    "parquet-avro-extra",
    file("parquet-avro-extra"),
    settings = buildSettings ++ Seq(
      libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-reflect" % _),
      libraryDependencies ++= Seq(
        "org.apache.avro" % "avro" % "1.7.4",
        "org.apache.avro" % "avro-compiler" % "1.7.4",
        "com.twitter" % "parquet-column" % "1.6.0rc4"
      ),
      libraryDependencies := {
        CrossVersion.partialVersion(scalaVersion.value) match {
          // if Scala 2.11+ is used, quasiquotes are available in the standard distribution
          case Some((2, scalaMajor)) if scalaMajor >= 11 =>
            libraryDependencies.value
          // in Scala 2.10, quasiquotes are provided by macro paradise
          case Some((2, 10)) =>
            libraryDependencies.value ++ Seq(
              compilerPlugin("org.scalamacros" % "paradise" % "2.0.0" cross CrossVersion.full),
              "org.scalamacros" %% "quasiquotes" % "2.0.0" cross CrossVersion.binary)
        }
      }
    )
  )

  lazy val parquetAvroSchema: Project = Project(
    "parquet-avro-schema",
    file("parquet-avro-schema"),
    settings = buildSettings ++ sbtavro.SbtAvro.avroSettings
  ).settings(
    publish := {},
    publishLocal := {}
  )

  lazy val parquetAvroExamples: Project = Project(
    "parquet-avro-examples",
    file("parquet-avro-examples"),
    settings = buildSettings ++ Seq(
      libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.1" % "test"
    )
  ).settings(
    publish         := {},
    publishLocal    := {}
  ).dependsOn(
    parquetAvroExtra, // macros
    parquetAvroSchema
  )
}
