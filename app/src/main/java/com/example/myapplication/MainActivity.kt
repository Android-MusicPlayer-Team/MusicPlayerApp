package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import androidx.core.content.ContextCompat
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.media.PlaybackParams
import android.provider.MediaStore
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ListView
import android.widget.SeekBar
import android.widget.SimpleAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.nio.charset.Charset
import java.util.Calendar
import java.util.Random

/**
 * 音乐播放器主界面
 *
 * 课题要求实现功能：
 *  1. 播放常见格式音乐文件（MP3/AMR/AAC/OGG）—— MediaPlayer 原生支持
 *  2. 暂停、快进（+10秒）、倒退（-10秒）
 *  3. 显示歌曲信息（歌名、歌手、时长）
 *  4. 扫描手机目录下歌曲并加入播放列表
 *  5. 上一首、下一首、顺序循环、单曲循环、随机播放
 *  6.（可选功能）歌词显示：读取与歌曲同目录同名的 .lrc 文件，跟随进度显示
 *
 * 实现思路：用系统自带的 MediaPlayer 播放，用 MediaStore 扫描手机里的音乐文件，
 * 用 ListView 展示歌曲列表，用 SeekBar 显示播放进度，用 TextView 显示当前歌词。
 */
class MainActivity : Activity() {

    // ========== 数据模型：一首歌 ==========
    data class Song(
        val title: String,
        val artist: String,
        val uri: Uri,
        val duration: Long,
        val dataPath: String   // 歌曲在手机里的真实路径（用于找同名歌词文件）
    )

    // ========== 播放器与播放状态 ==========
    private var mediaPlayer: MediaPlayer? = null      // 播放器对象
    private val songList = ArrayList<Song>()          // 扫描到的歌曲列表
    private var currentIndex = 0                      // 当前播放的是第几首
    private var playMode = 0                          // 播放模式：0=顺序循环 1=单曲循环 2=随机播放
    private var dailyIndex = -1                       // 今日推荐的歌曲在列表中的位置（-1 表示还没有）

    // ========== 歌词相关 ==========
    private val lrcList = ArrayList<Pair<Long, String>>()  // 解析后的歌词：时间(毫秒) -> 歌词文字

    // 当前选中/正在播放的歌曲下标，初始-1无选中
    private var currentPlayPosition = -1

    // ========== 倍速播放相关 ==========
    private var currentSpeed = 1.0f
    // 可选档位
    private val speedList = floatArrayOf(0.5f,0.75f,1.0f,1.25f,1.5f,2.0f)
    private val speedTextList = arrayOf("0.5x","0.75x","1.0x","1.25x","1.5x","2.0x")
    private var songAdapter: SimpleAdapter? = null

    // ========== UI 控件 ==========
    private lateinit var tvTitle: TextView    // 歌名
    private lateinit var tvArtist: TextView   // 歌手
    private lateinit var tvTime: TextView     // 时间显示
    private lateinit var tvLyric: TextView    // 歌词显示
    private lateinit var seekBar: SeekBar     // 进度条
    private lateinit var btnPlay: ImageButton // 播放/暂停按钮
    private lateinit var btnMode: Button      // 播放模式按钮
    private lateinit var listView: ListView   // 歌曲列表
    private lateinit var tvDailySong: TextView // 今日推荐歌曲显示
    private lateinit var spinnerSpeed: Spinner // 倍速下拉选择框

    // 用于定时刷新进度条的 Handler（主线程）
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()                 // 1. 初始化界面
        requestAudioPermission()    // 2. 申请读取音频的权限（成功后会扫描歌曲）
    }

    /** 初始化界面控件并绑定点击事件 */
    private fun initViews() {
        tvTitle = findViewById(R.id.tvTitle)
        tvArtist = findViewById(R.id.tvArtist)
        tvTime = findViewById(R.id.tvTime)
        tvLyric = findViewById(R.id.tvLyric)
        seekBar = findViewById(R.id.seekBar)
        btnPlay = findViewById(R.id.btnPlay)
        btnMode = findViewById(R.id.btnMode)
        listView = findViewById(R.id.listView)
        tvDailySong = findViewById(R.id.tvDailySong)
        spinnerSpeed = findViewById(R.id.spinnerSpeed)

        // ====== Spinner 倍速下拉（文字居中、白色文字、深色下拉背景）======
        val adapter = object : ArrayAdapter<String>(
            this,
            R.layout.item_speed,
            speedTextList
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view as TextView
                textView.gravity = Gravity.CENTER
                textView.setTextColor(Color.WHITE)
                textView.textSize = 14f
                return view
            }

            override fun getDropDownView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                val view = super.getDropDownView(position, convertView, parent)
                val textView = view as TextView
                textView.gravity = Gravity.CENTER
                textView.setTextColor(Color.WHITE)
                textView.textSize = 14f
                return view
            }
        }
        adapter.setDropDownViewResource(R.layout.item_speed)
        spinnerSpeed.adapter = adapter
        spinnerSpeed.setSelection(2)
// ==========删掉原来popupBackground这一行！==========

        spinnerSpeed.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val newSpeed = speedList[position]
                setPlaySpeed(newSpeed)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
// =================================================================
        // 点击"今日推荐"卡片 -> 直接播放推荐的歌
        findViewById<android.widget.LinearLayout>(R.id.llDaily).setOnClickListener {
            if (dailyIndex in songList.indices) {
                playSong(dailyIndex)
            } else {
                Toast.makeText(this, "还没有歌曲，无法推荐", Toast.LENGTH_SHORT).show()
            }
        }

        // 播放 / 暂停
        btnPlay.setOnClickListener { playOrPause() }
        // 上一首
        findViewById<ImageButton>(R.id.btnPrev).setOnClickListener { previousSong() }
        // 下一首
        findViewById<ImageButton>(R.id.btnNext).setOnClickListener { nextSong() }
        // 倒退 10 秒
        findViewById<ImageButton>(R.id.btnRewind).setOnClickListener { seekTo(-10000) }
        // 快进 10 秒
        findViewById<ImageButton>(R.id.btnForward).setOnClickListener { seekTo(10000) }
        // 切换播放模式
        btnMode.setOnClickListener { changePlayMode() }

        // 点击列表中的某一项 -> 播放对应歌曲
        listView.setOnItemClickListener { _, _, position, _ -> playSong(position) }

        // 拖动进度条 -> 跳转到指定位置
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                // 用户拖动时同步刷新时间文字
                if (fromUser) tvTime.text = formatTime(progress.toLong())
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}

            override fun onStopTrackingTouch(sb: SeekBar?) {
                // 松手后让播放器跳到该位置
                mediaPlayer?.seekTo(sb?.progress ?: 0)
            }
        })

        // 点击歌词区域：如果还没开启"所有文件访问"，跳到系统设置去开启
        tvLyric.setOnClickListener { openAllFilesAccess() }

        updateModeText()
    }

    // ========== 第一步：申请权限 ==========

    /**
     * 申请读取音频权限。
     * Android 13（API 33）及以上用 READ_MEDIA_AUDIO；
     * Android 12 及以下用 READ_EXTERNAL_STORAGE。
     */
    private fun requestAudioPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_AUDIO
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            scanSongs()   // 已有权限，直接扫描
        } else {
            requestPermissions(arrayOf(permission), 100)
        }
    }

    /** 权限申请结果回调：用户点了"允许"就开始扫描，点了"拒绝"就提示 */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scanSongs()
                // 歌词文件不在媒体库管辖内，需要"所有文件访问"权限；没开启时提示
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                    Toast.makeText(this, "提示：如需显示歌词，点歌词区域开启\"所有文件访问\"", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "没有权限，无法读取手机中的歌曲", Toast.LENGTH_LONG).show()
            }
        }
    }

    /** 跳转到系统设置，开启本 App 的"所有文件访问"权限（读取 .lrc 歌词用） */
    private fun openAllFilesAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            Toast.makeText(this, "文件访问已开启，重新点击歌曲播放即可显示歌词", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }else{
                Toast.makeText(this,"当前Android版本不支持全部文件访问",Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
        }
    }

    // ========== 第二步：扫描歌曲 ==========

    /**
     * 扫描手机存储中的音乐文件（MP3/AMR/AAC/OGG 等），加入播放列表。
     * MediaStore 是 Android 系统自带的媒体数据库，App 通过它查询手机里的音频。
     */
    private fun scanSongs() {
        songList.clear()
        currentPlayPosition = -1
        songAdapter = null

        // 要查询的字段：编号、歌名、歌手、时长、文件路径
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
        )

        // 查询所有音频文件（不过滤 IS_MUSIC，保证 WAV 等格式也能被扫到）
        val cursor: Cursor? = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, null, null, null
        )

        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val title = it.getString(titleCol) ?: "未知歌曲"
                val artist = it.getString(artistCol) ?: "未知歌手"
                val duration = it.getLong(durCol)
                val dataPath = it.getString(dataCol) ?: ""
                // 用 ContentUri 定位文件（Android 10 以上推荐用法）
                val uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                )
                songList.add(Song(title, artist, uri, duration, dataPath))
            }
        }

        showSongList()
        updateDailyRecommend()   // 扫描完顺便更新"今日推荐"

        if (songList.isEmpty()) {
            Toast.makeText(this, "手机里没有找到歌曲，请先往手机里放几首音乐", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * 每日推歌：用"当天日期"作为随机种子，从歌曲列表里选一首。
     * 效果：同一天内固定推荐同一首；到了第二天日期变了，推荐的歌自动换。
     */
    private fun updateDailyRecommend() {
        if (songList.isEmpty()) {
            tvDailySong.text = "手机里还没有歌曲"
            dailyIndex = -1
            return
        }
        // 把日期拼成一个数字，例如 2026-09-10 -> 20260910，作为随机种子
        val cal = Calendar.getInstance()
        val seed = cal.get(Calendar.YEAR) * 10000L +
                (cal.get(Calendar.MONTH) + 1) * 100L +
                cal.get(Calendar.DAY_OF_MONTH)
        dailyIndex = Random(seed).nextInt(songList.size)
        val song = songList[dailyIndex]
        tvDailySong.text = "${song.title} · ${song.artist}"
    }

    /** 把歌曲列表显示到 ListView 上（自定义卡片样式，歌名 + 歌手/时长） */
    private fun showSongList() {
        val data = songList.map {
            mapOf("title" to it.title, "info" to "${it.artist} · ${formatTime(it.duration)}")
        }
        songAdapter = object : SimpleAdapter(
            this,
            data,
            R.layout.item_song,
            arrayOf("title", "info"),
            intArrayOf(R.id.tvName, R.id.tvInfo)
        ) {
            override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
                val view = super.getView(position, convertView, parent)
                val tvName: android.widget.TextView = view.findViewById(R.id.tvName)
                val tvInfo: android.widget.TextView = view.findViewById(R.id.tvInfo)

                if (position == currentPlayPosition) {
                    tvName.setTextColor(0xFF2196F3.toInt())
                    tvName.textSize = 18f
                    tvName.typeface = android.graphics.Typeface.DEFAULT_BOLD
                    tvInfo.setTextColor(0xFF42A5F5.toInt())
                    view.setBackgroundColor(0xFFE3F2FD.toInt())
                } else {
                    tvName.setTextColor(0xFF000000.toInt())
                    tvName.textSize = 16f
                    tvName.typeface = android.graphics.Typeface.DEFAULT
                    tvInfo.setTextColor(0xFF666666.toInt())
                    view.setBackgroundColor(0xFFFFFFFF.toInt())
                }
                return view
            }
        }
        listView.adapter = songAdapter
    }
    // ========== 第三步：播放控制 ==========

    /**
     * 播放指定位置的歌曲。
     * 流程：创建 MediaPlayer -> 设置歌曲数据源 -> 异步准备 -> 准备完成开始播放。
     */
    private fun playSong(index: Int) {
        currentPlayPosition = index
        if (songList.isEmpty()) return
        currentIndex = index
        val song = songList[index]

        try {
            mediaPlayer?.release()

            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@MainActivity, song.uri)
                setOnPreparedListener { mp ->
                    // 切歌后自动应用上一次设置的倍速
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                        val params = mp.playbackParams
                        params.speed = currentSpeed
                        mp.playbackParams = params
                    }
                    mp.start()
                    seekBar.max = mp.duration
                    tvTitle.text = song.title
                    tvArtist.text = song.artist
                    btnPlay.setImageResource(android.R.drawable.ic_media_pause)
                    updateModeText()
                    updateProgress()
                }
                setOnCompletionListener { nextSong() }
                prepareAsync()
            }
            loadLyrics(song)
            // 刷新ListView，更新选中歌曲高亮
            songAdapter?.notifyDataSetChanged()
            listView.setSelection(index)
        } catch (e: Exception) {
            Toast.makeText(this, "播放失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /** 播放 / 暂停切换 */
    private fun playOrPause() {
        val mp = mediaPlayer ?: return
        if (mp.isPlaying) {
            mp.pause()
            btnPlay.setImageResource(android.R.drawable.ic_media_play)   // 按钮切回"播放"图标
        } else {
            mp.start()
            btnPlay.setImageResource(android.R.drawable.ic_media_pause)  // 按钮切为"暂停"图标
        }
    }

    /** 下一首：按当前播放模式决定切到哪首 */
    private fun nextSong() {
        if (songList.isEmpty()) return
        when (playMode) {
            1 -> playSong(currentIndex)                                     // 单曲循环：重播当前
            2 -> playSong(Random().nextInt(songList.size))                  // 随机播放
            else -> playSong((currentIndex + 1) % songList.size)            // 顺序循环
        }
    }

    /** 上一首 */
    private fun previousSong() {
        if (songList.isEmpty()) return
        if (playMode == 2) {
            playSong(Random().nextInt(songList.size))
        } else {
            playSong((currentIndex - 1 + songList.size) % songList.size)
        }
    }

    /** 快进 / 倒退：deltaMs > 0 快进，< 0 倒退，每次 10 秒 */
    private fun seekTo(deltaMs: Int) {
        val mp = mediaPlayer ?: return
        val target = (mp.currentPosition + deltaMs).coerceIn(0, mp.duration)
        mp.seekTo(target)
        tvTime.text = formatTime(target.toLong())
    }

    /** 切换播放模式：顺序循环 -> 单曲循环 -> 随机播放 -> 循环 */
    private fun changePlayMode() {
        playMode = (playMode + 1) % 3
        updateModeText()
        // 单曲循环时让系统自动循环当前这一首
        mediaPlayer?.isLooping = (playMode == 1)
    }

    /** 刷新播放模式按钮上的文字 */
    private fun updateModeText() {
        btnMode.text = when (playMode) {
            0 -> "模式：顺序循环"
            1 -> "模式：单曲循环"
            else -> "模式：随机播放"
        }
    }

    /**
     * 设置播放倍速
     * Android 6.0(API23)以上系统才支持PlaybackParams倍速
     * @param speed 播放速度 0.5f ~ 2.0f
     */
    private fun setPlaySpeed(speed: Float) {
        // 判断系统版本，低于Android M不支持倍速
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val mp = mediaPlayer ?: return
            try {
                // 每次都新建PlaybackParams实例，解决多次修改锁死的BUG
                val params = PlaybackParams()
                params.speed = speed
                mp.playbackParams = params
                // 保存当前倍速到全局变量，切歌复用
                currentSpeed = speed
                Toast.makeText(this, "倍速：${speed}x", Toast.LENGTH_SHORT).show()
            }catch (e:Exception){
                Toast.makeText(this,"当前歌曲不支持该倍速",Toast.LENGTH_SHORT).show()
            }
        }else{
            Toast.makeText(this,"系统版本过低，不支持倍速播放",Toast.LENGTH_SHORT).show()
        }
    }

    // ========== 歌词功能（可选加分项） ==========

    /**
     * 加载当前歌曲的歌词。
     * 歌词文件要求：与歌曲文件在同一目录、同名、扩展名 .lrc，例如
     *   歌曲：/Music/晴天.mp3
     *   歌词：/Music/晴天.lrc
     */
    private fun loadLyrics(song: Song) {
        lrcList.clear()
        try {
            val dataPath = song.dataPath
            if (dataPath.isNotEmpty()) {
                // 把歌曲扩展名换成 .lrc
                val dotIndex = dataPath.lastIndexOf('.')
                val lrcPath = if (dotIndex > 0)
                    dataPath.substring(0, dotIndex) + ".lrc"
                else
                    "$dataPath.lrc"

                val lrcFile = File(lrcPath)
                if (lrcFile.exists()) {
                    // 读取文件内容：优先 UTF-8，出现乱码就按 GBK 再解一次
                    val raw = lrcFile.readBytes()
                    val textUtf8 = String(raw, Charsets.UTF_8)
                    val text = if (textUtf8.contains('\uFFFD')) String(raw, Charset.forName("GBK"))
                    else textUtf8

                    lrcList.addAll(parseLrc(text))
                }
            }
        } catch (e: Exception) {
            // 读取失败（例如没开启文件访问权限）不影响播放
            lrcList.clear()
        }

        tvLyric.text = if (lrcList.isEmpty()) "暂无歌词（点此开启文件访问）" else ""
    }

    /**
     * 解析 LRC 歌词文本。
     * LRC 格式示例：[00:12.34]第一句歌词，每行前面是时间标签。
     * 返回按时间排序的列表：Pair(时间毫秒, 歌词文字)
     */
    private fun parseLrc(text: String): List<Pair<Long, String>> {
        val result = ArrayList<Pair<Long, String>>()
        // 匹配 [分:秒.百分秒] 或 [分:秒]
        val regex = Regex("\\[(\\d{1,2}):(\\d{1,2})(?:\\.(\\d{1,2}))?]")

        text.lineSequence().forEach { line ->
            val matches = regex.findAll(line)
            // 去掉时间标签后剩下的才是歌词文字
            val content = line.replace(regex, "").trim()
            if (content.isEmpty()) return@forEach

            for (m in matches) {
                val minute = m.groupValues[1].toLong()
                val second = m.groupValues[2].toLong()
                // 百分秒 -> 毫秒（xx 是百分秒，0.01 秒 = 10 毫秒）
                val frac = m.groupValues[3]
                val ms = if (frac.isEmpty()) 0L else frac.padEnd(2, '0').take(2).toLong() * 10
                val time = minute * 60_000 + second * 1000 + ms
                result.add(time to content)
            }
        }
        return result.sortedBy { it.first }
    }

    /** 根据当前播放进度，找出应该显示的歌词行 */
    private fun getCurrentLyric(position: Long): String {
        var current = ""
        for ((time, text) in lrcList) {
            if (time <= position) current = text else break
        }
        return current
    }

    // ========== 进度条刷新 ==========

    /**
     * 每隔 1 秒把播放器的当前进度同步到进度条和时间文字上，
     * 同时刷新歌词显示。
     * 用 Handler.postDelayed 实现循环刷新。
     */
    private fun updateProgress() {
        handler.post(object : Runnable {
            override fun run() {
                val mp = mediaPlayer ?: return
                if (mp.isPlaying) {
                    val position = mp.currentPosition
                    seekBar.progress = position
                    tvTime.text = "${formatTime(position.toLong())} / " +
                            formatTime(mp.duration.toLong())
                    // 有歌词就同步显示当前句
                    if (lrcList.isNotEmpty()) {
                        tvLyric.text = getCurrentLyric(position.toLong())
                    }
                }
                handler.postDelayed(this, 1000)   // 1 秒后再刷新一次
            }
        })
    }

    /** 把毫秒数格式化成"分:秒"，如 125000ms -> 02:05 */
    private fun formatTime(ms: Long): String {
        val totalSec = ms / 1000
        return String.format("%02d:%02d", totalSec / 60, totalSec % 60)
    }

    /** 界面销毁时释放播放器、停止定时器，防止内存泄漏 */
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.release()
        mediaPlayer = null
    }
}