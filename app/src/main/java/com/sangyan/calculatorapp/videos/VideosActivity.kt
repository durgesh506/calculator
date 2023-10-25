package com.sangyan.calculatorapp.videos

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.sangyan.calculatorapp.MainActivity2
import com.sangyan.calculatorapp.R
import com.sangyan.calculatorapp.databinding.ActivityVideosBinding

class VideosActivity : AppCompatActivity(), VideoItemClickListener {
    private lateinit var progressBar: ProgressBar
    private lateinit var binding: ActivityVideosBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var videoAdapter: VideoAdapter
    private lateinit var videoList: ArrayList<VideoModel>


    @SuppressLint("MissingInflatedId", "WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        videoAdapter = VideoAdapter(mutableListOf(),this,this) // Initialize with an empty list initially
        recyclerView.adapter = videoAdapter


        fetchVideoItemsFromFirebase()

        binding.toolbar.setNavigationOnClickListener(View.OnClickListener {
            val main = Intent(applicationContext, MainActivity2::class.java)
            startActivity(main)
        })


        binding.addVideo.setOnClickListener {
            // Open the gallery to select a video
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "video/*"
            startActivityForResult(intent, PICK_VIDEO_REQUEST)
        }
        
    }

    private fun fetchVideoItemsFromFirebase() {
        val storage = Firebase.storage
        val uid = FirebaseAuth.getInstance().uid ?: return

        val storageRef = FirebaseStorage.getInstance().getReference(uid).child("videos/") // Replace 'videos/' with your storage path

        val videoItems = mutableListOf<VideoModel>()

        storageRef.listAll().addOnSuccessListener { result ->
            for (item in result.items) {
                item.downloadUrl.addOnSuccessListener { uri ->
                    val videoUrl = uri.toString()
                    val videoName = item.name

                    val videoItem = VideoModel(videoUrl, videoName)
                    videoItems.add(videoItem)

                    // Update the adapter with the new video item
                    videoAdapter.updateData(videoItems)
                }
            }
        }.addOnFailureListener { exception ->
            // Handle errors during fetch operation
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_VIDEO_REQUEST && resultCode == RESULT_OK) {
            data?.data?.let { videoUri ->
                uploadVideoToFirebase(videoUri)
            }
        }
    }
    private fun uploadVideoToFirebase(videoUri: Uri) {
        // Get the Firebase Storage reference
        val storage = Firebase.storage
        val storageRef = storage.reference

        val originalFileName = getOriginalFileName(videoUri)
        // Create a reference to the video file in Firebase Storage
        val uid = FirebaseAuth.getInstance().uid ?: return

        val videoRef = FirebaseStorage.getInstance().getReference(uid).child("videos/$originalFileName")


            val processDialog = ProgressDialog(this@VideosActivity)
            processDialog.setMessage("Video Uploading")
            processDialog.setCancelable(false)
            processDialog.show()
        // Show progress bar during upload

        // Upload the video to Firebase Storage
        videoRef.putFile(videoUri)
            .addOnSuccessListener { taskSnapshot ->

                Toast.makeText(this@VideosActivity, "Video Upload Successfully", Toast.LENGTH_SHORT).show()
                processDialog.dismiss()
                refreshActivity()

            }
            .addOnFailureListener { exception ->
                Toast.makeText(this@VideosActivity, "Failed to Upload Video", Toast.LENGTH_SHORT).show()
                processDialog.dismiss()


            }
            .addOnProgressListener { taskSnapshot -> //displaying the upload progress
                val progress =
                    100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount
                processDialog.setMessage("Uploaded " + progress.toInt() + "%...")
            }

    }

    @SuppressLint("Range")
    private fun getOriginalFileName(uri: Uri?): String {
        // Use content resolver to get the original file name from the URI
        val cursor = contentResolver.query(uri!!, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val displayName = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                cursor.close()
                return displayName
            }
        }
        // If unable to get the original file name, generate a unique name or handle it as per your requirement
        return "video_${System.currentTimeMillis()}.mp4"
    }



    companion object {
        private const val PICK_VIDEO_REQUEST = 1
    }
    override fun onVideoItemClicked(videoUrl: String) {
        val intent = Intent(this, VideoPlayerActivity::class.java)
        intent.putExtra("VIDEO_URL", videoUrl)
        startActivity(intent)
    }
    private fun refreshActivity(){
        val mIntent = intent
        finish()
        startActivity(mIntent)
    }
}