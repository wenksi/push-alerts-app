package ch.wenksi.pushalerts

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.forEach
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import ch.wenksi.pushalerts.models.Project
import ch.wenksi.pushalerts.services.auth.SessionManager
import ch.wenksi.pushalerts.services.notifications.AppFirebaseMessagingService
import ch.wenksi.pushalerts.util.Events
import ch.wenksi.pushalerts.viewModels.ProjectsViewModel
import ch.wenksi.pushalerts.viewModels.TasksViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ch.wenksi.pushalerts.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseApp

// 0 - 1000 is reserved for project menu items.
const val MENU_ID_LOGOUT = 1001

/**
 * This activity contains everything except the login screens.
 * It configures the navigation drawer.
 * Listens to the projects live data and dynamically sets the navigation drawer items for each project.
 * Listens to the logoutRequest live data and finishes the activity.
 * Listens to the newNotification live data and displays a dialog.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val projectsViewModel: ProjectsViewModel by viewModels()
    private val tasksViewModel: TasksViewModel by viewModels()
    private val projects: ArrayList<Project> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        FirebaseApp.initializeApp(applicationContext)
        setContentView(binding.root)
        setSupportActionBar(binding.topAppBar)

        setupNavigation()
        projectsViewModel.getProjects(SessionManager.requireToken().uuid)
        Snackbar.make(
            binding.root,
            "Logged in as ${SessionManager.requireToken().email}",
            Snackbar.LENGTH_SHORT
        ).show()

        observeNotifications()
        observeErrors()
        observeLogoutRequest()
        observeProjects()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return NavigationUI.onNavDestinationSelected(
            item,
            findNavController(R.id.nav_host_fragment_content_main)
        )
                || super.onOptionsItemSelected(item)

    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return NavigationUI.navigateUp(navController, binding.drawerLayout)
    }

    private fun onClickMenuItem(menuItem: MenuItem): Boolean {
        binding.navigationView.menu.forEach { i ->
            i.isChecked = false
        }
        when (menuItem.itemId) {
            R.id.AboutFragment -> {
                findNavController(R.id.nav_host_fragment_content_main)
                    .navigate(R.id.action_MainFragment_to_AboutFragment)
            }
            MENU_ID_LOGOUT -> logout()
            else -> onClickProjectMenuItem(menuItem)
        }
        menuItem.isChecked = true
        binding.drawerLayout.close()
        return true
    }

    private fun addMenuItemsForProjects() {
        val menu = binding.navigationView.menu
        projects.forEach {
            menu.add(R.id.Projects, it.menuId, Menu.NONE, "Project ${it.name}")
        }
        menu.add(R.id.Actions, R.id.AboutFragment, Menu.NONE, "About")
            .setIcon(R.drawable.ic_baseline_info_24)
        menu.add(R.id.Actions, MENU_ID_LOGOUT, Menu.NONE, "Logout")
            .setIcon(R.drawable.ic_baseline_logout_24)
        menu.getItem(0).isChecked = true
        if (projects.isNotEmpty()) setAppBarTitle(projects.first().name)
        binding.navigationView.invalidate()
    }

    private fun observeProjects() {
        projectsViewModel.projects.observe(this) {
            projects.clear()
            projects.addAll(it)
            addMenuItemsForProjects()
            AppFirebaseMessagingService.subscribeToNotifications(projects)
        }
    }

    private fun observeNotifications() {
        Events.newNotification.observe(this) {
            MaterialAlertDialogBuilder(this@MainActivity)
                .setTitle(it.title)
                .setMessage(it.description)
                .setNeutralButton("Ok") { _, _ -> }
                .show()
            tasksViewModel.getTasks(projectsViewModel.selectedProjectUUID!!)
        }
    }

    private fun observeErrors() {
        tasksViewModel.error.observe(this) {
            Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
            logout()
        }
        projectsViewModel.error.observe(this) {
            Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
            logout()
        }
    }

    private fun observeLogoutRequest() {
        tasksViewModel.logoutRequest.observe(this) {
            logout()
        }
        projectsViewModel.logoutRequest.observe(this) {
            logout()
        }
    }

    private fun onClickProjectMenuItem(menuItem: MenuItem) {
        findNavController(R.id.nav_host_fragment_content_main)
            .navigate(R.id.action_Global_to_MainFragment)
        val project = projectsViewModel.getProject(menuItem.itemId)
        if (project != null) {
            setAppBarTitle(project.name)
            projectsViewModel.selectedProjectUUID = project.uuid
            tasksViewModel.getTasks(project.uuid)
        }
    }

    private fun logout() {
        SessionManager.clear()
        finish()
    }

    private fun setAppBarTitle(title: String) {
        binding.topAppBar.title = "Project $title"
    }

    private fun setupNavigation() {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.MainFragment, R.id.AboutFragment),
            binding.drawerLayout
        )
        binding.navigationView.setupWithNavController(navController)
        binding.topAppBar.setupWithNavController(navController, appBarConfiguration)
        binding.navigationView.setNavigationItemSelectedListener { i -> onClickMenuItem(i) }
    }
}