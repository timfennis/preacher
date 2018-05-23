package space.marketeer.preacher.arangodb

import com.arangodb.ArangoCollection

inline fun <reified T> ArangoCollection.getDocument(key: String): T {
    return this.getDocument(key, T::class.java)
}