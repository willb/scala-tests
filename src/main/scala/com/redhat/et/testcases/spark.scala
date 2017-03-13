package com.redhat.et.testcases

import org.apache.spark.{SparkConf, SparkContext}

class Spark extends Testcase {
  /* run a trivial job with Spark */

  def run {
    val ctx = new SparkContext("local[*]", "testcase!", new SparkConf())
    ctx.setLogLevel("OFF")

    val rdd = ctx.parallelize((1 to 1000).toSeq)
    val ct = rdd.flatMap(x => Range(0, x * 50)).count()

    Console.println(ct)

    ctx.stop()
  }
}

