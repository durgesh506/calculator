package com.sangyan.calculatorapp.documents

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.sangyan.calculatorapp.MainActivity2
import com.sangyan.calculatorapp.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class DocumentActivity : AppCompatActivity() {

    private val PICK_PDF_REQUEST = 1
    private lateinit var selectedDocumentUri: Uri
    private val documents: MutableList<Document> = mutableListOf()
    private lateinit var storageReference: StorageReference
    private lateinit var documentAdapter: DocumentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_document)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)

        toolbar.setNavigationOnClickListener(View.OnClickListener {
            val main = Intent(applicationContext, MainActivity2::class.java)
            startActivity(main)
        })



        //val storage = FirebaseStorage.getInstance()
        storageReference = FirebaseStorage.getInstance().getReference().child("documents")

        val documentRecyclerView: RecyclerView = findViewById(R.id.documentRecyclerView)
        documentAdapter = DocumentAdapter(this,documents)
        documentRecyclerView.adapter = documentAdapter
        documentRecyclerView.layoutManager = LinearLayoutManager(this)

        // Fetch documents from Firebase Storage
        fetchDocuments()

        val selectDocumentButton: TextView = findViewById(R.id.selectDocumentButton)
        selectDocumentButton.setOnClickListener {
            openFileChooser()
        }




    }

    private fun openFileChooser() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.type = "application/pdf" // You can also use "*/*" to allow all file types
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(intent, PICK_PDF_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_PDF_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedDocumentUri = data.data!!
            val documentName = getOriginalFileName(selectedDocumentUri)

           // val documentName = "Document_" + System.currentTimeMillis() + ".pdf"
            uploadDocumentToFirebaseStorage(documentName, selectedDocumentUri)
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

    private fun fetchDocuments() {
        storageReference.listAll()
            .addOnSuccessListener { listResult ->
                GlobalScope.launch(Dispatchers.IO) {
                    for (item in listResult.items) {
                        val downloadUrl = item.downloadUrl.await().toString()
                        val document = Document(item.name, item.name, Uri.parse(downloadUrl), downloadUrl)
                        withContext(Dispatchers.Main) {
                            documents.add(document)
                            documentAdapter.notifyDataSetChanged()
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                // Handle failure to fetch documents
                // ...
            }
    }

    private fun uploadDocumentToFirebaseStorage(documentName: String, documentUri: Uri) {
        
        val fileRef = storageReference.child(documentName)
        val uploadTask = fileRef.putFile(documentUri)

        val processDialog = ProgressDialog(this@DocumentActivity)
        processDialog.setMessage("Document Uploading")
        processDialog.setCancelable(false)
        processDialog.show()

        uploadTask.addOnSuccessListener {
            fileRef.downloadUrl.addOnSuccessListener { downloadUri ->
                val downloadUrl = downloadUri.toString()
                val documentId = fileRef.name // You can use the document name as the ID
                val document = Document(documentId, documentName, documentUri, downloadUrl)
                documents.add(document)
                documentAdapter.notifyDataSetChanged()
                processDialog.dismiss()
            }.addOnFailureListener { e ->
                // Handle any errors that occurred during getting download URL
                processDialog.dismiss()
            }
        }.addOnFailureListener { e ->
            processDialog.dismiss()
            // Handle upload failures
        }
            .addOnProgressListener { taskSnapshot -> //displaying the upload progress
                val progress =
                    100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount
                processDialog.setMessage("Uploaded " + progress.toInt() + "%...")
            }
    }


}
