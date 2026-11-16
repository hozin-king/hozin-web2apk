package com.hozinking.web2apk

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var rvProjects: RecyclerView
    private lateinit var tvEmpty: TextView
    private val projects = mutableListOf<Project>()

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        rvProjects = findViewById(R.id.rvProjects)
        tvEmpty = findViewById(R.id.tvEmpty)
        rvProjects.layoutManager = LinearLayoutManager(this)
        rvProjects.adapter = ProjectAdapter()
        findViewById<FloatingActionButton>(R.id.fabNew).setOnClickListener { showNewProjectDialog() }
        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        projects.clear()
        projects.addAll(ProjectStore.list(this))
        rvProjects.adapter?.notifyDataSetChanged()
        tvEmpty.visibility = if (projects.isEmpty()) View.VISIBLE else View.GONE
        rvProjects.visibility = if (projects.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showNewProjectDialog() {
        val et = EditText(this).apply { hint = getString(R.string.project_name_hint) }
        AlertDialog.Builder(this)
            .setTitle(R.string.new_project)
            .setView(et)
            .setPositiveButton(R.string.create) { _, _ ->
                val name = et.text.toString().trim().ifEmpty { "Proyek Baru" }
                val project = Project(name = name)
                ProjectStore.save(this, project)
                openBuilder(project.id)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun openBuilder(projectId: String) {
        startActivity(Intent(this, BuilderActivity::class.java).putExtra("project_id", projectId))
    }

    private inner class ProjectAdapter : RecyclerView.Adapter<ProjectAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvName: TextView = v.findViewById(R.id.tvName)
            val tvMeta: TextView = v.findViewById(R.id.tvMeta)
            val btnOpen: Button = v.findViewById(R.id.btnOpen)
            val btnDuplicate: Button = v.findViewById(R.id.btnDuplicate)
            val btnDelete: Button = v.findViewById(R.id.btnDelete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_project, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val p = projects[position]
            holder.tvName.text = p.name
            val date = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id")).format(Date(p.updatedAt))
            holder.tvMeta.text = "$date • " + getString(R.string.blocks_count, p.blocks.size)
            holder.btnOpen.setOnClickListener { openBuilder(p.id) }
            holder.itemView.setOnClickListener { openBuilder(p.id) }
            holder.btnDuplicate.setOnClickListener {
                ProjectStore.duplicate(this@MainActivity, p)
                refresh()
            }
            holder.btnDelete.setOnClickListener {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle(R.string.confirm_delete)
                    .setMessage(p.name)
                    .setPositiveButton(R.string.delete) { _, _ ->
                        ProjectStore.delete(this@MainActivity, p.id)
                        refresh()
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        }

        override fun getItemCount(): Int = projects.size
    }
}
