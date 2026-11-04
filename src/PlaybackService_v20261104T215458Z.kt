package cc0.temporalfractions.a0he7vjet48
import android.app.*
import android.content.*
import android.graphics.drawable.Icon
import android.media.*
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.Uri
import android.os.*
import java.io.IOException
import java.util.Locale
class PlaybackService:Service() {
companion object {const val SOURCE_ID="PlaybackService_v20261104T215458Z";const val CHANNEL="temporal_playback";@Volatile var playing=false;@Volatile var nowPlaying=""}
private val main=Handler(Looper.getMainLooper())
private var player:MediaPlayer?=null
private var wavpack:WavPackStreamer?=null
private var session:MediaSession?=null
private var audio:AudioManager?=null
private var focus:AudioFocusRequest?=null
private var currentRun=-1L
private var currentName=""
private var generation=0L
private var checked=false
private var checkCount=0
private var restrict=false
private var deviceCallback:AudioDeviceCallback?=null
private var checkTask:Runnable?=null
private val routeListener=AudioRouting.OnRoutingChangedListener {main.post {verifyRoute()} }
override fun onCreate(){super.onCreate();val nm=getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager;nm.createNotificationChannel(NotificationChannel(CHANNEL,"Timer audio",NotificationManager.IMPORTANCE_LOW));session=MediaSession(this,"TemporalFractions");session?.setCallback(object:MediaSession.Callback(){override fun onStop(){finish("Stopped")};override fun onPause(){finish("Stopped")};override fun onSeekTo(pos:Long){finish("Stopped by seek")};override fun onSkipToNext(){finish("Stopped by skip")};override fun onSkipToPrevious(){finish("Stopped by skip")};override fun onPlay(){}});session?.setActive(true)}
private fun notification(text:String):Notification {
val token=generation
val stop=Intent(this,PlaybackService::class.java).setAction("STOP").setData(Uri.parse("temporalstop://$token")).putExtra("token",token)
val pi=PendingIntent.getService(this,0,stop,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
val open=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
return Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("Temporal Fractions").setContentText(text).setOngoing(true).setContentIntent(open).addAction(Notification.Action.Builder(Icon.createWithResource(this,android.R.drawable.ic_media_pause),"Stop",pi).build()).setStyle(Notification.MediaStyle().setMediaSession(session!!.sessionToken).setShowActionsInCompactView(0)).build()
}
private fun status(text:String){if(currentRun>0)TimerStore(this).use {it.updateRun(currentRun,if(text=="Playing")"playing" else "blocked",text)}}
private fun playbackState(on:Boolean){session?.setPlaybackState(PlaybackState.Builder().setActions(PlaybackState.ACTION_STOP or PlaybackState.ACTION_PAUSE).setState(if(on)PlaybackState.STATE_PLAYING else PlaybackState.STATE_STOPPED,PlaybackState.PLAYBACK_POSITION_UNKNOWN,if(on)1f else 0f).build())}
override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
val action=intent?.action?:"STOP"
if(action=="STOP"){if(intent==null||!intent.hasExtra("token")||intent.getLongExtra("token",-1)==generation)finish("Stopped");return START_NOT_STICKY}
if(action!="ALARM"&&action!="PREVIEW")return START_NOT_STICKY
if(action=="PREVIEW"&&playing&&currentRun>0)return START_NOT_STICKY
val runId=intent?.getLongExtra("run",-1L) ?: -1L
val r=if(action=="ALARM")TimerStore(this).use{it.getRun(runId)} else null
val source=if(action=="PREVIEW")intent?.getStringExtra("uri")?:"" else r?.uri?:""
val sourceName=if(action=="PREVIEW")intent?.getStringExtra("file")?:"Preview" else r?.file?:""
val display=if(action=="PREVIEW")intent?.getStringExtra("name")?:"Audio test" else r?.name?:"Timer"
if(action=="ALARM"&&r==null){AudioDiagnostics.event(this,"Alarm run $runId missing from database");return START_NOT_STICKY}
if(source.isEmpty()){if(r!=null)TimerStore(this).use{it.updateRun(runId,"blocked","No audio file selected")};AudioDiagnostics.event(this,"$action $display: No audio file selected");return START_NOT_STICKY}
release("Interrupted by another timer",false)
currentRun=if(action=="ALARM")runId else -1L
currentName=display
restrict=OutputRouteGuard.enabled(this)
checked=false;checkCount=0
val token=++generation
playing=true;nowPlaying=display
AudioDiagnostics.event(this,"$action started, name=$display, file=$sourceName, restricted=$restrict; ${AudioDiagnostics.routes(this).replace("\n"," | ")}")
try{startForeground(8317,notification("Preparing $display"))}catch(ex:Exception){AudioDiagnostics.event(this,"Foreground service failed: ${ex.javaClass.simpleName}: ${ex.message}");finish("Playback error: foreground service failed");return START_NOT_STICKY}
playbackState(false)
val problem=OutputRouteGuard.prereq(this)
if(problem!=null){finish("Audio blocked: $problem");return START_NOT_STICKY}
try {
audio=getSystemService(Context.AUDIO_SERVICE) as AudioManager
val attrs=AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
focus=AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attrs).setOnAudioFocusChangeListener {change->if(token==generation&&(change==AudioManager.AUDIOFOCUS_LOSS||change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT))main.post{finish("Audio focus lost")} }.build()
val focusResult=audio?.requestAudioFocus(focus!!)
AudioDiagnostics.event(this,"Audio focus request returned $focusResult")
if(focusResult!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED)throw IOException("Audio focus unavailable: result $focusResult")
if(sourceName.lowercase(Locale.ROOT).endsWith(".wv")){
AudioDiagnostics.event(this,"Opening WavPack streamer")
val w=WavPackStreamer(this,source,restrict,{track->main.post{if(token==generation){AudioDiagnostics.event(this,"AudioTrack entered PLAYING state; ${OutputRouteGuard.routeDetails(track)}");track.addOnRoutingChangedListener(routeListener,main);startChecks();started()}else wStop(track)}},{outcome->main.post{if(token==generation)finish(outcome)}})
wavpack=w;w.start()
}else{
val p=MediaPlayer();player=p
p.setAudioAttributes(attrs);p.setWakeMode(this,PowerManager.PARTIAL_WAKE_LOCK);p.setVolume(if(restrict)0f else 1f,if(restrict)0f else 1f)
p.setDataSource(this,Uri.parse(source));p.isLooping=restrict
p.setOnPreparedListener {if(token==generation){try{if(restrict){val preferred=OutputRouteGuard.preferred(this);if(preferred!=null){val chosen=p.setPreferredDevice(preferred);AudioDiagnostics.event(this,"MediaPlayer preferred device=${preferred.productName} type=${preferred.type}, accepted=$chosen")}else AudioDiagnostics.event(this,"No eligible preferred device available")};p.start();AudioDiagnostics.event(this,"MediaPlayer start called, isPlaying=${p.isPlaying}, restricted=$restrict; ${OutputRouteGuard.routeDetails(p)}");main.postDelayed({if(token==generation&&player===p)AudioDiagnostics.event(this,"MediaPlayer after 800ms: isPlaying=${p.isPlaying}; ${OutputRouteGuard.routeDetails(p)}")},800);p.addOnRoutingChangedListener(routeListener,main);startChecks();started()}catch(ex:Exception){AudioDiagnostics.event(this,"MediaPlayer prepared callback failed: ${ex.javaClass.simpleName}: ${ex.message}");finish("Playback error: ${ex.message}")}}}
p.setOnCompletionListener {if(token==generation)finish("Completed")}
p.setOnErrorListener {_,what,extra->if(token==generation)finish("Media error $what/$extra");true}
AudioDiagnostics.event(this,"MediaPlayer prepareAsync starting")
p.prepareAsync()
}
}catch(ex:Exception){finish("Playback error: ${ex.message}")}catch(ex:LinkageError){finish("Native decoder unavailable: ${ex.message}")}
return START_NOT_STICKY
}
private fun wStop(track:AudioTrack){try{track.setVolume(0f);track.stop()}catch(_:Exception){}}
private fun started(){if(!restrict){val route=player?.let{OutputRouteGuard.routeDetails(it)}?:wavpack?.track?.let{OutputRouteGuard.routeDetails(it)}?:"No audio object";AudioDiagnostics.event(this,"Unrestricted playback started; route=$route; ${AudioDiagnostics.routes(this).replace("\n"," | ")}");status("Playing");playbackState(true);(getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(8317,notification("Playing $currentName"))}}
private fun startChecks(){if(!restrict)return;deviceCallback=object:AudioDeviceCallback(){override fun onAudioDevicesAdded(added:Array<AudioDeviceInfo>){main.post{verifyRoute()}};override fun onAudioDevicesRemoved(removed:Array<AudioDeviceInfo>){main.post{verifyRoute()}}};audio?.registerAudioDeviceCallback(deviceCallback,main);val task=object:Runnable{override fun run(){if(!playing||!restrict)return;verifyRoute();if(playing)main.postDelayed(this,150)}};checkTask=task;main.postDelayed(task,60)}
private fun verifyRoute(){if(!playing||!restrict)return;val obj:Any=player?:wavpack?.track?:return;val ok=OutputRouteGuard.routed(obj)
if(ok){if(!checked){checked=true;AudioDiagnostics.event(this,"Eligible output confirmed; ${OutputRouteGuard.routeDetails(obj)}");player?.let{it.seekTo(0);it.isLooping=false;it.setVolume(1f,1f)};wavpack?.authorize();wavpack?.track?.setVolume(1f);status("Playing");playbackState(true);(getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(8317,notification("Playing $currentName"))}}
else {if(checked){player?.setVolume(0f,0f);wavpack?.track?.setVolume(0f);finish("Audio blocked: routed to a disallowed or missing output")}else {checkCount++;if(checkCount==1||checkCount%10==0)AudioDiagnostics.event(this,"Waiting for eligible audio route; attempt $checkCount; ${OutputRouteGuard.routeDetails(obj)}");if(checkCount>=60)finish("Audio blocked: external output could not be verified; ${OutputRouteGuard.routeDetails(obj)}")}}
}
private fun release(reason:String,terminate:Boolean){val prior=currentRun;val previousName=currentName;if(playing)AudioDiagnostics.event(this,"Playback ended: $reason; restricted=$restrict; route=${player?.let{OutputRouteGuard.routeDetails(it)}?:wavpack?.track?.let{OutputRouteGuard.routeDetails(it)}?:"none"}");currentRun=-1;generation++;playing=false;nowPlaying="";checkTask?.let{main.removeCallbacks(it)};checkTask=null;deviceCallback?.let{try{audio?.unregisterAudioDeviceCallback(it)}catch(_:Exception){}};deviceCallback=null
val w=wavpack;wavpack=null;w?.stop()
val p=player;player=null;if(p!=null){try{p.setVolume(0f,0f);p.stop()}catch(_:Exception){};p.release()}
val old=focus;focus=null;if(old!=null)try{audio?.abandonAudioFocusRequest(old)}catch(_:Exception){}
playbackState(false)
val intentionallyBlocked=reason.startsWith("Audio blocked")
val playbackFailed=reason.startsWith("Playback error")||reason.startsWith("Media error")||reason.startsWith("Native decoder")
if(prior>0){val state=when {reason=="Completed"->"completed";reason=="Stopped"->"stopped";intentionallyBlocked->"blocked";playbackFailed->"failed";else->"interrupted"};TimerStore(this).use {it.updateRun(prior,state,reason)}}
if(terminate){stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();if(prior>0&&(intentionallyBlocked||playbackFailed)){val heading=if(intentionallyBlocked)"Timer audio blocked" else "Timer audio failed";val notice=Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle(heading).setContentText("$previousName · $reason").setAutoCancel(true).build();(getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(8320,notice)}}
}
private fun finish(reason:String){release(reason,true)}
override fun onDestroy(){release("Service destroyed",false);session?.release();session=null;super.onDestroy()}
override fun onBind(i:Intent?):IBinder?=null
}
