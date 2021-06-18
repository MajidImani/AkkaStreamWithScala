class Statistics(var minimumDependencies: Int, var compile: Double, var test: Double, var runtime: Double, var provided: Double) {

  override def toString: String = {
    //"Considered minimum number of dependencies:  " + minimumDependencies.toString +
    s"\nCompile: ${compile.toString}" +
      s"\nProvided: ${provided.toString}" +
      s"\nRuntime: ${runtime.toString}" +
      s"\nTest: ${test.toString}"
  }
}
