import sbt.*

object AppDependencies {

  private val bootstrapVersion = "10.7.0"
  private val hmrcMongoVersion = "2.12.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-backend-play-30" % bootstrapVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"        % hmrcMongoVersion
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-test-play-30"  % bootstrapVersion % Test,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-30" % hmrcMongoVersion % Test,
    // not scoped to Test only to allow data generators to be used in test only endpoints
    "org.scalatestplus" %% "scalacheck-1-17" % "3.2.18.0"
  )

  val it: Seq[ModuleID] = Seq.empty
}
