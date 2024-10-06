
@file:JvmName("MergeEvent")

package biz.riopapa.blackbox

import android.media.MediaPlayer
import android.util.Log
import com.googlecode.mp4parser.authoring.Movie
import com.googlecode.mp4parser.authoring.Track
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator
import com.googlecode.mp4parser.authoring.tracks.AppendTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.text.Collator
import java.util.Arrays
import java.util.LinkedList

class MergeEvent {

    private var outputFile: String? = null
    fun exec(startTime: Long) {
        val scope = CoroutineScope(Job() + Dispatchers.Main)
        scope.launch {
            execute(startTime)
        }
    }
    private suspend fun execute(startTime: Long) {

        withContext(Dispatchers.IO) {
            val beginTimeS = Vars.utils.getMilliSec2String(startTime, Vars.FORMAT_TIME)
            val endTimeS: String
            val latitude = Vars.gpsTracker.getLatitude()
            val longitude = Vars.gpsTracker.getLongitude()
            val files2Merge: Array<File> = Vars.utils.getDirectoryList(Vars.mPackageWorkingPath)
            if (files2Merge.size < 3) {
                Log.e("Err",
                    Vars.utils.logBoth("eventMP4", "<<file[] too short " + files2Merge.size).toString())
            } else {
                Arrays.sort(files2Merge)
                endTimeS = files2Merge[files2Merge.size - 2].name
                outputFile = File(
                    Vars.mPackageEventPath, Vars.DATE_PREFIX + beginTimeS + Vars.SUFFIX
                            + " x" + latitude + "," + longitude + ".mp4"
                ).toString()
                merge2OneVideo(beginTimeS, endTimeS, files2Merge)
                val mp = MediaPlayer()
                try {
                    mp.setDataSource(outputFile)
                    mp.prepare()
                } catch (e: IOException) {
                    Log.w("Err", "<<Event IO>> " + files2Merge.size)
                }
                mp.release()
                Vars.utils.logBoth("eventMP4", beginTimeS)
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
            } catch (e: IOException) {
                Vars.utils.logE(LOG_ID, "IOException~ ", e)
            }
        } else {
            Vars.utils.beepOnce(3, 1f)
            Vars.utils.logOnly(LOG_ID, "IOException~ ")
        }
        Vars.utils.logBoth("Even", "videoTracks.size() = " + videoTracks.size)
    }
    companion object {
        private const val LOG_ID = "EMERGE"
    }
}


