package com.sksamuel.scapegoat.inspections

import com.sksamuel.scapegoat.{Inspection, InspectionContext, Inspector, Levels}

class AvoidRequire
    extends Inspection(
      text = "Use of require",
      defaultLevel = Levels.Warning,
      description = "Use require in code.",
      explanation = "Using require throws an untyped Exception."
    ) {

  def inspector(ctx: InspectionContext): Inspector =
    new Inspector(ctx) {
      override def postTyperTraverser: context.Traverser =
        new context.Traverser {

          import context.global._

          override def inspect(tree: Tree): Unit = {
            tree match {
              case Select(lhs, TermName("require")) if lhs.tpe.typeSymbol.fullName == "scala.Predef" =>
                context.warn(tree.pos, self, tree.toString.take(200))
              case _ =>
                continue(tree)
            }
          }
        }
    }

}
