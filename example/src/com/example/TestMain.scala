package com.example

object TestMain {

  def main(args: Array[String]): Unit = {
    println("Testing compiler plugin...")

    val result = calculateSum(5, 10)
    println(s"Result: $result")
  }

  def calculateSum(a: Int, b: Int): Int = {
    val xcount = 0 // Test: Variable mit 'c'
    val xcc = 6
    val xccc = 777
    val xc2 = 5
    val xc = 3333
    val xc3 = 37
    val xc444 = 444
    

    val sum = a + b
    sum
  }
}
