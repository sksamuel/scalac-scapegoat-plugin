package com.sksamuel.scapegoat.inspections.unnecessary

import com.sksamuel.scapegoat.inspections.unneccesary.UnusedMethodParameter
import com.sksamuel.scapegoat.{Inspection, InspectionTest, Warning}
import org.scalatest.Assertion

/** @author Stephen Samuel */
class UnusedMethodParameterTest extends InspectionTest {

  override val inspections: Seq[Inspection] = Seq(new UnusedMethodParameter)

  "UnusedMethodParameter" - {
    "should report warning" - {
      "for unused parameters in concrete methods" in {
        val code = """class Test {
                     |  val initstuff = "sammy"
                     |  def foo(a: String, b: Int, c: Int) {
                     |    println(b)
                     |    foo(a, b, b)
                     |  }
                     |}""".stripMargin

        compileCodeSnippet(code)
        compiler.scapegoat.feedback.warnings.size shouldBe 1
        compiler.scapegoat.feedback.warns.size shouldBe 1
      }
    }

    "should ignore @SuppressWarnings" in {
      val code = """class Test {
                   |  @SuppressWarnings(Array("all"))
                   |  def foo(a: String, b: Int, c: Int) {
                   |    println(b)
                   |    foo(a, b, b)
                   |  }
                   |}""".stripMargin

      compileCodeSnippet(code)
      compiler.scapegoat.feedback.warnings.size shouldBe 0
    }

    "should not report warning" - {
      "for main method" in {
        val code = """class Test {
                     |  def main(args: Array[String]): Unit = {}
                     |}""".stripMargin

        compileCodeSnippet(code)
        compiler.scapegoat.feedback.warnings.size shouldBe 0
      }

      "for abstract methods" in {
        val code = """abstract class Test {
                     |  def foo(name: String): String
                     |}""".stripMargin

        compileCodeSnippet(code)
        compiler.scapegoat.feedback.warnings.size shouldBe 0
      }

      "for abstract constructor" in {
        val code = """abstract class EventBusMessage(messageVersion: Int)"""

        compileCodeSnippet(code)
        compiler.scapegoat.feedback.warnings.size shouldBe 0
      }

      "for methods not returning" in {
        val code = """class Test {
                     |  def foo(name: String) = throw new RuntimeException
                     |}""".stripMargin

        compileCodeSnippet(code)
        compiler.scapegoat.feedback.warnings.size shouldBe 0
      }

      "for methods not returning when their return type is specified" in {
        val code = """class Test {
                     |  def foo(name: String): String = throw new RuntimeException
                     |}""".stripMargin

        compileCodeSnippet(code)
        compiler.scapegoat.feedback.warnings.size shouldBe 0
      }

      "for overridden method" in {
        val code = """package com.sam
                     |trait Foo {
                     |  def foo(name: String): String
                     |}
                     |object Fool extends Foo {
                     |  override def foo(name: String): String = "sam"
                     |}""".stripMargin

        compileCodeSnippet(code)
        compiler.scapegoat.feedback.warnings.size shouldBe 0
      }

      "for overridden method without override keyword" in {
        val code = """package com.sam
                     |trait Foo {
                     |  def foo(name: String): String
                     |}
                     |object Fool extends Foo {
                     |  def foo(name: String): String = "sam"
                     |}""".stripMargin

        compileCodeSnippet(code)
        compiler.scapegoat.feedback.warnings.size shouldBe 0
      }

      "for implemented method" in {
        val code = """package com.sam
                     |trait Foo {
                     |  def foo(name: String): String
                     |}
                     |case class Fool() extends Foo {
                     |  def foo(name: String): String = "sam"
                     |}""".stripMargin

        compileCodeSnippet(code)
        compiler.scapegoat.feedback.warnings.size shouldBe 0
      }

      "for js.native defined method" in {
        val code = """package scala.scalajs {
                     |  object js {
                     |    def native: Nothing = ???
                     |  }
                     |}
                     |package com.sam {
                     |  import scalajs.js
                     |  class Foo {
                     |    def foo(name: String): String = js.native
                     |  }
                     |}""".stripMargin
        compileCodeSnippet(code)
        compiler.scapegoat.feedback.warnings.size shouldBe 0
      }
    }

    "should handle constructor params" - {
      "ignore unused case class primary param" in
      assertNoWarnings("""case class Foo(x: Int)""")

      "warn on unused case class secondary params" in {
        val code = """case class Foo(x: Int)(y: Int)"""

        compileCodeSnippet(code)
        compiler.scapegoat.feedback.warnings match {
          case Seq(warning: Warning) =>
            warning.snippet.get should include("y")
        }
      }

      "not warn on case class secondary params used as fields" in {
        assertNoWarnings("""case class Foo(x: Int)(y: Int) {
                           |  def example: String = {
                           |    s"x = $x, y = $y"
                           |  }
                           |}""".stripMargin)
      }

      "not warn on case class secondary params used as params" in {
        assertNoWarnings("""case class Foo(x: Int)(y: Int) {
                           |  println(s"x = $x, y = $y")
                           |
                           |  def example: String = "irrelevant"
                           |}""".stripMargin)
      }

      "warn on unused non-case class primary params" in {
        val code = """class Foo(x: Int)"""

        compileCodeSnippet(code)
        compiler.scapegoat.feedback.warnings match {
          case Seq(warning: Warning) =>
            warning.snippet.get should include("x")
        }
      }

      "not warn on non-case class primary params used as fields" in {
        assertNoWarnings("""class Foo(x: Int) {
                           |  def example: String = {
                           |    s"x = $x"
                           |  }
                           |}""".stripMargin)
      }

      "not warn on non-case class primary params used as params" in {
        assertNoWarnings("""class Foo(x: Int) {
                           |  println(s"x = $x")
                           |
                           |  def example: String = "irrelevant"
                           |}""".stripMargin)
      }

      "not warn on non-case class primary params marked val" in
      assertNoWarnings("""class Foo(val x: Int)""")
    }
  }

  private def assertNoWarnings(code: String): Assertion = {
    compileCodeSnippet(code)
    compiler.scapegoat.feedback.warnings.size shouldBe 0
  }
}
