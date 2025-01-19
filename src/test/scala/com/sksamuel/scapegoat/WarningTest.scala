package com.sksamuel.scapegoat

import org.scalatest.freespec.AnyFreeSpec

class WarningTest extends AnyFreeSpec {

  private val warning = com.sksamuel.scapegoat.Warning(
    "text",
    1,
    Levels.Info,
    "sourceFileFull",
    "sourceFileNormalized",
    None,
    "explanation",
    "inspection"
  )

  "Warning" - {
    "hasMinimalLevelOf" - {
      "info >= info" in
      assert(warning.hasMinimalLevelOf(Levels.Info) === true)
      "info < warning" in
      assert(warning.hasMinimalLevelOf(Levels.Warning) === false)
      "info < error" in
      assert(warning.hasMinimalLevelOf(Levels.Error) === false)
      "error > warning" in
      assert(warning.copy(level = Levels.Error).hasMinimalLevelOf(Levels.Warning) === true)
    }
  }

}
