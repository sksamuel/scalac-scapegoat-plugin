name := "scalac-scapegoat-plugin"
organization := "com.sksamuel.scapegoat"
description := "Scala compiler plugin for static code analysis"
homepage := Some(url("https://github.com/sksamuel/scapegoat"))
licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))
scmInfo := Some(
  ScmInfo(
    url("https://github.com/sksamuel/scapegoat"),
    "scm:git@github.com:sksamuel/scapegoat.git",
    Some("scm:git@github.com:sksamuel/scapegoat.git")
  )
)
developers := List(
  Developer(
    "sksamuel",
    "sksamuel",
    "@sksamuel",
    url("https://github.com/sksamuel")
  )
)

scalaVersion := "3.6.2"
crossScalaVersions := Seq("2.12.19", "2.12.20", "2.13.15", "2.13.16", "3.3.4", "3.6.2")
autoScalaLibrary := false
crossVersion := CrossVersion.full
crossTarget := {
  // workaround for https://github.com/sbt/sbt/issues/5097
  target.value / s"scala-${scalaVersion.value}"
}
versionScheme := Some("early-semver")
semanticdbEnabled := (scalaBinaryVersion.value == "3")
scalafixConfig := Some(file(if (scalaBinaryVersion.value == "3") ".scalafix.conf" else ".scalafix-2.conf"))

// https://github.com/sksamuel/scapegoat/issues/298
ThisBuild / useCoursier := false

val scala2Options = Seq(
  "-Xlint",
  "-Xlint:adapted-args",
  "-Xlint:nullary-unit"
)

val scalac13Options = scala2Options ++ Seq(
  "-Xlint:inaccessible",
  "-Xlint:infer-any",
  "-Xlint:strict-unsealed-patmat",
  "-Yrangepos",
  "-Ywarn-unused",
  "-Xsource:3"
)
val scalac12Options = scala2Options ++ Seq(
  "-Ywarn-inaccessible",
  "-Ywarn-infer-any",
  "-Xlint:nullary-override",
  "-Xmax-classfile-name",
  "254"
)
val scala3Options = Seq(
  "-Wsafe-init",
  "-Wnonunit-statement",
  "-Wunused:all",
  "-Wvalue-discard",
  // Unused locals seem to wrong on Scala XML syntax
  "-Wconf:msg=unused value of type scala.xml.NodeBuffer:silent"
)

scalacOptions := {
  val common = Seq(
    "-unchecked",
    "-deprecation",
    "-feature",
    "-encoding",
    "utf8"
  )
  common ++ (scalaBinaryVersion.value match {
    case "2.12" => scalac12Options
    case "2.13" => scalac13Options
    case "3"    => scala3Options
  })
}
javacOptions ++= Seq("-source", "1.8", "-target", "1.8")

// because that's where "PluginRunner" is
Compile / console / fullClasspath ++= (Test / fullClasspath).value
console / initialCommands := s"""
import com.sksamuel.scapegoat._
def check(code: String) = {
  val runner = new PluginRunner { val inspections = ScapegoatConfig.inspections }
  // Not sufficient for reuse, not sure why.
  // runner.reporter.reset
  val c = runner compileCodeSnippet code
  val feedback = c.scapegoat.feedback
  feedback.warnings map (x => "%-40s %s %s".format(x.text, x.explanation, x.snippet.getOrElse(""))) foreach println
  feedback
}
"""

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % "2.3.0" excludeAll ExclusionRule(organization = "org.scala-lang"),
  "org.scala-lang.modules" %% "scala-collection-compat" % "2.12.0" excludeAll ExclusionRule(organization =
    "org.scala-lang"
  ),
  "org.scalatest" %% "scalatest"    % "3.2.19"  % "test",
  "org.mockito"    % "mockito-all"  % "1.10.19" % "test",
  "joda-time"      % "joda-time"    % "2.13.0"  % "test",
  "org.joda"       % "joda-convert" % "3.0.1"   % "test",
  "org.slf4j"      % "slf4j-api"    % "2.0.16"  % "test"
)

libraryDependencies ++= (if (scalaBinaryVersion.value == "3") {
                           Seq(
                             "org.scala-lang" %% "scala3-compiler" % scalaVersion.value % "provided",
                             "org.scala-lang" %% "scala3-compiler" % scalaVersion.value % "test"
                           )
                         } else {
                           Seq(
                             "org.scala-lang" % "scala-reflect"  % scalaVersion.value % "provided",
                             "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided",
                             "org.scala-lang" % "scala-compiler" % scalaVersion.value % "test",
                             compilerPlugin(
                               "org.scalameta" % "semanticdb-scalac" % "4.12.6" cross CrossVersion.full
                             )
                           )
                         })

// Test
Test / run / fork := true
Test / logBuffered := false
Test / parallelExecution := false

// ScalaTest reporter config:
// -o - standard output,
// D - show all durations,
// T - show reminder of failed and cancelled tests with short stack traces,
// F - show full stack traces.
Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oDTF")

// Assembly
// include the scala xml and compat modules into the final jar, shaded
assembly / assemblyShadeRules := Seq(
  ShadeRule.rename("scala.xml.**" -> "scapegoat.xml.@1").inAll,
  ShadeRule.rename("scala.collection.compat.**" -> "scapegoat.compat.@1").inAll,
  // scala-collection-compat has classes outside of the previous shade path, move them as well.
  ShadeRule.rename("scala.util.control.compat.**" -> "scapegoat.util.@1").inAll
)
Compile / packageBin := crossTarget.value / (assembly / assemblyJarName).value
makePom := makePom.dependsOn(assembly).value
assembly / test := {} // do not run tests during assembly
Test / publishArtifact := false

// Scalafix
ThisBuild / scalafixDependencies += "com.nequissimus" %% "sort-imports" % "0.6.1"
addCommandAlias("fix", "all Compile / scalafix Test / scalafix; fixImports")
addCommandAlias("fixImports", "Compile / scalafix SortImports; Test / scalafix SortImports")
addCommandAlias("fixCheck", "Compile / scalafix --check; Test / scalafix --check; fixCheckImports")
addCommandAlias(
  "fixCheckImports",
  "Compile / scalafix --check SortImports; Test / scalafix --check SortImports"
)

// Scalafmt
ThisBuild / scalafmtOnCompile := sys.env.get("GITHUB_ACTIONS").forall(_.toLowerCase == "false")
