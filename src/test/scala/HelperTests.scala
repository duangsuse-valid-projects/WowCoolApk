import org.scalatest._

class HelperTests extends FlatSpec with Matchers {
  "bytesToHexString" should "convert byte array to String" in {
    val bytes = new Array[Byte](4)
    bytes{0} = 0x1f.asInstanceOf[Byte]; bytes{1} = 0x2f.asInstanceOf[Byte]; bytes{2} = 0x1; bytes{3} = 0x00
    val result = WowCoolApk.bytesToHexString(bytes)
    result shouldBe "1f2f0100"
  }
}
