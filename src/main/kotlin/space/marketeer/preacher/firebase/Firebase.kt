package space.marketeer.preacher.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import java.io.FileInputStream

fun initFirebase(filePath: String) {
    val serviceAccount = FileInputStream(filePath)

    val options = FirebaseOptions.Builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .build()

    FirebaseApp.initializeApp(options)
}