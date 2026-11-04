package cc0.temporalfractions.a0he7vjet48
object FractionMathTest {
const val SOURCE_ID="FractionMathTest_v20261104T222055Z"
@JvmStatic fun main(args:Array<String>){
check(TimeMath.durationNs(0,2,0)==86400000000000L)
check(TimeMath.durationNs(1,2,0)==2551442876907840L)
check(TimeMath.durationNs(2,3,0)==31556972778336000L)
check(TimeMath.durationNs(0,2,4)==5400000000000L)
check(TimeMath.durationNs(0,3,2)==9600000000000L)
check(TimeMath.durationNs(1,3,1)==850480958969280L)
check(TimeMath.durationNs(0,3,4)==1066666666667L)
for(period in 0..2)for(radix in 2..3){val max=TimeMath.maxLevel(period,radix);check(TimeMath.durationNs(period,radix,max)>=1000000000L);check(TimeMath.durationNs(period,radix,max+1)<1000000000L)}
println("Temporal Fractions fixed-duration tests passed")
}
}
