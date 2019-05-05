import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import java.text.SimpleDateFormat;
import java.text.ParseException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

object WowCoolApk {
  val TOKEN = "token://com.coolapk.market/c67ef5943784d09750dcfbb31020f0ab?";
  lazy val DEFAULT_DEVICE_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
  val PACKAGE = "com.coolapk.market";

  lazy val MD5 = MessageDigest.getInstance("MD5"); // unchecked
  lazy val DATE = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")

  val NAME = "WowCoolApk"
  var flags: String = ""

  /**
   * Given a array of Byte, return join result of format %02x<p>
   * bytes -> 7f454c46...
   */
  def bytesToHexString(bs : Array[Byte]):
  String = {
    val sb = new StringBuilder
    for (b <- bs) sb.append("%02x".format(b))
    return sb.toString()
  }

  /**
   * Given a array of Byte (to compute md5sum)<p>
   * Return hexdigest of this byteary
   */
  def toMd5String(dat : Array[Byte]):
  String = {
    val bytes = MD5.digest(dat)
    return bytesToHexString(bytes)
  }

  /**
   * Encode bytebuffer to String with Base64
   */
  def toBase64(dat : Array[Byte]):
  String = {
    return new String(Base64.getEncoder().encode(dat))
  }

  /**
   * Get CoolApk's timestamp MD5 hexdigest string (digest for String.valueof(unixTime))
   */
  def timestampMd5(unixTime: Long):
  String = {
    val ut_repr = String.valueOf(unixTime)
    return toMd5String(ut_repr.getBytes())
  }

  /**
   * Generate api.CoolApk.com token with X-App-Id, generation time, Device UUID information
   * <p> if uuid is None, then use  default value (in parameter list)
   */
  def getCoolToken(apk: String = PACKAGE, timeAt: Date = new Date, uuid: Option[UUID] = Some(DEFAULT_DEVICE_UUID)):
  String = {
    val verb = mkVerb(flags)
    verb(s"Token=$TOKEN; uuid=$uuid; apk=$apk")
    val timestamp = timeAt.getTime() / 1000; verb(s"TimeStamp: $timestamp")
    val salt = TOKEN + timestampMd5(timestamp) + "$" + uuid + "&" + apk
    verb(s"Salt: $salt")

    val stringBytes = (s: String) => s.getBytes()
    val toBase64AndMD5Digest = toMd5String _ compose stringBytes compose toBase64

    val salt_b64_md5 = toBase64AndMD5Digest(salt.getBytes())
    verb(s"toBase64AndThenMD5Digest: $salt_b64_md5")

    val utime_hex = "0x%x".format(timestamp); verb(s"Hex time: $utime_hex")
    return salt_b64_md5 + uuid.getOrElse(DEFAULT_DEVICE_UUID).toString + utime_hex;
  }

  def noisyS(f : (String) => Unit) = (p : Boolean, msg : String) => if (p) f(msg)
  val noisyln = noisyS(println).curried

  def isM(m: String) = m.equals _
  def not(p: Boolean) = !p
  def isM_2(m: String, m1: String) = (s: String) => isM(m)(s) || isM(m1)(s)

  val mkNote = (isM_2("", "v") andThen noisyln)
  val mkVerb = (noisyln compose isM("v"))
  val emerg = (noisyln compose (isM("q") andThen not))

  val _parse_failed = (str: String) => { emerg(flags)(s"Failed to parse date `$str`"); new Date }
  def parseDate(str : String): Date = try {
    DATE.parse(str) match {
      case null => _parse_failed(str)
      case date => return date
    }
  } catch {
    case ex: ParseException => _parse_failed(str)
  }

  def tryParseUUID(str : String): Option[UUID] = try { return Some(UUID.fromString(str)) } catch { case _: IllegalArgumentException => None }

  def cmd(args : List[String], flag: String): Unit
  = {
    this.flags = flag
    val note = mkNote(flags)
    args match {
      case List("!") => note(getCoolToken())
      case List(uuid) => note(getCoolToken(uuid = tryParseUUID(uuid)))
      case List(uuid, date) => note(getCoolToken(timeAt = parseDate(date), uuid = tryParseUUID(uuid)))
      case List(uuid, date, apk) => note(getCoolToken(apk, parseDate(date), tryParseUUID(uuid)))
      case _ => {
        eprintln(s"Bad arity (flags = $flags)")
        eprintln(s"Unknown command `${args.mkString(" ")}`; use $NAME help to show help")
      }
    }
  }

  val eprintln = System.err.println (_: String)
  def main(args : Array[String]): Unit
  = args.toList match {
    case List() => eprintln(s"Run $NAME help to show help")
    case List("get" | "gen") => println(getCoolToken())
    case List("help" | "-h" | "-help" | "--help") => {
      val w = println (_ : String)
      val wn = (m: String) => { print(NAME); print(' '); println(m) }
      w(s"Generate X-App-Token HTTP request header field for $PACKAGE")
      w("Usage:")
      wn("<help -help -h --help> Get help")
      wn("<uuid> ")
      wn("<uuid> <date>")
      wn("<uuid> <date> <apk>")
      wn("<flag: _ or q or v> rest... [! to use defaults]")
      wn("<get gen> generate one with default options:\n" +
          s" 0*-0*-0*-0*-0* as UUID,\n $PACKAGE as market,\n current time as time")
    }
    case "_" :: xs => cmd(xs, "")
    case "q" :: xs => cmd(xs, "q")
    case "v" :: xs => cmd(xs, "v")
    case xs => cmd(xs, "")
  }
}

