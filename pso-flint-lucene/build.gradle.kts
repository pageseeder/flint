dependencies {

  implementation(libs.lucene.core)
  implementation(libs.lucene.misc)
  implementation(libs.lucene.analysis.common)
  implementation(libs.lucene.highlighter)
  implementation(libs.lucene.queryparser)
  implementation(libs.lucene.suggest)
  implementation(project(":pso-flint"))

}
