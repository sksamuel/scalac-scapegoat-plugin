package com.sksamuel.scapegoat.inspections.unsafe

import com.sksamuel.scapegoat._

/**
 * @author
 *   Stephen Samuel
 */
class IsInstanceOf
    extends Inspection(
      text = "Use of isInstanceOf",
      defaultLevel = Levels.Warning,
      description = "Checks for use of isInstanceOf.",
      explanation =
        "Use of isInstanceOf is considered a bad practice - consider using pattern matching instead."
    ) {

  def inspector(ctx: InspectionContext): Inspector =
    new Inspector(ctx) {
      override def postTyperTraverser: context.Traverser =
        new context.Traverser {

          import context.global._

          override def inspect(tree: Tree): Unit = {
            tree match {
              case TypeApply(Select(_, TermName("isInstanceOf")), _) =>
                context.warn(tree.pos, self, tree.toString.take(500))
              case DefDef(modifiers, _, _, _, _, _) if modifiers.hasFlag(Flag.SYNTHETIC) => // avoid partial function stuff
              case Match(_, cases) => // ignore selector and process cases
                cases.foreach(traverse)
              case _ => continue(tree)
            }
          }
        }
    }
}
