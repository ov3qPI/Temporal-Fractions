package cc0.temporalfractions.a0he7vjet48
import java.math.BigInteger
import java.math.RoundingMode
import java.util.Locale
object TimeMath {
const val SOURCE_ID="TimeMath_v20261104T222055Z"
val names=arrayOf("Day","Mean synodic month","December-solstice year")
val nanos=arrayOf("86400000000000","2551442876907840","31556972778336000").map { BigInteger(it) }
fun denominator(radix:Int,level:Int):BigInteger=BigInteger.valueOf(radix.toLong()).pow(level)
fun durationNs(period:Int,radix:Int,level:Int):Long {
require(period in 0..2 && radix in 2..3 && level>=0 && level<=64)
val pair=nanos[period].divideAndRemainder(denominator(radix,level))
val q=pair[0].add(if(pair[1].shiftLeft(1)>=denominator(radix,level)) BigInteger.ONE else BigInteger.ZERO)
return q.longValueExact()
}
fun maxLevel(period:Int,radix:Int):Int { var n=0;while(n<64&&durationNs(period,radix,n+1)>=1_000_000_000L)n++;return n }
fun millisCeil(ns:Long):Long=ns/1_000_000L+if(ns%1_000_000L>0)1L else 0L
fun durationLabel(ns:Long):String {
val seconds=ns/1_000_000_000L;val days=seconds/86400;val hours=seconds/3600%24;val minutes=seconds/60%60;val sec=seconds%60;val fractional=ns%1_000_000_000L
val whole=buildString { if(days>0)append("${days}d ");if(hours>0||days>0)append("${hours}h ");if(minutes>0||hours>0||days>0)append("${minutes}m ");append(sec) }
val decimal=if(fractional==0L)"" else "."+String.format(Locale.US,"%09d",fractional).trimEnd('0')
return "$whole${decimal}s"
}
fun fraction(radix:Int,level:Int):String=if(level==0)"1" else "1/${denominator(radix,level)}"
fun parse(period:Int,radix:Int,level:Int):String="${names[period]} · ${fraction(radix,level)} · ${durationLabel(durationNs(period,radix,level))}"
}
