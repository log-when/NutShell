package formal

import chisel3._
import chiseltest._
import chiseltest.formal._
import org.scalatest.flatspec.AnyFlatSpec

import nutcore.{NutCore, NutCoreConfig}
import top._

class NutCoreSpec extends AnyFlatSpec with Formal with ChiselScalatestTester {
  behavior of "MyModule"
  it should "pass" in {
    // config
    val s = (FormalSettings()) ++ (InOrderSettings())
    s.foreach { Settings.settings += _ }
    Settings.settings.toList.sortBy(_._1)(Ordering.String).foreach {
      case (f, v: Long) =>
        println(f + " = 0x" + v.toHexString)
      case (f, v) =>
        println(f + " = " + v)
    }
    // test
    test(new NutCore()(NutCoreConfig())) { c => }

    // verify
    verify(new NutCore()(NutCoreConfig()), Seq(BoundedCheck(3)))
  }
}
