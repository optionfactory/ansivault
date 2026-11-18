package net.optionfactory.jetbrains.ansivault.crypto.decoders

import java.io.IOException

/** The HMAC of the vault does not match: the password is wrong (or the vault was tampered with). */
class WrongPasswordException(message: String) : IOException(message)
