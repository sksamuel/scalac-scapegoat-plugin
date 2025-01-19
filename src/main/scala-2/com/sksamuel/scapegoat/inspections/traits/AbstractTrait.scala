package com.sksamuel.scapegoat.inspections.traits

import com.sksamuel.scapegoat._

class AbstractTrait
    extends Inspection(
      text = "Use of abstract trait",
      defaultLevel = Levels.Info,
      description = "Traits are automatically abstract.",
      explanation =
        "The abstract modifier is used in class definitions. It is redundant for traits, and mandatory for all other classes which have incomplete members."
    ) {

  override def inspector(ctx: InspectionContext): Inspector = {
    new Inspector(ctx) {
      override def postTyperTraverser: context.Traverser =
        new context.Traverser {

          import context.global._

          def isAbstractTrait(positions: Map[Long, Position]): Boolean =
            positions.contains(Flag.TRAIT) && positions.contains(Flag.ABSTRACT)

          override def inspect(tree: Tree): Unit = {
            tree match {
              // I use positions, because all traits are abstract by default
              case ClassDef(mods, _, _, _) if isAbstractTrait(mods.positions) =>
                context.warn(tree.pos, self, tree.toString.take(500))
              case _ => continue(tree)
            }
          }
        }
    }
  }
}
