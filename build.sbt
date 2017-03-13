name := "scalatests"

organization := "com.redhat.et"

version := "0.0.1"

scalaVersion := "2.10.6"

val SPARK_VERSION = "1.6.0"
val SCALA_VERSION = "2.10.6"

def commonSettings = Seq(
  libraryDependencies ++= Seq(
    "com.github.scopt" %% "scopt" % "3.5.0",
    "org.apache.spark" %% "spark-core" % SPARK_VERSION,
    "org.apache.spark" %% "spark-sql" % SPARK_VERSION,
    "org.apache.spark" %% "spark-mllib" % SPARK_VERSION,
    "org.scala-lang" % "scala-reflect" % SCALA_VERSION,
    "org.slf4j" % "slf4j-nop" % "1.7.6",
    "org.scalacheck" %% "scalacheck" % "1.11.3"
  )
)

seq(commonSettings:_*)

licenses += ("Apache-2.0", url("http://opensource.org/licenses/Apache-2.0"))

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

scalacOptions in (Compile, doc) ++= Seq("-doc-root-content", baseDirectory.value+"/root-doc.txt")

(dependencyClasspath in Compile) <<= (dependencyClasspath in Compile).map(
  _.filterNot(_.data.name.contains("slf4j-log4j12"))
)

lazy val testcases = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    mainClass in assembly := Some("com.redhat.et.testcases.Main"),
    assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false),
    assemblyMergeStrategy in assembly := { 
      case m if m.toLowerCase.endsWith("manifest.mf")          => MergeStrategy.discard
      case m if m.toLowerCase.matches("meta-inf.*\\.sf$")      => MergeStrategy.discard
      case "log4j.properties"                                  => MergeStrategy.discard
      case m if m.toLowerCase.startsWith("meta-inf/services/") => MergeStrategy.filterDistinctLines
      case "reference.conf"                                    => MergeStrategy.concat
      case _                                                   => MergeStrategy.first
    }
  )
