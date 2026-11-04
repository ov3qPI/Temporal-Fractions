package cc0.temporalfractions.a0he7vjet48
import android.content.Context
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.Locale
data class Preset(val id:Long,val name:String,val period:Int,val radix:Int,val level:Int,val uri:String,val file:String)
data class TimerRun(val id:Long,val presetId:Long,val name:String,val period:Int,val radix:Int,val level:Int,val durationNs:Long,val uri:String,val file:String,val startWall:Long,val startElapsed:Long,val deadlineWall:Long,val deadlineElapsed:Long,val bootCount:Int,val generation:Long,val state:String,val detail:String)
class TimerStore(ctx:Context):SQLiteOpenHelper(ctx,"temporal_fractions.sqlite",null,1) {
companion object {const val SOURCE_ID="TimerStore_v20261104T220602Z"}
override fun onCreate(db:SQLiteDatabase){db.execSQL("CREATE TABLE presets(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,period INTEGER NOT NULL,radix INTEGER NOT NULL,level INTEGER NOT NULL,uri TEXT NOT NULL,file TEXT NOT NULL)");db.execSQL("CREATE TABLE runs(id INTEGER PRIMARY KEY AUTOINCREMENT,preset_id INTEGER NOT NULL,name TEXT NOT NULL,period INTEGER NOT NULL,radix INTEGER NOT NULL,level INTEGER NOT NULL,duration_ns INTEGER NOT NULL,uri TEXT NOT NULL,file TEXT NOT NULL,start_wall INTEGER NOT NULL,start_elapsed INTEGER NOT NULL,deadline_wall INTEGER NOT NULL,deadline_elapsed INTEGER NOT NULL,boot_count INTEGER NOT NULL,generation INTEGER NOT NULL DEFAULT 1,state TEXT NOT NULL,detail TEXT NOT NULL DEFAULT '')")}
override fun onUpgrade(db:SQLiteDatabase,old:Int,now:Int){throw IllegalStateException("No migrations from $old to $now")}
private fun Cursor.preset()=Preset(getLong(0),getString(1),getInt(2),getInt(3),getInt(4),getString(5),getString(6))
private fun Cursor.run()=TimerRun(getLong(0),getLong(1),getString(2),getInt(3),getInt(4),getInt(5),getLong(6),getString(7),getString(8),getLong(9),getLong(10),getLong(11),getLong(12),getInt(13),getLong(14),getString(15),getString(16))
private val runColumns="id,preset_id,name,period,radix,level,duration_ns,uri,file,start_wall,start_elapsed,deadline_wall,deadline_elapsed,boot_count,generation,state,detail"
fun presets(longest:Boolean):List<Preset>{val result=mutableListOf<Preset>();readableDatabase.rawQuery("SELECT id,name,period,radix,level,uri,file FROM presets",null).use { while(it.moveToNext())result.add(it.preset()) };return result.sortedWith(compareBy<Preset>{TimeMath.durationNs(it.period,it.radix,it.level)}.let { if(longest)it.reversed() else it }.thenBy { it.name.lowercase(Locale.ROOT) }.thenBy { it.id })}
fun preset(id:Long):Preset?=readableDatabase.rawQuery("SELECT id,name,period,radix,level,uri,file FROM presets WHERE id=?",arrayOf(id.toString())).use { if(it.moveToFirst())it.preset() else null }
fun savePreset(p:Preset):Long {val v=ContentValues().apply {put("name",p.name);put("period",p.period);put("radix",p.radix);put("level",p.level);put("uri",p.uri);put("file",p.file)};return if(p.id>0){writableDatabase.update("presets",v,"id=?",arrayOf(p.id.toString()));p.id}else writableDatabase.insertOrThrow("presets",null,v)}
fun deletePreset(id:Long){writableDatabase.delete("presets","id=?",arrayOf(id.toString()))}
fun runs():List<TimerRun>{val result=mutableListOf<TimerRun>();readableDatabase.rawQuery("SELECT $runColumns FROM runs ORDER BY id DESC LIMIT 300",null).use {while(it.moveToNext())result.add(it.run())};return result}
fun activeRuns():List<TimerRun>{val result=mutableListOf<TimerRun>();readableDatabase.rawQuery("SELECT $runColumns FROM runs WHERE state='armed' ORDER BY deadline_wall",null).use {while(it.moveToNext())result.add(it.run())};return result}
fun getRun(id:Long):TimerRun?=readableDatabase.rawQuery("SELECT $runColumns FROM runs WHERE id=?",arrayOf(id.toString())).use {if(it.moveToFirst())it.run() else null}
fun createRun(p:Preset,wall:Long,elapsed:Long,boot:Int):TimerRun {
val ns=TimeMath.durationNs(p.period,p.radix,p.level);val ms=TimeMath.millisCeil(ns)
val v=ContentValues().apply {put("preset_id",p.id);put("name",p.name);put("period",p.period);put("radix",p.radix);put("level",p.level);put("duration_ns",ns);put("uri",p.uri);put("file",p.file);put("start_wall",wall);put("start_elapsed",elapsed);put("deadline_wall",Math.addExact(wall,ms));put("deadline_elapsed",Math.addExact(elapsed,ms));put("boot_count",boot);put("generation",1L);put("state","armed");put("detail","Armed")}
val id=writableDatabase.insertOrThrow("runs",null,v);return getRun(id)!!
}
fun updateRun(id:Long,state:String,detail:String){val v=ContentValues().apply{put("state",state);put("detail",detail)};writableDatabase.update("runs",v,"id=?",arrayOf(id.toString()))}
fun changeElapsed(id:Long,elapsed:Long,boot:Int,wall:Long){val v=ContentValues().apply {put("deadline_elapsed",elapsed);put("boot_count",boot);put("deadline_wall",wall)};writableDatabase.update("runs",v,"id=? AND state='armed'",arrayOf(id.toString()))}
fun fire(id:Long,generation:Long):TimerRun? {val db=writableDatabase;db.beginTransaction();try {val r=getRun(id);if(r==null||r.state!="armed"||r.generation!=generation){db.setTransactionSuccessful();return null};val v=ContentValues().apply {put("state","fired");put("detail","Triggered")};val ok=db.update("runs",v,"id=? AND generation=? AND state='armed'",arrayOf(id.toString(),generation.toString()));db.setTransactionSuccessful();return if(ok==1)r else null} finally {db.endTransaction()}}
fun cancel(id:Long){val v=ContentValues().apply {put("state","canceled");put("detail","Canceled");put("generation",(getRun(id)?.generation?:0)+1)};writableDatabase.update("runs",v,"id=? AND state='armed'",arrayOf(id.toString()))}
}
