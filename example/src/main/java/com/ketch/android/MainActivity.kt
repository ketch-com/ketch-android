package com.ketch.android

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.ketch.android.example.R
import com.ketch.android.repository.KetchRepository
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), RepositoryProvider {

    private lateinit var ketchRepository: KetchRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        addFragment(SetupFragment.newInstance(), false)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return false
    }

    fun addFragment(fragment: Fragment, addToBackstack: Boolean = true) {
        val name: String = fragment.javaClass.getSimpleName()
        if (addToBackstack) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.content, fragment, name)
                .addToBackStack(name)
                .commit()
        } else {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.content, fragment, name)
                .commit()
        }
    }

    override fun setRepository(repo: KetchRepository) {
        ketchRepository = repo
    }

    override fun getRepository(): KetchRepository = ketchRepository
}
