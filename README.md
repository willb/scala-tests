# scala-tests

Some simple smoke tests for Scala 2.10 in Fedora/etc.

Currently includes "Hello, world!", a trivial Spark application, and property-based testing of a small computational geometry library.

## building

`./sbt assembly`

## running

(on Fedora or similar system where xmvn is available):

`java -cp $(build-classpath scala):target/scala-2.10/scalatests-assembly-0.0.1.jar com.redhat.et.testcases.Main`

## running a subset of tests

`java -cp $(build-classpath scala):target/scala-2.10/scalatests-assembly-0.0.1.jar com.redhat.et.testcases.Main hello geometry`
