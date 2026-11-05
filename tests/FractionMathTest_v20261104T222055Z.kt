package cc0.temporalfractions.a0he7vjet48
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
object FractionMathTest {
const val SOURCE_ID="FractionMathTest_v20261105T191008Z"
@JvmStatic fun main(args:Array<String>){
check(TimeMath.durationNs(0,2,0)==86400000000000L)
check(TimeMath.durationNs(1,2,0)==2551442876907840L)
check(TimeMath.durationNs(2,3,0)==31556972778336000L)
check(TimeMath.durationNs(0,2,4)==5400000000000L)
check(TimeMath.durationNs(0,3,2)==9600000000000L)
check(TimeMath.durationNs(1,3,1)==850480958969280L)
check(TimeMath.durationNs(0,3,4)==1066666666667L)
for(period in 0..2)for(radix in 2..3){val max=TimeMath.maxLevel(period,radix);check(TimeMath.durationNs(period,radix,max)>=1000000000L);check(TimeMath.durationNs(period,radix,max+1)<1000000000L)}
check(TimeMath.fireTimestamp(0L,ZoneId.of("UTC"))=="1970-01-01T00:00:00.000Z")
val ny=ZoneId.of("America/New_York")
check(TimeMath.fireTimestamp(Instant.parse("2026-11-01T05:30:00.125Z").toEpochMilli(),ny)=="2026-11-01T01:30:00.125-04:00")
check(TimeMath.fireTimestamp(Instant.parse("2026-11-01T06:30:00.125Z").toEpochMilli(),ny)=="2026-11-01T01:30:00.125-05:00")
check(TimeMath.fireTimestamp(Instant.parse("2026-11-05T12:34:56.789Z").toEpochMilli(),ZoneId.of("Asia/Kolkata"))=="2026-11-05T18:04:56.789+05:30")
println("Temporal Fractions fixed-duration tests passed");println("Temporal Fractions scheduled firing timestamp tests passed")
}
}
