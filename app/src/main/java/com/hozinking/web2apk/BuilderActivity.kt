package com.hozinking.web2apk

import android.content.ClipData
import android.content.ClipDescription
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONObject
import java.io.File
import java.util.ArrayDeque
import java.util.Collections

class BuilderActivity : AppCompatActivity() {

    private lateinit var project: Project
    private lateinit var adapter: BlockAdapter
    private lateinit var rvCanvas: RecyclerView
    private lateinit var rvPalette: RecyclerView
    private lateinit var tvPaletteHint: TextView
    private lateinit var scrollProps: ScrollView
    private lateinit var tvPropsTitle: TextView
    private lateinit var tilText: TextInputLayout
    private lateinit var tilLink: TextInputLayout
    private lateinit var etText: TextInputEditText
    private lateinit var etLink: TextInputEditText
    private lateinit var rowSize: LinearLayout
    private lateinit var rowHeight: LinearLayout
    private lateinit var rowColor: LinearLayout
    private lateinit var colorRow: LinearLayout
    private lateinit var btnPickImage: Button
    private lateinit var btnUndo: Button
    private lateinit var btnRedo: Button
    private lateinit var btnPreview: Button
    private lateinit var btnExport: Button

    private var selectedPos = -1
    private var previewMode = false
    private var fillingProps = false
    private val undoStack = ArrayDeque<String>()
    private val redoStack = ArrayDeque<String>()

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val block = selectedBlock() ?: return@registerForActivityResult
        if (uri == null) return@registerForActivityResult
        pushUndo()
        val ext = contentResolver.getType(uri)?.let {
            MimeTypeMap.getSingleton().getExtensionFromMimeType(it)
        } ?: "jpg"
        val name = "img_" + System.currentTimeMillis() + "." + ext
        val dest = File(ProjectStore.assetsDir(this, project.id), name)
        runCatching {
            contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { input.copyTo(it) }
            }
        }
        block.imageName = name
        save()
        if (selectedPos >= 0) adapter.notifyItemChanged(selectedPos)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_builder)
        val projectId = intent.getStringExtra("project_id") ?: run { finish(); return }
        project = ProjectStore.load(this, projectId) ?: run { finish(); return }

        findViewById<TextView>(R.id.tvTitle).text = project.name
        rvCanvas = findViewById(R.id.rvCanvas)
        rvPalette = findViewById(R.id.rvPalette)
        tvPaletteHint = findViewById(R.id.tvPaletteHint)
        scrollProps = findViewById(R.id.scrollProps)
        tvPropsTitle = findViewById(R.id.tvPropsTitle)
        tilText = findViewById(R.id.tilText)
        tilLink = findViewById(R.id.tilLink)
        etText = findViewById(R.id.etText)
        etLink = findViewById(R.id.etLink)
        rowSize = findViewById(R.id.rowSize)
        rowHeight = findViewById(R.id.rowHeight)
        rowColor = findViewById(R.id.rowColor)
        colorRow = findViewById(R.id.colorRow)
        btnPickImage = findViewById(R.id.btnPickImage)
        btnUndo = findViewById(R.id.btnUndo)
        btnRedo = findViewById(R.id.btnRedo)
        btnPreview = findViewById(R.id.btnPreview)
        btnExport = findViewById(R.id.btnExport)

        // Canvas
        adapter = BlockAdapter(this, project.blocks, ProjectStore.assetsDir(this, project.id))
        adapter.onBlockClick = { pos -> selectBlock(pos) }
        adapter.onButtonClick = { block -> openLink(block.link) }
        rvCanvas.layoutManager = LinearLayoutManager(this)
        rvCanvas.adapter = adapter
        rvCanvas.setOnClickListener { deselect() }
        rvCanvas.setOnDragListener { _, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED ->
                    event.clipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true
                DragEvent.ACTION_DROP -> {
                    val name = event.clipData?.getItemAt(0)?.text?.toString()
                        ?: return@setOnDragListener false
                    val type = runCatching { BlockType.valueOf(name) }.getOrNull()
                        ?: return@setOnDragListener false
                    val child = rvCanvas.findChildViewUnder(event.x, event.y)
                    val pos = if (child != null) rvCanvas.getChildAdapterPosition(child)
                    else RecyclerView.NO_POSITION
                    addBlock(type, if (pos == RecyclerView.NO_POSITION) project.blocks.size else pos)
                    true
                }
                else -> true
            }
        }
        val touchHelper = ItemTouchHelper(object :
            ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(
                rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder
            ): Boolean {
                val from = vh.bindingAdapterPosition
                val to = target.bindingAdapterPosition
                if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false
                Collections.swap(project.blocks, from, to)
                adapter.notifyItemMoved(from, to)
                if (selectedPos == from) selectedPos = to
                else if (selectedPos == to) selectedPos = from
                adapter.selectedPosition = selectedPos
                return true
            }

            override fun onSelectedChanged(vh: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(vh, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) pushUndo()
            }

            override fun clearView(rv: RecyclerView, vh: RecyclerView.ViewHolder) {
                super.clearView(rv, vh)
                save()
                adapter.notifyDataSetChanged()
            }

            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {}
        })
        touchHelper.attachToRecyclerView(rvCanvas)

        // Palet
        rvPalette.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvPalette.adapter = PaletteAdapter()

        // Toolbar
        findViewById<Button>(R.id.btnBack).setOnClickListener { save(); finish() }
        btnUndo.setOnClickListener { undo() }
        btnRedo.setOnClickListener { redo() }
        btnPreview.setOnClickListener { togglePreview() }
        btnExport.setOnClickListener { exportZip() }

        // Panel properti
        etText.addTextChangedListener(simpleWatcher { s ->
            if (fillingProps) return@simpleWatcher
            selectedBlock()?.let { it.text = s }
        })
        etLink.addTextChangedListener(simpleWatcher { s ->
            if (fillingProps) return@simpleWatcher
            selectedBlock()?.let { it.link = s }
        })
        val rebindOnBlur = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && selectedPos >= 0) {
                save()
                adapter.notifyItemChanged(selectedPos)
            }
        }
        etText.onFocusChangeListener = rebindOnBlur
        etLink.onFocusChangeListener = rebindOnBlur
        findViewById<Button>(R.id.btnSizeS).setOnClickListener { applySize(0) }
        findViewById<Button>(R.id.btnSizeM).setOnClickListener { applySize(1) }
        findViewById<Button>(R.id.btnSizeL).setOnClickListener { applySize(2) }
        findViewById<Button>(R.id.btnHeightS).setOnClickListener { applyHeight(48) }
        findViewById<Button>(R.id.btnHeightM).setOnClickListener { applyHeight(96) }
        findViewById<Button>(R.id.btnHeightL).setOnClickListener { applyHeight(160) }
        btnPickImage.setOnClickListener { pickImage.launch("image/*") }
        findViewById<Button>(R.id.btnMoveUp).setOnClickListener { moveSelected(-1) }
        findViewById<Button>(R.id.btnMoveDown).setOnClickListener { moveSelected(1) }
        findViewById<Button>(R.id.btnDeleteBlock).setOnClickListener { deleteSelected() }
        buildColorRow()
        updateUndoButtons()
    }

    override fun onPause() {
        super.onPause()
        save()
    }

    // ---------- Proyek ----------

    private fun save() {
        ProjectStore.save(this, project)
    }

    private fun selectedBlock(): Block? =
        if (selectedPos in project.blocks.indices) project.blocks[selectedPos] else null

    private fun currentJson(): String = project.toJson().toString()

    private fun pushUndo() {
        undoStack.addLast(currentJson())
        if (undoStack.size > 50) undoStack.removeFirst()
        redoStack.clear()
        updateUndoButtons()
    }

    private fun restore(json: String) {
        val p = Project.fromJson(JSONObject(json))
        project.blocks = p.blocks
        project.name = p.name
        selectedPos = -1
        adapter.selectedPosition = -1
        scrollProps.visibility = View.GONE
        adapter.notifyDataSetChanged()
        save()
    }

    private fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.addLast(currentJson())
        restore(undoStack.removeLast())
        updateUndoButtons()
    }

    private fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.addLast(currentJson())
        restore(redoStack.removeLast())
        updateUndoButtons()
    }

    private fun updateUndoButtons() {
        btnUndo.isEnabled = undoStack.isNotEmpty()
        btnUndo.alpha = if (undoStack.isNotEmpty()) 1f else 0.4f
        btnRedo.isEnabled = redoStack.isNotEmpty()
        btnRedo.alpha = if (redoStack.isNotEmpty()) 1f else 0.4f
    }

    // ---------- Blok ----------

    private fun addBlock(type: BlockType, at: Int) {
        pushUndo()
        val b = Block(
            type = type,
            text = BlockType.defaultText(type),
            textSizeSp = BlockType.defaultSizeSp(type)
        )
        val pos = at.coerceIn(0, project.blocks.size)
        project.blocks.add(pos, b)
        adapter.notifyItemInserted(pos)
        rvCanvas.scrollToPosition(pos)
        save()
        selectBlock(pos)
    }

    private fun selectBlock(pos: Int) {
        if (pos !in project.blocks.indices) {
            deselect()
            return
        }
        val old = selectedPos
        selectedPos = pos
        adapter.selectedPosition = pos
        if (old >= 0 && old != pos) adapter.notifyItemChanged(old)
        adapter.notifyItemChanged(pos)
        fillProps(project.blocks[pos])
        scrollProps.visibility = View.VISIBLE
    }

    private fun deselect() {
        val old = selectedPos
        selectedPos = -1
        adapter.selectedPosition = -1
        if (old >= 0) adapter.notifyItemChanged(old)
        scrollProps.visibility = View.GONE
    }

    private fun moveSelected(d: Int) {
        val from = selectedPos
        val to = from + d
        if (from < 0 || to < 0 || to >= project.blocks.size) return
        pushUndo()
        Collections.swap(project.blocks, from, to)
        adapter.notifyItemMoved(from, to)
        selectedPos = to
        adapter.selectedPosition = to
        save()
    }

    private fun deleteSelected() {
        if (selectedPos !in project.blocks.indices) return
        pushUndo()
        project.blocks.removeAt(selectedPos)
        adapter.notifyItemRemoved(selectedPos)
        selectedPos = -1
        adapter.selectedPosition = -1
        scrollProps.visibility = View.GONE
        save()
    }

    // ---------- Panel properti ----------

    private fun fillProps(block: Block) {
        fillingProps = true
        tvPropsTitle.text = getString(R.string.properties) + " — " + block.type.label
        tilText.visibility = if (block.type in
            listOf(BlockType.JUDUL, BlockType.TEKS, BlockType.TOMBOL, BlockType.KARTU, BlockType.NAVBAR, BlockType.FOOTER)
        ) View.VISIBLE else View.GONE
        tilLink.visibility = if (block.type in
            listOf(BlockType.TOMBOL, BlockType.VIDEO, BlockType.NAVBAR)
        ) View.VISIBLE else View.GONE
        etLink.hint = when (block.type) {
            BlockType.TOMBOL -> getString(R.string.link_hint_button)
            BlockType.VIDEO -> getString(R.string.link_hint_video)
            else -> getString(R.string.link_hint_navbar)
        }
        rowSize.visibility = if (block.type in
            listOf(BlockType.JUDUL, BlockType.TEKS, BlockType.TOMBOL, BlockType.KARTU, BlockType.NAVBAR, BlockType.FOOTER)
        ) View.VISIBLE else View.GONE
        rowHeight.visibility = if (block.type == BlockType.SPASI) View.VISIBLE else View.GONE
        rowColor.visibility = if (block.type in
            listOf(BlockType.GAMBAR, BlockType.SPASI, BlockType.VIDEO)
        ) View.GONE else View.VISIBLE
        btnPickImage.visibility = if (block.type == BlockType.GAMBAR) View.VISIBLE else View.GONE
        etText.setText(block.text)
        etLink.setText(block.link)
        fillingProps = false
    }

    private fun sizeOptions(type: BlockType): Triple<Int, Int, Int> = when (type) {
        BlockType.JUDUL -> Triple(20, 28, 36)
        BlockType.TEKS -> Triple(14, 16, 20)
        BlockType.TOMBOL -> Triple(14, 16, 18)
        BlockType.KARTU -> Triple(14, 16, 20)
        BlockType.NAVBAR -> Triple(16, 20, 24)
        BlockType.FOOTER -> Triple(12, 14, 16)
        else -> Triple(14, 16, 20)
    }

    private fun applySize(which: Int) {
        val block = selectedBlock() ?: return
        pushUndo()
        val (s, m, l) = sizeOptions(block.type)
        block.textSizeSp = when (which) {
            0 -> s
            2 -> l
            else -> m
        }
        save()
        adapter.notifyItemChanged(selectedPos)
    }

    private fun applyHeight(dp: Int) {
        val block = selectedBlock() ?: return
        pushUndo()
        block.heightDp = dp
        save()
        adapter.notifyItemChanged(selectedPos)
    }

    private fun buildColorRow() {
        val presets = listOf(
            "#FFFFFF", "#2DD4BF", "#F59E0B", "#EF4444",
            "#60A5FA", "#A78BFA", "#1F2A33", "#12181D"
        )
        for (hex in presets) {
            val swatch = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply {
                    marginEnd = dp(8)
                }
                background = GradientDrawable().apply {
                    setColor(Color.parseColor(hex))
                    cornerRadius = dp(8).toFloat()
                    setStroke(dp(1), Color.parseColor("#33414D"))
                }
                tag = hex
                setOnClickListener {
                    val block = selectedBlock() ?: return@setOnClickListener
                    pushUndo()
                    val color = it.tag as String
                    when (block.type) {
                        BlockType.KARTU, BlockType.TOMBOL, BlockType.PEMBATAS ->
                            block.bgHex = color
                        else -> block.colorHex = color
                    }
                    save()
                    adapter.notifyItemChanged(selectedPos)
                }
            }
            colorRow.addView(swatch)
        }
    }

    // ---------- Pratinjau & export ----------

    private fun togglePreview() {
        previewMode = !previewMode
        adapter.previewMode = previewMode
        adapter.selectedPosition = -1
        selectedPos = -1
        scrollProps.visibility = View.GONE
        rvPalette.visibility = if (previewMode) View.GONE else View.VISIBLE
        tvPaletteHint.visibility = if (previewMode) View.GONE else View.VISIBLE
        btnExport.visibility = if (previewMode) View.GONE else View.VISIBLE
        btnUndo.visibility = if (previewMode) View.GONE else View.VISIBLE
        btnRedo.visibility = if (previewMode) View.GONE else View.VISIBLE
        btnPreview.text =
            if (previewMode) getString(R.string.edit) else getString(R.string.preview)
        adapter.notifyDataSetChanged()
    }

    private fun openLink(url: String) {
        if (url.isBlank()) return
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure {
            Toast.makeText(this, url, Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportZip() {
        Toast.makeText(this, R.string.exporting, Toast.LENGTH_SHORT).show()
        Thread {
            try {
                val zip = ZipExporter.exportZip(this, project)
                runOnUiThread {
                    val uri = FileProvider.getUriForFile(
                        this, "$packageName.fileprovider", zip
                    )
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/zip"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(intent, getString(R.string.export_done)))
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, R.string.export_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    // ---------- Util ----------

    private fun dp(v: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics
        ).toInt()

    private fun simpleWatcher(onText: (String) -> Unit): TextWatcher =
        object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                onText(s?.toString() ?: "")
            }
        }

    private inner class PaletteAdapter : RecyclerView.Adapter<PaletteAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvIcon: TextView = v.findViewById(R.id.tvIcon)
            val tvLabel: TextView = v.findViewById(R.id.tvLabel)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_palette, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val type = BlockType.values()[position]
            holder.tvIcon.text = type.icon
            holder.tvLabel.text = type.label
            holder.itemView.setOnClickListener { addBlock(type, project.blocks.size) }
            holder.itemView.setOnLongClickListener { view ->
                val clip = ClipData.newPlainText("block-type", type.name)
                view.startDragAndDrop(clip, View.DragShadowBuilder(view), type, 0)
                true
            }
        }

        override fun getItemCount(): Int = BlockType.values().size
    }
}
