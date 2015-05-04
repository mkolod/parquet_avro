import sbt.Keys._
import sbt._
import sbtassembly.AssemblyPlugin.autoImport._
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
    parallelExecution := true,
    parallelExecution in Test := false,
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
    ),
    shellPrompt <<= name(name => { state: State =>
      object devnull extends ProcessLogger {
        def info(s: => String) {}
        def error(s: => String) {}
        def buffer[T](f: => T): T = f
      }
      val current = """\*\s+(\w+)""".r
      def gitBranches = ("git branch --no-color" lines_! devnull mkString)
      "git %s/project %s>" format (
        current findFirstMatchIn gitBranches map (_.group(1)) getOrElse "-",
        name
      )
    }),
    test in assembly := {}
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
  ).aggregate(
    macros,
    parquetAvroExamples
  )

  lazy val parquetAvroSchema: Project = Project(
    "parquet-avro-schema",
    file("parquet-avro-schema"),
    settings = buildSettings ++ sbtavro.SbtAvro.avroSettings
  )

  lazy val parquetAvroExamples: Project = Project(
    "parquet-avro-examples",
    file("parquet-avro-examples"),
    settings = buildSettings ++ Seq(
      libraryDependencies ++=  Seq (
        "org.scalatest" %% "scalatest" % "2.2.1" % "test",
        "com.twitter" % "parquet-avro" % "1.6.0rc7",
        "org.apache.avro" % "avro" % "1.7.7",
        "org.apache.spark"  %%  "spark-assembly"  %  "1.1.1"
      )
    )
  ).dependsOn(
    macros,
    parquetAvroSchema
  )
}
