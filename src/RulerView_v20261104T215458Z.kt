package cc0.temporalfractions.a0he7vjet48
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.roundToInt
class RulerView(ctx:Context,val radix:Int,private val choose:(Int,Int)->Unit):View(ctx){
companion object {const val SOURCE_ID="RulerView_v20261104T215458Z"}
private val density=resources.displayMetrics.density
private val step=43f*density
private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
private val frame=RectF()
private val clip=Path()
private var position=4f
private var selected=4
private var lastY=0f
private var moved=0f
var period=0;set(value){field=value;selected=selected.coerceIn(0,TimeMath.maxLevel(value,radix));position=selected.toFloat();updateDescription();invalidate()}
var chosenRadix=radix;set(value){field=value;invalidate()}
var chosenLevel:Int get()=selected;set(value){selected=value.coerceIn(0,TimeMath.maxLevel(period,radix));position=selected.toFloat();updateDescription();invalidate()}
init{isFocusable=true;isClickable=true;contentDescription="Fraction ruler"}
private fun px(value:Float)=value*density
private fun updateDescription(){contentDescription="${TimeMath.names[period]}, ${if(radix==2)"dyadic" else "triadic"}, ${TimeMath.fraction(radix,selected)}"}
private fun chooseLevel(n:Int){val v=n.coerceIn(0,TimeMath.maxLevel(period,radix));if(v!=selected){selected=v;updateDescription();choose(radix,v)};invalidate()}
private fun snapTo(n:Int){position=n.coerceIn(0,TimeMath.maxLevel(period,radix)).toFloat();chooseLevel(position.toInt())}
private fun pen(color:Int,stroke:Float=0f):Paint{paint.color=color;paint.style=if(stroke==0f)Paint.Style.FILL else Paint.Style.STROKE;paint.strokeWidth=stroke;paint.strokeCap=Paint.Cap.SQUARE;paint.typeface=android.graphics.Typeface.DEFAULT;return paint}
override fun onDraw(c:Canvas){super.onDraw(c);val w=width.toFloat();val h=height.toFloat();if(w<=0f||h<=0f)return;frame.set(0f,0f,w,h);clip.reset();clip.addRoundRect(frame,px(12f),px(12f),Path.Direction.CW);c.save();c.clipPath(clip);c.drawColor(Color.rgb(20,20,20));val center=h/2f;val rail=px(43f);val pad=px(8f);val bottom=TimeMath.maxLevel(period,radix);c.drawLine(rail,0f,rail,h,pen(Color.rgb(78,65,65),px(1f)));c.drawRoundRect(pad,center-step*0.46f,w-pad,center+step*0.46f,px(6f),px(6f),pen(Color.rgb(66,29,29)));for(n in 0..bottom){val y=center+(n-position)*step;if(y< -step||y>h+step)continue;val active=n==selected;val length=if(n%4==0)px(69f) else if(n%2==0)px(58f) else px(46f);val x2=rail+length;val lineColor=if(active)Color.WHITE else Color.rgb(219,79,79);c.drawLine(rail,y,x2,y,pen(lineColor,if(active)px(3f) else px(2f)));paint.color=if(active)Color.WHITE else Color.rgb(184,176,176);paint.style=Paint.Style.FILL;paint.textSize=px(if(active)19f else 16f);paint.typeface=if(active)android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT;c.drawText(TimeMath.fraction(radix,n),rail+px(84f),y+paint.textSize*0.36f,paint)};val indicator=Path().apply{moveTo(px(11f),center-px(7f));lineTo(px(23f),center);lineTo(px(11f),center+px(7f));close()};c.drawPath(indicator,pen(Color.rgb(243,77,77)));c.restore();c.drawRoundRect(px(0.5f),px(0.5f),w-px(0.5f),h-px(0.5f),px(12f),px(12f),pen(Color.rgb(65,65,65),px(1f)))}
override fun onTouchEvent(e:MotionEvent):Boolean{when(e.actionMasked){MotionEvent.ACTION_DOWN->{lastY=e.y;moved=0f;parent?.requestDisallowInterceptTouchEvent(true);return true};MotionEvent.ACTION_MOVE->{val delta=e.y-lastY;lastY=e.y;moved+=abs(delta);position=(position-delta/step).coerceIn(0f,TimeMath.maxLevel(period,radix).toFloat());chooseLevel(position.roundToInt());return true};MotionEvent.ACTION_UP->{val n=if(moved<px(9f))(position+(e.y-height/2f)/step).roundToInt() else position.roundToInt();snapTo(n);parent?.requestDisallowInterceptTouchEvent(false);performClick();return true};MotionEvent.ACTION_CANCEL->{snapTo(position.roundToInt());parent?.requestDisallowInterceptTouchEvent(false);return true}};return super.onTouchEvent(e)}
override fun performClick():Boolean{super.performClick();return true}
}
