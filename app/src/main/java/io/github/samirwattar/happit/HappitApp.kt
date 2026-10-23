package io.github.samirwattar.happit

import android.app.Application
import android.content.Context
import io.github.samirwattar.happit.data.AppDatabase
import io.github.samirwattar.happit.data.Prefs
import io.github.samirwattar.happit.data.Repository
import io.github.samirwattar.happit.reminders.Reminders

class HappitApp : Application() {
    lateinit var db: AppDatabase
        private set
    lateinit var repo: Repository
        private set

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.create(this)
        repo = Repository(this, db, Prefs(this))
        Reminders.createChannel(this)
    }
}

val Context.repo: Repository get() = (applicationContext as HappitApp).repo
