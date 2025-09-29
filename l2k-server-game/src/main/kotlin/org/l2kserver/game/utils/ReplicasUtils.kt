package org.l2kserver.game.utils

private const val MY_TEXT_MISSING = "<html><body>{}:<br/> My text is missing!</body></html>"

fun getNoTextMessage(id: Int, name: String = "") = MY_TEXT_MISSING.replace("{}", "${name}_${id}")
