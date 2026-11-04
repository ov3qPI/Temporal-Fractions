package cc0.temporalfractions.a0he7vjet48
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
object AudioDiagnostics {
const val SOURCE_ID="AudioDiagnostics_v20261104T215458Z"
private val lock=Any()
fun event(c:Context,message:String){val date=SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",Locale.US).apply{timeZone=TimeZone.getTimeZone("UTC")}.format(Date());val entry="$date  $message";synchronized(lock){val prefs=c.getSharedPreferences("audio_diagnostics",0);val previous=prefs.getString("events","")?:"";prefs.edit().putString("events",(previous+entry+"\n").takeLast(16000)).apply()};Log.i("TemporalFractionsAudio",message)}
fun routes(c:Context):String {return try{val m=c.getSystemService(Context.AUDIO_SERVICE) as AudioManager;val outputs=m.getDevices(AudioManager.GET_DEVICES_OUTPUTS).joinToString("; "){"${it.productName} type=${it.type} id=${it.id}${if(OutputRouteGuard.allowed(it))" eligible" else ""}"};val attrs=AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();val policy=if(Build.VERSION.SDK_INT>=33)m.getAudioDevicesForAttributes(attrs).joinToString("; "){"${it.productName} type=${it.type} id=${it.id}"} else "unavailable";"Android API ${Build.VERSION.SDK_INT}\nConnected Audio Only: ${OutputRouteGuard.enabled(c)}\nMusic volume: ${m.getStreamVolume(AudioManager.STREAM_MUSIC)}/${m.getStreamMaxVolume(AudioManager.STREAM_MUSIC)}\nOutputs: $outputs\nMedia policy route: $policy"}catch(ex:Exception){"Route diagnostics unavailable: ${ex.javaClass.simpleName}: ${ex.message}"}}
fun report(c:Context):String=routes(c)+"\n\nRecent playback events (UTC):\n"+(c.getSharedPreferences("audio_diagnostics",0).getString("events","")?:"")
}
