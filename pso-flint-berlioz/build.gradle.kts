
dependencies {

  implementation(libs.berlioz)
  implementation(libs.lucene.core)
  implementation(libs.lucene.queryparser)
  implementation(libs.lucene.analysis.common)
  implementation(project(":pso-flint"))
  implementation(project (":pso-flint-lucene"))

  compileOnly(libs.servlet.api)

}
