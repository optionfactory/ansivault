package net.optionfactory.jetbrains.ansivault.actions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YamlValueLineTest {

    @Test
    fun `parses a plain value`() {
        assertEquals(YamlValueLine(0, 10, "s3cret", null), YamlValueLine.parse("password: s3cret"))
    }

    @Test
    fun `parses an indented list item key with a comment`() {
        val line = "    - db_pass: s3cret value  # the db"
        assertEquals(YamlValueLine(6, 15, "s3cret value", "# the db"), YamlValueLine.parse(line))
    }

    @Test
    fun `keeps colons and hashes inside the value`() {
        assertEquals("http://host:8080/#a", YamlValueLine.parse("url: http://host:8080/#a")!!.value)
    }

    @Test
    fun `unquotes double and single quoted values`() {
        assertEquals("a \"b\"\n#c", YamlValueLine.parse("""key: "a \"b\"\n#c" # note""")!!.value)
        assertEquals("it's", YamlValueLine.parse("key: 'it''s'")!!.value)
        assertEquals("x", YamlValueLine.parse("\"quoted key\": x")!!.value)
    }

    @Test
    fun `ignores what is not a single line scalar`() {
        listOf(
            "key:",
            "key: ",
            "key: !vault |",
            "key: |",
            "key: >-",
            "key: &anchor x",
            "key: *alias",
            "key: [a, b]",
            "key: {a: b}",
            "key: \"unterminated",
            "key: 'x' trailing",
            "- just a list item",
            "# key: commented",
            "plain text",
        ).forEach { assertNull(it, YamlValueLine.parse(it)) }
    }
}
