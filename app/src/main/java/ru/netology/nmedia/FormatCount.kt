package ru.netology.nmedia

fun Long.formatCount(): String {
    return when {
        this < 1_000 -> this.toString()

        this < 10_000 -> {
            val thousands = this / 1_000
            val hundreds = (this % 1_000) / 100
            if (hundreds == 0L) "${thousands}K" else "${thousands}.${hundreds}K"
        }

        this < 1_000_000 -> {
            "${this / 1_000}K"
        }

        this < 10_000_000 -> {
            val millions = this / 1_000_000
            val hundredThousands = (this % 1_000_000) / 100_000
            if (hundredThousands == 0L) "${millions}M" else "${millions}.${hundredThousands}M"
        }

        else -> {
            "${this / 1_000_000}M"
        }
    }
}