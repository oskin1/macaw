package com.github.oskin1.macaw

import org.scalameter.api._
import org.scalameter.picklers.Implicits._
import org.scalameter.{Gen, KeyValue}

import scala.util.Random

object TreeVectorBench extends Bench.ForkedTime {

  protected val values: Gen[IndexedSeq[Int]] = for {
    size <- Gen.enumeration("itemsQty")(1000, 4000, 16000)
  } yield (0 until size).map(_ => Random.nextInt())

  protected val config: Seq[KeyValue] = Seq[KeyValue](
    exec.minWarmupRuns -> 10,
    exec.maxWarmupRuns -> 30,
    exec.benchRuns -> 20,
    exec.requireGC -> true
  )

  performance of "TreeVector vs Vector" in {
    performance of "TreeVector.append" in {
      using(values) config(config: _*) in { vals =>
        var vec = TreeVector.empty[Int]
        vals.foreach { v =>
          vec = v +: vec
        }
      }
    }
    performance of "Vector.append" in {
      using(values) config(config: _*) in { vals =>
        var vec = Vector.empty[Int]
        vals.foreach { v =>
          vec = v +: vec
        }
      }
    }
  }

}
