package net.optionfactory.jetbrains.ansivault.crypto.decoders

import java.io.IOException

object CypherFactory {

    @Throws(IOException::class)
    fun getCypher(cypherName: String): CypherInterface {
        if (cypherName == CypherAES256.CYPHER_ID) {
            return CypherAES256()
        }
        throw IOException("Unsupported vault cypher '$cypherName'")
    }
}
