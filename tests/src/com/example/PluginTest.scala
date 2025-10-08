package com.example

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class PluginTest extends AnyFunSuite with Matchers {
  test("Plugin should compile successfully") {
    // Basic test to verify plugin works
    val result = TestMain.calculateSum(2, 3)
    result shouldBe 5
  }

  test("Plugin should process methods") {
    // Add more specific plugin tests here
    succeed
  }
}
