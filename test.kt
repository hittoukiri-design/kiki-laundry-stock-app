fun main() {
    val input = "12345678"
    val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
    println(bytes.joinToString("") { "%02x".format(it) })
}
