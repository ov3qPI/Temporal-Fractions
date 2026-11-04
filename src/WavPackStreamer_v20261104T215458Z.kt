package cc0.temporalfractions.a0he7vjet48
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.net.Uri
import android.os.PowerManager
import android.os.SystemClock
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
class WavPackStreamer(private val context:Context,private val uri:String,private val restricted:Boolean,private val started:(AudioTrack)->Unit,private val finished:(String)->Unit){
companion object {const val SOURCE_ID="WavPackStreamer_v20261104T215458Z"}
private val stopping=AtomicBoolean(false)
private val authorized=AtomicBoolean(!restricted)
fun authorize(){authorized.set(true)}
@Volatile var track:AudioTrack?=null
@Volatile private var worker:Thread?=null
fun start(){worker=Thread({stream()},"temporal-wavpack-stream").also{it.start()}}
fun stop(){stopping.set(true);worker?.interrupt();try{track?.setVolume(0f);track?.pause();track?.flush()}catch(_:Exception){}}
private fun stream(){var outcome="Completed";var handle=0L;var output:AudioTrack?=null;var wake:PowerManager.WakeLock?=null
try {
val power=context.getSystemService(Context.POWER_SERVICE) as PowerManager
wake=power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"TemporalFractions:WavPack");wake.acquire()
context.contentResolver.openFileDescriptor(Uri.parse(uri),"r")?.use {pfd->
handle=WavPackNative.nativeOpen(pfd.fd)
val info=WavPackNative.nativeInfo(handle)?:throw IOException("Invalid WavPack header")
if(info.size<3||info[0] !in 8000..192000||info[1] !in 1..2)throw IOException("WavPack supports 8–192 kHz mono/stereo")
val rate=info[0];val channels=info[1];val mask=if(channels==1)AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO
val min=AudioTrack.getMinBufferSize(rate,mask,AudioFormat.ENCODING_PCM_FLOAT)
if(min<=0)throw IOException("Floating-point PCM unsupported")
val attrs=AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
val format=AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_FLOAT).setSampleRate(rate).setChannelMask(mask).build()
output=AudioTrack(attrs,format,maxOf(min*2,rate*channels*2),AudioTrack.MODE_STREAM,AudioManagerCompat.SESSION_GENERATE)
val a=output
if(a.state!=AudioTrack.STATE_INITIALIZED)throw IOException("Could not initialize AudioTrack")
track=a
if(restricted){a.setVolume(0f);val pref=OutputRouteGuard.preferred(context);if(pref!=null){val accepted=a.setPreferredDevice(pref);AudioDiagnostics.event(context,"WavPack AudioTrack preferred=${pref.productName} type=${pref.type} accepted=$accepted")}else AudioDiagnostics.event(context,"WavPack AudioTrack: no preferred eligible device")}
if(stopping.get())return@use
if(restricted&&OutputRouteGuard.prereq(context)!=null)throw IOException("External audio route vanished")
a.play()
if(a.playState!=AudioTrack.PLAYSTATE_PLAYING)throw IOException("AudioTrack did not begin")
AudioDiagnostics.event(context,"WavPack AudioTrack playState=${a.playState}, sampleRate=$rate, channels=$channels")
started(a)
if(restricted){val silence=FloatArray(1024*channels);while(!stopping.get()&&!authorized.get()){val wrote=a.write(silence,0,silence.size,AudioTrack.WRITE_BLOCKING);if(wrote<0)throw IOException("AudioTrack route-primer failed: $wrote");if(wrote==0)SystemClock.sleep(20)}}
val buffer=FloatArray(4096*channels)
var submitted=0L
while(!stopping.get()){
val frames=WavPackNative.nativeRead(handle,buffer,4096)
if(frames<0)throw IOException("WavPack decoding failed")
if(frames==0)break
val count=frames*channels
var offset=0
while(offset<count&&!stopping.get()){
val wrote=a.write(buffer,offset,count-offset,AudioTrack.WRITE_BLOCKING)
if(wrote<0)throw IOException("AudioTrack write failed: $wrote")
if(wrote==0){SystemClock.sleep(10);continue}
offset+=wrote
}
submitted+=offset/channels
}
if(!stopping.get()){
var previous=0L;var wraps=0L
while(!stopping.get()){
val now=a.playbackHeadPosition.toLong() and 0xffffffffL
if(now<previous)wraps++
previous=now
if(wraps*4294967296L+now>=submitted)break
SystemClock.sleep(40)
}
}
}?:throw IOException("WavPack file cannot be opened")
if(stopping.get())outcome="Stopped"
}catch(ex:Exception){outcome=if(stopping.get())"Stopped" else "Playback error: ${ex.message}"}catch(ex:LinkageError){outcome="Native WavPack error: ${ex.message}"}finally{
track=null
try{output?.pause();output?.flush();output?.release()}catch(_:Exception){}
if(handle!=0L)try{WavPackNative.nativeClose(handle)}catch(_:Throwable){}
if(wake?.isHeld==true)wake.release()
finished(outcome)
}
}
}
private object AudioManagerCompat {const val SESSION_GENERATE=0}
