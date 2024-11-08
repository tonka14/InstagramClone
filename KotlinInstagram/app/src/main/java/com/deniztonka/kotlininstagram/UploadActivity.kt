package com.deniztonka.kotlininstagram

import android.content.pm.PackageManager
import android.Manifest
import android.provider.MediaStore
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.text
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import com.deniztonka.kotlininstagram.ui.theme.KotlinInstagramTheme
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import java.util.UUID

class UploadActivity : ComponentActivity() {
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var auth : FirebaseAuth
    private lateinit var storage : FirebaseStorage
    private lateinit var firestore : FirebaseFirestore
    var selectedPicture : Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activityupload)
        enableEdgeToEdge()

        auth = Firebase.auth
        firestore = Firebase.firestore
        storage = Firebase.storage

    }

    fun upload (view : View) {

        //unique ID

        val uuid = UUID.randomUUID()
        val imagename = "$uuid.jpg"

        val reference = storage.reference
        val imagereference = reference.child("images").child(imagename)
        if (selectedPicture != null) {
            imagereference.putFile(selectedPicture!!).addOnSuccessListener { }
                .addOnFailureListener {
                }

            val uploadPictureReference = storage.reference.child("images").child(imagename)
            uploadPictureReference.downloadUrl.addOnSuccessListener {
                val downloadUrl = it.toString()
                val commentEditText = findViewById<EditText>(R.id.commentText)

                val postMap = hashMapOf<String, Any>()
                postMap.put("downloadUrl",downloadUrl)
                postMap.put("userEmail",auth.currentUser!!.email!!)
                postMap.put("comment", commentEditText.text.toString())
                postMap.put("date",Timestamp.now())

                firestore.collection("Posts").add(postMap).addOnSuccessListener {
                    finish()
                }.addOnFailureListener {  }


            }
        }
    }

    fun selectImage(view: View) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)){
                Snackbar.make(view,"Permission need for Galery", Snackbar.LENGTH_INDEFINITE).setAction("Give Permission")
                {
                    //request perm
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }.show()
            }else
            {
                //request perm
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }else
        {
            val intentToGallery= Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            activityResultLauncher.launch(intentToGallery)
            //start activity for result
        }


    }
    private fun registerLauncher()
    {
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result -> if (result.resultCode == RESULT_OK){
                val intentfromResult = result.data
            if (intentfromResult != null){
                selectedPicture = intentfromResult.data
                selectedPicture?.let {
                    val imageView = findViewById<ImageView>(R.id.imageView)
                    imageView.setImageURI(it)
                }
            }
        }
        }

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            result ->
            if (result) {
                //prem granted
                val intentToGallery= Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }else {
                //perm denied
                Toast.makeText(this@UploadActivity, "Permission Needed!", Toast.LENGTH_LONG).show()
            }
        }
    }
}

