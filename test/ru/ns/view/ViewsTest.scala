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
      import java.time.LocalDateTime

      val device_1: DeviceView = DeviceView("sdb2", "Acer", "1234567", "Acer 1", LocalDateTime.parse("2010-06-30T01:20"), visible = true, reliable = true, 10)
      val device_2: DeviceView = DeviceView("sdb3", "Bcer", "111-222", "Acer 2", LocalDateTime.MIN, visible = true, reliable = true, 1)

      val rowSeq = Vector[DeviceView](device_1, device_2, DeviceView("sdb1", "WD", "qwerty", "WD 4", LocalDateTime.parse("2010-06-30T01:20"), visible = true, reliable = true, 1))
      val viewRes = views.html.smr_index(rowSeq.toBuffer)()

      contentType(viewRes) mustBe "text/html"
      contentAsString(viewRes) must include("sdb2")
      contentAsString(viewRes) must include("Acer")

      contentAsString(viewRes) must include("1234567")

      contentAsString(viewRes) must include("2010")
    }
  }

}
