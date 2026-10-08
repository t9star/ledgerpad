package jp.tpp.t9s.ledgerpad.util

/**
 * Lightweight arithmetic expression evaluator for the on-screen keypad.
 * Supports +, -, × (*), ÷ (/) and decimal points.
 */
object Calc {

    fun evaluate(expression: String): Double? {
        val tokens = tokenize(expression) ?: return null
        if (tokens.isEmpty()) return null

        // Shunting-yard or simple precedence evaluation
        return try {
            evalTokens(tokens)
        } catch (_: Exception) {
            null
        }
    }

    private fun tokenize(expr: String): List<String>? {
        val s = expr.replace("×", "*").replace("÷", "/").replace(" ", "")
        if (s.isEmpty()) return emptyList()

        val list = mutableListOf<String>()
        var currentNum = StringBuilder()

        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c.isDigit() || c == '.') {
                currentNum.append(c)
            } else if (c in listOf('+', '-', '*', '/')) {
                if (currentNum.isNotEmpty()) {
                    list.add(currentNum.toString())
                    currentNum = StringBuilder()
                } else if (c == '-' && (list.isEmpty() || list.last() in listOf("+", "-", "*", "/"))) {
                    // Unary minus
                    currentNum.append(c)
                    i++
                    continue
                }
                list.add(c.toString())
            } else {
                return null // invalid char
            }
            i++
        }
        if (currentNum.isNotEmpty()) {
            list.add(currentNum.toString())
        }
        return list
    }

    private fun evalTokens(tokens: List<String>): Double {
        // Step 1: Multiply and divide
        val pass1 = mutableListOf<String>()
        var idx = 0
        while (idx < tokens.size) {
            val token = tokens[idx]
            if (token == "*" || token == "/") {
                val prev = pass1.removeAt(pass1.size - 1).toDouble()
                val next = tokens[++idx].toDouble()
                val res = if (token == "*") prev * next else (if (next != 0.0) prev / next else 0.0)
                pass1.add(res.toString())
            } else {
                pass1.add(token)
            }
            idx++
        }

        // Step 2: Add and subtract
        var total = pass1[0].toDouble()
        idx = 1
        while (idx < pass1.size) {
            val op = pass1[idx]
            val next = pass1[++idx].toDouble()
            if (op == "+") total += next else if (op == "-") total -= next
            idx++
        }
        return total
    }
}
