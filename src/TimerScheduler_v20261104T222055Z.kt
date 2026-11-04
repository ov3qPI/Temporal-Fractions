package cc0.temporalfractions.a0he7vjet48
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
object TimerScheduler {
const val SOURCE_ID="TimerScheduler_v20261104T222055Z"
const val ACTION_FIRE="cc0.temporalfractions.a0he7vjet48.FIRE"
fun bootCount(c:Context):Int=try{Settings.Global.getInt(c.contentResolver,"boot_count",-1)}catch(_:Exception){-1}
private fun manager(c:Context)=c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
fun authorized(c:Context):Boolean=manager(c).canScheduleExactAlarms()
private fun pending(c:Context,r:TimerRun):PendingIntent {
val i=Intent(c,TimerAlarmReceiver::class.java).setAction(ACTION_FIRE).setData(Uri.parse("fractiontimer://run/${r.id}")).putExtra("run",r.id).putExtra("generation",r.generation)
return PendingIntent.getBroadcast(c,0,i,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}
fun cancel(c:Context,r:TimerRun){manager(c).cancel(pending(c,r))}
fun arm(c:Context,r:TimerRun,restoring:Boolean=false):String {
if(!authorized(c))throw IllegalStateException("Grant Alarms & reminders permission")
val now=System.currentTimeMillis();val elapsed=SystemClock.elapsedRealtime();val currentBoot=bootCount(c)
val sameBoot=!restoring&&r.startElapsed<=elapsed&&(r.bootCount<0||currentBoot<0||r.bootCount==currentBoot)
val left=if(sameBoot)r.deadlineElapsed-elapsed else r.deadlineWall-now
if(left<=0){TimerStore(c).use { it.updateRun(r.id,"missed","Expired while the app was unavailable; sound skipped") };cancel(c,r);return "Expired without playback"}
val target=Math.addExact(now,left)
TimerStore(c).use {it.changeElapsed(r.id,Math.addExact(elapsed,left),currentBoot,target)}
val show=PendingIntent.getActivity(c,0,Intent(c,MainActivity::class.java).setAction("OPEN_TIMER").setData(Uri.parse("fractiontimer://show/${r.id}")),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
manager(c).setAlarmClock(AlarmManager.AlarmClockInfo(target,show),pending(c,r))
return "Armed"
}
fun restore(c:Context,fromBoot:Boolean=false){TimerStore(c).use {store->store.activeRuns().forEach{r->try{arm(c,r,fromBoot)}catch(ex:Exception){store.updateRun(r.id,"blocked","Could not reschedule: ${ex.message}")}}}}
}
class TimerAlarmReceiver:BroadcastReceiver(){override fun onReceive(c:Context,i:Intent){if(i.action!=TimerScheduler.ACTION_FIRE)return;val id=i.getLongExtra("run",-1);val generation=i.getLongExtra("generation",-1);val run=TimerStore(c).use {it.fire(id,generation)}?:return;AudioDiagnostics.event(c,"Timer alarm broadcast received: run=${run.id}, name=${run.name}, file=${run.file}");val service=Intent(c,PlaybackService::class.java).setAction("ALARM").putExtra("run",run.id);try{c.startForegroundService(service)}catch(ex:Exception){TimerStore(c).use {it.updateRun(run.id,"blocked","Playback service could not start: ${ex.message}")};AudioDiagnostics.event(c,"Foreground playback service launch failed: ${ex.javaClass.simpleName}: ${ex.message}");Log.e("TemporalFractions","Foreground start failed",ex)}}}
class TimerSystemReceiver:BroadcastReceiver(){override fun onReceive(c:Context,i:Intent){val action=i.action?:return;if(action==Intent.ACTION_BOOT_COMPLETED||action==Intent.ACTION_LOCKED_BOOT_COMPLETED||action==Intent.ACTION_TIME_CHANGED||action==Intent.ACTION_TIMEZONE_CHANGED||action==AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED){if(action==Intent.ACTION_LOCKED_BOOT_COMPLETED)return;TimerScheduler.restore(c,action==Intent.ACTION_BOOT_COMPLETED)}}}
