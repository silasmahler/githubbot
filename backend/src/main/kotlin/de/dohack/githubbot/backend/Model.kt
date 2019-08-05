package de.dohack.githubbot.backend

data class User(val id: Long, val name: String, val password: String)

data class Organization(val id: Long)