import uk.gov.hmrc.DefaultBuildSettings

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "3.3.6"

lazy val microservice = Project("view-demo", file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test)
  .settings(scalacOptions := scalacOptions.value.diff(Seq("-Wunused:all", "-encoding", "-unchecked", "-deprecation")))
  .settings(PlayKeys.playDefaultPort := 22222)
  .settings(CodeCoverageSettings.settings)
//  .settings( scalafmtOnCompile := true)

