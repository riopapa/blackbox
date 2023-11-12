
@file:JvmName("MergeEvent")

package com.riopapa.blackbox

import android.util.Log
import com.googlecode.mp4parser.authoring.Movie
import com.googlecode.mp4parser.authoring.Track
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator
import com.googlecode.mp4parser.authoring.tracks.AppendTrack
import com.riopapa.blackbox.Vars.DATE_PREFIX
import com.riopapa.blackbox.Vars.FORMAT_TIME
import com.riopapa.blackbox.Vars.SUFFIX
import com.riopapa.blackbox.Vars.gpsTracker
import com.riopapa.blackbox.Vars.mPackageNormalDatePath
import com.riopapa.blackbox.Vars.mPackageNormalPath
import com.riopapa.blackbox.Vars.normal_duration
import com.riopapa.blackbox.Vars.sdfTime
import com.riopapa.blackbox.Vars.utils
import com.riopapa.blackbox.utility.DiskSpace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.text.Collator
import java.text.ParseException
import java.util.Arrays
import java.util.Date
import java.util.LinkedList

class MergeNormal {

    private var nextNormalTime: Long = 0
    private var outputFile: String? = null
    fun exec() {
        val scope = CoroutineScope(Job() + Dispatchers.Main)
        scope.launch {
            execute()
        }
    }
    private suspend fun execute() {

        withContext(Dispatchers.IO) {

            if (nextNormalTime == 0L)
                nextNormalTime = System.currentTimeMillis() - normal_duration - 20000
            val beginTimeS = utils.getMilliSec2String(nextNormalTime, FORMAT_TIME)
            val files2Merge: Array<File> = utils.getDirectoryList(Vars.mPackageWorkingPath)
            if (files2Merge.size < 3) {
                Log.w("normal", "files short, len=" + files2Merge.size)
            } else {
                Arrays.sort(files2Merge)
                val endTimeS: String = files2Merge[files2Merge.size - 3].name
                //                utils.logBoth(logID, beginTimeS+" to "+endTimeS+" len="+files2Merge.length);
                var date: Date? = null
                try {
                    date = sdfTime.parse(endTimeS)
                } catch (e: ParseException) {
                    utils.logE(logID, "$endTimeS parse Error", e)
                }
                nextNormalTime = date!!.time - 6000
                outputFile = File(mPackageNormalDatePath, DATE_PREFIX + beginTimeS + SUFFIX
                            + " x" + gpsTracker.getLatitude() + "," + gpsTracker.getLongitude() + ".mp4"
                ).toString()
                merge2OneVideo(beginTimeS, endTimeS, files2Merge)
            }
        }
    }

    private fun merge2OneVideo(beginTimeS: String, endTimeS: String, files2Merge: Array<File>) {
        val listMovies: MutableList<Movie> = ArrayList()
        val videoTracks: MutableList<Track> = LinkedList()
        val audioTracks: MutableList<Track> = LinkedList()
        val myCollator = Collator.getInstance()
        for (file in files2Merge) {
            val shortFileName = file.name
            if (myCollator.compare(shortFileName, beginTimeS) >= 0 &&
                myCollator.compare(shortFileName, endTimeS) < 0
            ) {
                try {
                    listMovies.add(MovieCreator.build(file.toString()))
                } catch (e: Exception) {
                    Log.w("Err", "<<Event Merge>> " + files2Merge.size)
                }
            }
        }
        for (movie in listMovies) {
            for (track in movie.tracks) {
                if (track.handler == "vide") {    // excluding "audi"
                    videoTracks.add(track)
                } else { // track.getHandler().equals("soon")
                    audioTracks.add(track)
                }
            }
        }
        if (videoTracks.isNotEmpty()) {
            val outputMovie = Movie()
            try {
                outputMovie.addTrack(AppendTrack(*videoTracks.toTypedArray()))
                outputMovie.addTrack(AppendTrack(*audioTracks.toTypedArray()))
                val container = DefaultMp4Builder().build(outputMovie)
                val fileChannel = RandomAccessFile(outputFile, "rw").channel
                container.writeContainer(fileChannel)
                fileChannel.close()
                Log.w("normal", "$beginTimeS done")
                // remove old working files //
                val toTimeS : String = utils.getMilliSec2String(System.currentTimeMillis()
                        - normal_duration, FORMAT_TIME)
                for (file in files2Merge) {
                    val shortFileName = file.name
                    if (myCollator.compare(shortFileName, toTimeS) < 0)
                        file.delete()
                }

                val msg :String = DiskSpace().squeeze(mPackageNormalPath)
                if (msg.isNotEmpty())
                    utils.logBoth("DISK", msg)

            } catch (e: IOException) {
                    utils.logE(logID, "IOException~ ", e)
            }
        } else {
            utils.beepOnce(3, 1f)
            utils.logOnly(logID, "IOException~ ")
        }
    }
    companion object {
        private const val logID = "Normal"
    }
}


