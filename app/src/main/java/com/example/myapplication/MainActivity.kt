package com.example.myapplication
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Start Ktor server (not typical for Android apps)
        CoroutineScope(Dispatchers.IO).launch {
            startKtorServer()
        }
    }

    // Start Ktor Server
    private fun startKtorServer() {
        embeddedServer(Netty, port = 8080) {
            install(ContentNegotiation) {
                json(Json { prettyPrint = true })
            }

            routing {
                get("/properties") {
                    val type = call.request.queryParameters["type"]
                    val location = call.request.queryParameters["location"]

                    val properties = fetchPropertiesFromFirebase()

                    val filtered = properties.filter {
                        (type == null || it.propertyType == type) &&
                                (location == null || it.location == location)
                    }

                    call.respond(filtered)
                }
            }
        }.start(wait = true)
    }

    // Fetch Properties from Firestore
    private suspend fun fetchPropertiesFromFirebase(): List<Property> {
        val db = FirebaseFirestore.getInstance()
        val snapshot = db.collection("properties").get().await()

        return snapshot.documents.map { doc ->
            Property(
                id = doc.id.toIntOrNull() ?: 0,
                name = doc.getString("name") ?: "",
                location = doc.getString("location") ?: "",
                propertyType = doc.getString("propertyType") ?: "",
                price = doc.getDouble("price") ?: 0.0
            )
        }
    }

    // Firebase Auth & Firestore access if needed
    object FirebaseHelper {
        fun initialize(context: Context) {
            FirebaseApp.initializeApp(context)
        }

        fun getFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
        fun getAuth(): FirebaseAuth = FirebaseAuth.getInstance()
    }

    // Property Data Model
    @Serializable
    data class Property(
        val id: Int,
        val name: String,
        val location: String,
        val propertyType: String,
        val price: Double
    )
}
