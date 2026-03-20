//package com.example.gyroquiz3
//
//import androidx.compose.ui.input.pointer.HistoricalChange
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//import org.json.JSONArray
//import java.net.HttpURLConnection
//import java.net.URL
//
//object API{
//    private const val api = "";
//    suspend fun loadQuestion():List<Question> = withContext(Dispatchers.IO){
//        val conn = URL(api).openConnection() as HttpURLConnection
//        conn.requestMethod = "GET"
//        conn.connect()
//        val jsonArray = JSONArray(conn.inputStream.bufferedReader().readText())
//        List(jsonArray.length()){i->
//            val obj = jsonArray.getJSONObject(i)
//            Question(
//                obj.getString("title"),
//                List(obj.getJSONArray("options").length()){j->
//                    obj.getJSONArray("options").getString(j)
//                },
//                obj.getInt("answer")
//            )
//        }
//    }
//}