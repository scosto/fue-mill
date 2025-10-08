package com.example

object TestMain {

  def main(args: Array[String]): Unit = {
    println("Testing compiler plugin...")

    val result = calculateSum(5, 10)
    println(s"Result: $result")
  }

  def calculateSum(a: Int, b: Int): Int = {
    val sum = a + b
    sum
  }
}
