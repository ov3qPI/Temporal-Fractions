package cc0.temporalfractions.a0he7vjet48
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
object FractionMathTest {
const val SOURCE_ID="FractionMathTest_v20261105T175418Z"
@JvmStatic fun main(args:Array<String>){
check(TimeMath.durationNs(0,2,0)==86400000000000L)
check(TimeMath.durationNs(1,2,0)==2551442876907840L)
check(TimeMath.durationNs(2,3,0)==31556972778336000L)
check(TimeMath.durationNs(0,2,4)==5400000000000L)
check(TimeMath.durationNs(0,3,2)==9600000000000L)
check(TimeMath.durationNs(1,3,1)==850480958969280L)
check(TimeMath.durationNs(0,3,4)==1066666666667L)
for(period in 0..2)for(radix in 2..3){val max=TimeMath.maxLevel(period,radix);check(TimeMath.durationNs(period,radix,max)>=1000000000L);check(TimeMath.durationNs(period,radix,max+1)<1000000000L)}
check(TimeMath.fireTimestamp(0L,ZoneId.of("UTC"),Locale.US)==Pair("Thu, Jan 1, 1970","00:00:00.000 UTC (Z)"))
val ny=ZoneId.of("America/New_York")
check(TimeMath.fireTimestamp(Instant.parse("2026-11-01T05:30:00.125Z").toEpochMilli(),ny,Locale.US)==Pair("Sun, Nov 1, 2026","01:30:00.125 EDT (-04:00)"))
check(TimeMath.fireTimestamp(Instant.parse("2026-11-01T06:30:00.125Z").toEpochMilli(),ny,Locale.US)==Pair("Sun, Nov 1, 2026","01:30:00.125 EST (-05:00)"))
println("Temporal Fractions fixed-duration tests passed");println("Temporal Fractions scheduled firing timestamp tests passed")
}
}
