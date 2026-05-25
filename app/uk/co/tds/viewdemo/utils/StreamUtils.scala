/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.co.tds.viewdemo.utils

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Flow

object StreamUtils {
  def ungrouper[A]: Flow[List[A], A, NotUsed] = Flow[List[A]].mapConcat(identity)

  def grouper[A](groupSize: Int): Flow[A, List[A], NotUsed] = Flow[A].grouped(groupSize).map(_.toList)

  def regrouper[A](groupSize: Int): Flow[List[A], List[A], NotUsed] = ungrouper[A].via(grouper[A](groupSize))
}
