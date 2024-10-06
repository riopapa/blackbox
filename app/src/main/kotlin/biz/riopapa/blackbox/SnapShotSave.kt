package biz.riopapa.blackbox

import android.os.SystemClock
import android.util.Log
import biz.riopapa.blackbox.utility.ImageStack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

internal class SnapShotSave {
    companion object {
        var nbr:Long = 1000000
    }

    fun exec(path2Write: File, phase: Int, last: Boolean) {
        if (phase == 1)
            nbr = 1000000
        val scope = CoroutineScope(Job() + Dispatchers.Main)
        scope.launch {
            execute(path2Write, phase, last)
        }
    }
    private suspend fun execute(path2Write: File, phase: Int, last: Boolean) {
        withContext(Dispatchers.IO) {

            val minPos = 0
//            val suffix = phase * 1000
            val maxSize = Vars.share_image_size - 2

            var jpgBytes = getClone(Vars.imageStack.snapNowPos)

            var prefixTime: String = path2Write.name
            prefixTime = "D" + prefixTime.substring(1, prefixTime.length - 1) + "."
            val th = Thread {
                for (i in minPos until maxSize) {
                    if (jpgBytes[i] == null) continue
                    var t = ("" + nbr).substring(2)
                    t = t.substring(0,2) + "-" + t.substring(2,4)
                    nbr += Vars.share_snap_interval + 40
                    val imageFile = File(path2Write, "$prefixTime$t.jpg")
                    if (jpgBytes[i]!!.size > 1) {
                        bytes2File(jpgBytes[i], imageFile)
                        SystemClock.sleep(24) // not to hold all the time
                    } else Log.e("$phase image error $i", imageFile.name)
                    jpgBytes[i] = null
                }
                if (last) { // last phase
                    Vars.utils.beepOnce(3, 1f)
                    Vars.mActivity.runOnUiThread {
                        Vars.vTextCountEvent.text = (++Vars.CountEvent).toString()
                        Vars.activeEventCount--
                        val text =
                            if ((Vars.activeEventCount == 0)) "" else " " + Vars.activeEventCount + " "
                        Vars.vTextActiveCount.text = text
                    }
                    Vars.utils.logBoth("finish", path2Write.name)
                    System.gc()
                }
            }
            th.start()
        }
    }

    private fun bytes2File(bytes: ByteArray?, file: File?) {
        var fileOutputStream: FileOutputStream? = null
        try {
            fileOutputStream = FileOutputStream(file)
            fileOutputStream.write(bytes)
        } catch (e: IOException) {
            Vars.utils.logE("snap", "IOException catch", e)
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close()
                } catch (e: IOException) {
                    Vars.utils.logE("snap", "IOException finally", e)
                }
            }
        }
    }

    private fun getClone(startPos: Int): Array<ByteArray?> {
        var jpgIdx = 0

        val jBytes = arrayOfNulls<ByteArray>(Vars.share_image_size)
        for (i in startPos until Vars.share_image_size) {
            if (ImageStack.snapBytes[i] != null) {
                jBytes[jpgIdx++] = ImageStack.snapBytes[i].clone()
                ImageStack.snapBytes[i] = null
            }
        }
        for (i in 0 until startPos - 1) {
            if (jpgIdx >= Vars.share_image_size) break
            if (ImageStack.snapBytes[i] != null) {
                jBytes[jpgIdx++] = ImageStack.snapBytes[i].clone()
                ImageStack.snapBytes[i] = null
            }
        }
        return jBytes
    }
}