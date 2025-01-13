package com.sksamuel.scapegoat.inspections.option

import com.sksamuel.scapegoat.{InspectionTest, Levels}

class OptionGetTest extends InspectionTest(classOf[OptionGet]) {

  "option.get use" - {
    "should report warning" in {

      val code = """class Test {
                      val o = Option("sammy")
                      o.get
                    } """.stripMargin

      val feedback = runner.compileCodeSnippet(code)
      feedback.errors.assertable shouldEqual Seq(
        warning(2, Levels.Error, Some("o.get"))
      ).assertable
    }
  }

}
