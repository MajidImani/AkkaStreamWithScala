class LibraryDependencies(var library: String, var dependencyType: String
                          , var compile: Int, var test: Int, var runtime: Int, var provided: Int) {


  override def toString: String = {
    s"${library} --> " +
      s"Comile: ${compile.toString} " +
      s"Provided: ${provided.toString} " +
      s"Runtime: ${runtime.toString} " +
      s"Test: ${test.toString}"
  };

  def sumOfDependencies: Int = test + compile + runtime + provided

}
