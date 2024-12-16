package net.optionfactory.jetbrains.ansivault.crypto.decoders

object CypherFactory {

    fun getCypher(cypherName: String): CypherInterface {
        if (cypherName == CypherAES.CYPHER_ID) {
            return CypherAES()
        }

        if (cypherName == CypherAES256.CYPHER_ID) {
            return CypherAES256()
        }

        throw IllegalArgumentException("Unsupported Cypher '$cypherName'")
    }
}
