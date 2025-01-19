package com.sksamuel.scapegoat.inspections.equality

import com.sksamuel.scapegoat.{Inspection, InspectionTest}

/** @author Stephen Samuel */
class ComparingUnrelatedTypesTest extends InspectionTest {

  override val inspections: Seq[Inspection] = Seq(new ComparingUnrelatedTypes)

  private def verifyNoWarnings(code: String): Unit = {
    compileCodeSnippet(code)
    compiler.scapegoat.feedback.warnings.size shouldBe 0
  }

  "ComparingUnrelatedTypes" - {
    "should report warning" - {
      "for comparing Option[Int] to Option[String]" in {
        val code = """object Main {
                        def main(args: Array[String]): Unit = {
                          val stringOption: Option[String] = Some("1")
                          val intOption: Option[Int] = Some(1)
                          stringOption == intOption
                        }
                      }""".stripMargin
        compileCodeSnippet(code)
        compiler.scapegoat.feedback.warnings.size shouldBe 1
      }
      "for different enum values" in {
        val code = """object Main {
                        def main(args: Array[String]): Unit = {
                          Enum1.Value1 == Enum2.Value1
                          Enum1.Value1 != Enum2.Value1
                        }
                      }

                      object Enum1 extends Enumeration {
                        val Value1, Value2, Value3 = Value
                      }

                      object Enum2 extends Enumeration {
                        val Value1, Value2, Value3 = Value
                      }""".stripMargin
        compileCodeSnippet(code)
        compiler.scapegoat.feedback.warnings.size shouldBe 2
      }
    }
    "should not report warning" - {
      "for zero" - {
        "compared to long" in verifyNoWarnings("""object A { val l = 100l; val b = 0 == l }""")
        "compared to double" in verifyNoWarnings("""object A { val d = 100d; val b = 0 == d }""")
        "compared to float" in verifyNoWarnings("""object A { val f = 100f; val b = 0 == f }""")
      }
      "for long" - {
        "compared to zero" in verifyNoWarnings("""object A { val l = 100l; val b = l == 0 }""")
        "compared to int literal" in verifyNoWarnings("""object A { val l = 100l; val b = l == 100 }""")
      }
      "for char" - {
        "compared to char-sized long literal" in
        verifyNoWarnings("""object A { val c = 'a'; val l = c == 97L }""")
        "compared to char-sized int literal" in
        verifyNoWarnings("""object A { val c = 'a'; val l = c == 97 }""")
      }
      "for short" - {
        "compared to Short.MaxValue as int literal" in
        verifyNoWarnings("""object A { val s = 1.toShort; val b = s == 32767 }""")
      }
      "for double" - {
        "compared to zero" in verifyNoWarnings("""object A { val d = 100d; val b = d == 0 }""")
      }
      "for float" - {
        "compared to zero" in verifyNoWarnings("""object A { val f = 100f; val b = f == 0 }""")
      }

      "for numbers types comparing to number literals" - {
        "number left hand side" in verifyNoWarnings("""object A { val a = BigDecimal(5); val b = a == 0 } """)
        "number right hand side" in verifyNoWarnings(
          """object A { val a = BigDecimal(5); val b = 0 != a } """
        )
      }

      "for same enum values" in {
        val code = """object Main {
                        def main(args: Array[String]): Unit = {
                          Enum.Value1 == Enum.Value1
                          Enum.Value1 != Enum.Value1
                        }
                      }

                      object Enum extends Enumeration {
                        val Value1, Value2, Value3 = Value
                      }""".stripMargin
        compileCodeSnippet(code)
        compiler.scapegoat.feedback.warnings.size shouldBe 0
      }
      "for isSize comparison" - {
        "equality check" in verifyNoWarnings("""object A { val b = Seq("a", "b").sizeIs == 2 }""")
        "inequality check" in verifyNoWarnings("""object A { val b = Seq("a", "b").sizeIs != 2 }""")
      }
      "singleton types" - {
        "sealed trait hierarchy match if guard" in {
          verifyNoWarnings(
            """
              |sealed trait Enum
              |object Enum {
              |  case object E1 extends Enum
              |
              |  (E1: Enum) match {
              |    case x if x == E1 =>
              |  }
              |}""".stripMargin
          )
        }
        "bound variable if guard" in
        verifyNoWarnings("""object A { System.getProperty("x") match { case s if s == "y" => } }""")
      }
    }
  }
}
