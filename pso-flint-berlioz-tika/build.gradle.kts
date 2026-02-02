dependencies {

  implementation(project(":pso-flint"))
  implementation(project(":pso-flint-berlioz"))
  implementation(libs.berlioz)
  implementation(libs.lucene.core)
  implementation(libs.lucene.queryparser)
  implementation(libs.lucene.analysis.common)
  implementation(libs.tika.core)
  implementation(libs.tika.parsers)

}
