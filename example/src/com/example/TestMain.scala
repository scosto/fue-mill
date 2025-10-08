package com.example

object TestMain {

  def main(args: Array[String]): Unit = {
    println("Testing compiler plugin...")

    val result = calculateSum(5, 10)
    println(s"Result: $result")
  }

  def calculateSum(a: Int, b: Int): Int = {
    val count = 0 // Test: Variable mit 'c'
    val xcc = 6
    val ccc = 777
    val xc = 5
    val c = 3333

    val sum = a + b
    sum
  }
}
