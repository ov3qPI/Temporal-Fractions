package cc0.temporalfractions.a0he7vjet48
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioAttributes
import android.os.Build
object OutputRouteGuard {
const val SOURCE_ID="OutputRouteGuard_v20261104T222055Z"
fun enabled(c:Context)=c.getSharedPreferences("settings",0).getBoolean("external_only",false)
fun setEnabled(c:Context,on:Boolean){c.getSharedPreferences("settings",0).edit().putBoolean("external_only",on).apply()}
fun allowed(d:AudioDeviceInfo):Boolean=when(d.type){AudioDeviceInfo.TYPE_WIRED_HEADSET,AudioDeviceInfo.TYPE_WIRED_HEADPHONES,AudioDeviceInfo.TYPE_USB_HEADSET,AudioDeviceInfo.TYPE_USB_DEVICE,AudioDeviceInfo.TYPE_USB_ACCESSORY,AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,AudioDeviceInfo.TYPE_BLUETOOTH_SCO,AudioDeviceInfo.TYPE_BLE_HEADSET,AudioDeviceInfo.TYPE_BLE_SPEAKER,AudioDeviceInfo.TYPE_BLE_BROADCAST,AudioDeviceInfo.TYPE_HEARING_AID->true;else->false}
fun available(c:Context):List<AudioDeviceInfo> {val mgr=c.getSystemService(Context.AUDIO_SERVICE) as AudioManager;return try{mgr.getDevices(AudioManager.GET_DEVICES_OUTPUTS).filter {allowed(it)}}catch(_:Exception){emptyList()}}
fun prereq(c:Context):String? {if(!enabled(c))return null;if(Build.VERSION.SDK_INT<36)return "Connected Audio Only requires Android 16 (API 36) for complete route inspection";if(available(c).isEmpty())return "No eligible headphone, USB audio or Bluetooth output is connected";return null}
fun routed(player:Any):Boolean {
if(Build.VERSION.SDK_INT<36)return false
return try {val method=player.javaClass.getMethod("getRoutedDevices");val devices=method.invoke(player) as? List<*> ?:return false;devices.isNotEmpty()&&devices.all {it is AudioDeviceInfo&&allowed(it)}}catch(_:Exception){false}
}
fun preferred(c:Context):AudioDeviceInfo?{val outputs=available(c);if(outputs.isEmpty())return null;val mgr=c.getSystemService(Context.AUDIO_SERVICE) as AudioManager;if(Build.VERSION.SDK_INT>=33){try{val attrs=AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();mgr.getAudioDevicesForAttributes(attrs).firstOrNull{allowed(it)}?.let{return it}}catch(_:Exception){}};val types=listOf(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,AudioDeviceInfo.TYPE_BLE_HEADSET,AudioDeviceInfo.TYPE_BLE_SPEAKER,AudioDeviceInfo.TYPE_BLE_BROADCAST,AudioDeviceInfo.TYPE_HEARING_AID,AudioDeviceInfo.TYPE_WIRED_HEADPHONES,AudioDeviceInfo.TYPE_WIRED_HEADSET,AudioDeviceInfo.TYPE_USB_HEADSET,AudioDeviceInfo.TYPE_USB_DEVICE,AudioDeviceInfo.TYPE_USB_ACCESSORY,AudioDeviceInfo.TYPE_BLUETOOTH_SCO);return types.firstNotNullOfOrNull{type->outputs.firstOrNull{it.type==type}}?:outputs.first()}
fun routeDetails(player:Any):String{return try{if(Build.VERSION.SDK_INT<36)return "API below 36";val devices=player.javaClass.getMethod("getRoutedDevices").invoke(player) as? List<*>;if(devices==null)"Route query returned null" else if(devices.isEmpty())"Route not established" else devices.joinToString("; "){d->if(d is AudioDeviceInfo)"${d.productName} type=${d.type} id=${d.id} allowed=${allowed(d)}" else "Unexpected device $d"}}catch(ex:Exception){"Route query failed: ${ex.javaClass.simpleName}: ${ex.message}"}}
}
