package dev.trooped.tvquickbars.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.trooped.tvquickbars.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dev.trooped.tvquickbars.persistence.SavedEntitiesManager
import dev.trooped.tvquickbars.ui.adapters.QuickBarSavedEntitiesSelectorAdapter

/**
 * QuickBarEntitySelectorActivity
 * Activity for selecting entities for a QuickBar (launched from the QuickBar editor).
 * @property recyclerView The RecyclerView for displaying saved entities.
 * @property emptyView A TextView to show when there are no saved entities.
 * @property saveButton A MaterialButton to save the selected entities.
 * @property searchInputLayout The TextInputLayout wrapping the search field.
 * @property searchInput A TextInputEditText to filter the entity list as the user types.
 * @property savedEntitiesManager A SavedEntitiesManager to manage saved entities.
 * @property selectedEntityIds A mutable set of selected entity IDs.
 */
class QuickBarEntitySelectorActivity : BaseActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var saveButton: MaterialButton
    private lateinit var searchInputLayout: TextInputLayout
    private lateinit var searchInput: TextInputEditText
    private lateinit var savedEntitiesManager: SavedEntitiesManager
    private var selectedEntityIds = mutableSetOf<String>()
    private var entitiesAdapter: QuickBarSavedEntitiesSelectorAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_quickbar_entity_selector)

            // Set up toolbar
            val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)

            toolbar.setNavigationOnClickListener {
                showDiscardChangesDialog()
            }
            makeToolbarNavIconFocusable(toolbar)
            setToolbarNavigationIconColor(toolbar, R.color.md_theme_onSurface)

            recyclerView = findViewById(R.id.saved_entities_recycler_view)
            emptyView = findViewById(R.id.empty_view)
            saveButton = findViewById(R.id.btn_save_selections)
            searchInputLayout = findViewById(R.id.search_input_layout)
            searchInput = findViewById(R.id.et_search_entities)

            savedEntitiesManager = SavedEntitiesManager(this)

            // Get the initially selected IDs from the intent
            val initiallySelectedIds =
                intent.getStringArrayListExtra("INITIAL_SELECTED_IDS")?.toSet() ?: emptySet()
            selectedEntityIds.addAll(initiallySelectedIds)

            setupRecyclerView()
            setupSearch()

            saveButton.setOnClickListener {
                val resultIntent = Intent().apply {
                    putStringArrayListExtra("SELECTED_IDS", ArrayList(selectedEntityIds))
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        } catch (e: Exception) {
            Log.e("EntitySelector", "Error in onCreate", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun showDiscardChangesDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Discard Changes")
            .setMessage("Are you sure you want to leave this screen? Changes will not be saved.")
            .setPositiveButton("Leave") { _, _ ->
                // User confirmed - leave without saving
                super.onBackPressed()
            }
            .setNegativeButton("Stay") { dialog, _ ->
                // User canceled - dismiss the dialog and stay on screen
                dialog.dismiss()
            }
            .show()
    }

    @SuppressLint("MissingSuperCall")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Show dialog first, super.onBackPressed() will be called if user confirms
        showDiscardChangesDialog()
    }

    private fun setupRecyclerView() {
        val savedEntities = savedEntitiesManager.loadEntities()

        if (savedEntities.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
            // Nothing to filter, so hide the search field as well
            searchInputLayout.visibility = View.GONE
            return
        }

        recyclerView.visibility = View.VISIBLE
        emptyView.visibility = View.GONE

        recyclerView.layoutManager = LinearLayoutManager(this)
        entitiesAdapter = QuickBarSavedEntitiesSelectorAdapter(
            savedEntities,
            selectedEntityIds
        ) { entityId, isSelected ->
            // This is where we update the selection state
            if (isSelected) {
                selectedEntityIds.add(entityId)
            } else {
                selectedEntityIds.remove(entityId)
            }
            // Show selection count on FAB
            updateSelectionCount()
        }
        recyclerView.adapter = entitiesAdapter

        // Initial selection count
        updateSelectionCount()
    }

    /**
     * Wires the search field to the adapter so the list filters as the user types.
     * Matches on friendly name, custom name and entity id (case-insensitive).
     * Selection state is kept in [selectedEntityIds], so entities that are checked
     * and then filtered out of view remain checked.
     */
    private fun setupSearch() {
        searchInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val adapter = entitiesAdapter ?: return
                adapter.filter(s?.toString().orEmpty())

                // Show a "no results" message when the filter matches nothing,
                // but keep the search field visible so the user can clear it.
                if (adapter.itemCount == 0) {
                    recyclerView.visibility = View.GONE
                    emptyView.text = getString(R.string.entity_selector_search_no_results)
                    emptyView.visibility = View.VISIBLE
                } else {
                    recyclerView.visibility = View.VISIBLE
                    emptyView.visibility = View.GONE
                }
            }
        })
    }

    private fun updateSelectionCount() {
        val count = selectedEntityIds.size
        saveButton.text = if (count > 0) "Save ($count Entities) " else "Save"
    }
}