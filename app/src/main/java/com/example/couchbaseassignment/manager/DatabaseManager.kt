package com.example.couchbaseassignment.manager

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.couchbase.lite.*
import com.example.couchbaseassignment.model.ErrorModel
import com.example.couchbaseassignment.model.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Instant
import java.net.URI

class DatabaseManager private constructor(private val context: Context) {
    companion object {
        @Volatile
        private var INSTANCE: DatabaseManager? = null
        private const val TAG = "DatabaseManager"
        
        fun getInstance(context: Context): DatabaseManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DatabaseManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private var database: Database? = null
    private var collection: com.couchbase.lite.Collection? = null
    private var replicator: Replicator? = null
    private var lastQuery: Query? = null
    private var lastQueryToken: ListenerToken? = null
    
    private val _notesFlow = MutableStateFlow<List<Note>>(emptyList())
    val notesFlow: StateFlow<List<Note>> = _notesFlow.asStateFlow()
    
    init {
        CouchbaseLite.init(context)
        Database.log.console.level = LogLevel.INFO
        initializeDatabase()
    }
    
    private fun initializeDatabase() {
        try {
            database = Database("notes-assignment")
            collection = database?.defaultCollection
            val indexConfig = FullTextIndexConfiguration("title")
            collection?.createIndex("titleIndex", indexConfig)
            getStartedWithReplication(true)
            queryElements()
        } catch (e: Exception) {
            ErrorManager.showError(
                ErrorModel(
                    title = "Database Error",
                    message = "Failed to initialize database: ${e.localizedMessage}",
                    closable = true
                )
            )
        }
    }
    
    private fun startListeningForChanges(query: Query) {
        lastQuery = query
        lastQueryToken = query.addChangeListener { change ->
            val results = change.results
            if (results != null) {
                val notes = mutableListOf<Note>()
                
                for (result in results) {
                    try {
                        val coll = collection ?: continue
                        val dict = result.getDictionary(coll.name)
                        if (dict != null) {
                            val title = dict.getString("title")
                            val content = dict.getString("content")
                            val type = dict.getString("type")
                            val id = dict.getInt("id")
                            val createdAt = dict.getDouble("createdAt")
                            
                            if (title != null && content != null && type == "note") {
                                notes.add(
                                    Note(
                                        id = id,
                                        title = title,
                                        content = content,
                                        createdAt = Instant.fromEpochSeconds(createdAt.toLong())
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        ErrorManager.showError(
                            ErrorModel(
                                title = "Data Parsing Error",
                                message = "Failed to parse note data: ${e.localizedMessage}",
                                closable = true
                            )
                        )
                    }
                }
                
                _notesFlow.value = notes
            }
        }
    }
    
    private fun stopListeningForChanges() {
        lastQueryToken?.let { token ->
            lastQuery?.removeChangeListener(token)
        }
    }

    fun queryElements(descending: Boolean = false, textSearch: String? = null) {
        val coll = collection ?: return

        stopListeningForChanges()

        try {
            val queryBuilder = QueryBuilder.select(SelectResult.all())
                .from(DataSource.collection(coll))

            val whereClause = if (!textSearch.isNullOrEmpty()) {
                Expression.property("type").equalTo(Expression.string("note"))
                    .and(FullTextExpression.index("titleIndex").match("$textSearch*"))
            } else {
                Expression.property("type").equalTo(Expression.string("note"))
            }

            val query = queryBuilder
                .where(whereClause)
                .orderBy(
                    if (descending) {
                        Ordering.property("createdAt").descending()
                    } else {
                        Ordering.property("createdAt").ascending()
                    }
                )

            val results = query.execute()
            startListeningForChanges(query)
        } catch (e: Exception) {
            ErrorManager.showError(
                ErrorModel(
                    title = "Query Error",
                    message = "Failed to query notes: ${e.localizedMessage}",
                    closable = true
                )
            )
        }
    }

    fun addNewElement(note: Note) {
        val coll = collection ?: return
        
        try {
            val doc = MutableDocument()
            doc.setString("type", "note")
            doc.setInt("id", note.id)
            doc.setString("title", note.title)
            doc.setString("content", note.content)
            doc.setDouble("createdAt", note.createdAt.epochSeconds.toDouble())
            
            coll.save(doc)
        } catch (e: Exception) {
            ErrorManager.showError(
                ErrorModel(
                    title = "Save Error",
                    message = "Failed to save note: ${e.localizedMessage}",
                    closable = true
                )
            )
        }
    }
    
    fun updateExistingElement(note: Note) {
        val db = database ?: return
        val coll = collection ?: return
        
        try {
            val query = QueryBuilder.select(SelectResult.expression(Meta.id))
                .from(DataSource.collection(coll))
                .where(Expression.property("type").equalTo(Expression.string("note"))
                    .and(Expression.property("id").equalTo(Expression.intValue(note.id))))
            
            val results = query.execute()
            
            for (result in results) {
                val docId = result.getString(0)
                if (docId != null) {
                    val doc = coll.getDocument(docId)
                    if (doc != null) {
                        val mutableDoc = doc.toMutable()
                        mutableDoc.setString("type", "note")
                        mutableDoc.setInt("id", note.id)
                        mutableDoc.setString("title", note.title)
                        mutableDoc.setString("content", note.content)
                        mutableDoc.setDouble("createdAt", note.createdAt.epochSeconds.toDouble())
                        
                        coll.save(mutableDoc)
                    }
                }
            }
        } catch (e: Exception) {
            ErrorManager.showError(
                ErrorModel(
                    title = "Update Error",
                    message = "Failed to update note: ${e.localizedMessage}",
                    closable = true
                )
            )
        }
    }
    
    fun deleteElement(note: Note) {
        val db = database ?: return
        val coll = collection ?: return
        
        try {
            val query = QueryBuilder.select(SelectResult.expression(Meta.id))
                .from(DataSource.collection(coll))
                .where(Expression.property("type").equalTo(Expression.string("note"))
                    .and(Expression.property("id").equalTo(Expression.intValue(note.id))))
            
            val results = query.execute()
            
            for (result in results) {
                val docId = result.getString(0)
                if (docId != null) {
                    val doc = coll.getDocument(docId)
                    if (doc != null) {
                        coll.delete(doc)
                    }
                }
            }
        } catch (e: Exception) {
            ErrorManager.showError(
                ErrorModel(
                    title = "Delete Error",
                    message = "Failed to delete note: ${e.localizedMessage}",
                    closable = true
                )
            )
        }
    }
    
    fun deleteElementWithName(name: String) {
        val db = database ?: return
        val coll = collection ?: return
        
        try {
            val query = QueryBuilder.select(SelectResult.expression(Meta.id))
                .from(DataSource.collection(coll))
                .where(Expression.property("type").equalTo(Expression.string("note"))
                    .and(Expression.property("title").equalTo(Expression.string(name))))
            
            val results = query.execute()
            
            for (result in results) {
                val docId = result.getString(0)
                if (docId != null) {
                    val doc = coll.getDocument(docId)
                    if (doc != null) {
                        coll.delete(doc)
                    }
                }
            }
        } catch (e: Exception) {
            ErrorManager.showError(
                ErrorModel(
                    title = "Delete Error",
                    message = "Failed to delete note by name: ${e.localizedMessage}",
                    closable = true
                )
            )
        }
    }
    
    private fun getStartedWithReplication(replication: Boolean) {
        val config = ConfigurationManager.getConfiguration()
        if (config == null) {
            ErrorManager.showError(
                ErrorModel(
                    title = "Configuration Error",
                    message = "Invalid Capella URL or credentials.",
                    closable = true
                )
            )
            return
        }
        
        if (replication) {
            val coll = collection ?: return
            
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            val hasInternet = networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            
            try {
                val target = URLEndpoint(config.capellaEndpointURL)
                val replConfig = ReplicatorConfiguration(target)
                replConfig.addCollection(coll, null)
                replConfig.type = ReplicatorType.PUSH_AND_PULL
                replConfig.isContinuous = true
                replConfig.heartbeat = 60
                replConfig.maxAttempts = 15
                replConfig.maxAttemptWaitTime = 180

                val authenticator = BasicAuthenticator(config.username, config.password.toCharArray())
                replConfig.authenticator = authenticator
                
                replicator = Replicator(replConfig)
                
                replicator?.addChangeListener { change ->
                    val error = change.status.error
                    if (error != null) {
                        ErrorManager.showError(
                            ErrorModel(
                                title = "Sync Error",
                                message = "Failed to sync with Capella: ${error.localizedMessage} (Code: ${error.code})",
                                closable = true
                            )
                        )
                    }
                }
                
                replicator?.start()
                
            } catch (e: Exception) {
                ErrorManager.showError(
                    ErrorModel(
                        title = "Sync Setup Error",
                        message = "Failed to setup sync: ${e.localizedMessage}",
                        closable = true
                    )
                )
            }
        }
    }
    
    fun cleanup() {
        stopListeningForChanges()
        replicator?.stop()
        database?.close()
    }
} 