package com.redhat.et.testcases

import scopt.OptionParser

case class TestConfig(cases: Seq[String])

trait Testcase {
  def run: Unit
}

object Main {
  val TESTCASES = Map[String, Testcase]("hello" -> new Hello(), "spark" -> new Spark(), "geometry" -> new Geometry())

  def main(args: Array[String]) {
    val parser = new OptionParser[TestConfig]("testcases") {
      head("testcases", "0.0.1")

      help("help").text("prints this usage text")

      arg[String]("<test>...").unbounded().optional().action( (x, c) =>
	c.copy(cases = c.cases :+ x) ).text("test cases to run")

      note("valid test cases include: \n  * hello (\"hello, world\")\n  * spark (run a trivial Spark job)\n  * geometry (property-based testing of computational geometry code)\n\nif no test case is specified, run all")
    }
    
    parser.parse(args, TestConfig(Seq())) match {
      case Some(config) =>
	val cases = config.cases match {
	  case Seq() => TESTCASES.keys
	  case x => x
	}

	cases.foreach(c => TESTCASES.get(c).map(v => v.run).getOrElse(Console.println(s"unknown test case $c (use --help for info)")))
      case None => ()
    }
  }
}
