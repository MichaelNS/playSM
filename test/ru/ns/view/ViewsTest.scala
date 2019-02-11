package ru.ns.view

import models.DeviceView
import org.scalatest.BeforeAndAfter
import org.scalatestplus.play.guice.GuiceOneServerPerTest
import org.scalatestplus.play.{HtmlUnitFactory, OneBrowserPerTest, PlaySpec, ServerProvider}
import play.api.test.Helpers.{contentAsString, contentType, _}


class ViewsTest extends PlaySpec
  with OneBrowserPerTest
  with GuiceOneServerPerTest
  with HtmlUnitFactory
  with ServerProvider
  with BeforeAndAfter {

  "smr_index" should {
    "render" in {
      import org.joda.time.DateTime

      val device_1: DeviceView = DeviceView ("sdb2", "Acer", "1234567", "Acer 1", DateTime.parse("2010-06-30T01:20"), visible = true, reliable = true, 10)
      val device_2: DeviceView = DeviceView ("sdb3", "Bcer", "111-222", "Acer 2", DateTime.parse("-292275055-05-17T01:30:17.000+02:30:17"), visible = true, reliable = true, 1)

      val rowSeq = Vector[DeviceView](device_1, device_2, DeviceView("sdb1", "WD", "qwerty", "WD 4", DateTime.parse("2010-06-30T01:20"), true, true, 1))
      val viewRes = views.html.smr_index(rowSeq.toBuffer)

      contentType(viewRes) mustBe "text/html"
      contentAsString(viewRes) must include("sdb2")
      contentAsString(viewRes) must include("Acer")

      contentAsString(viewRes) must include("1234567")

      contentAsString(viewRes) must include("2010")
    }
  }

}
